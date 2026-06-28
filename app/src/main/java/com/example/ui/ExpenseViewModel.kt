package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

private fun isValidNumber(value: Double): Boolean = !value.isNaN() && !value.isInfinite()

fun formatAmount(amount: Double): String {
    if (!isValidNumber(amount)) return "₹0.00"
    val isNegative = amount < 0.0
    val absAmount = if (isNegative) -amount else amount
    val s = String.format(java.util.Locale.US, "%.2f", absAmount)
    val parts = s.split(".")
    val intPart = parts[0]
    val decPart = parts[1]
    
    val len = intPart.length
    val formatted = if (len <= 3) {
        "${intPart}.${decPart}"
    } else {
        val lastThree = intPart.substring(len - 3)
        val remaining = intPart.substring(0, len - 3)
        val sb = StringBuilder()
        val revRemaining = remaining.reversed()
        revRemaining.forEachIndexed { index, c ->
            if (index > 0 && index % 2 == 0) {
                sb.append(",")
            }
            sb.append(c)
        }
        "${sb.reverse()},$lastThree.$decPart"
    }
    return if (isNegative) "-$formatted" else formatted
}

fun formatAmount(amount: Long): String {
    if (!isValidNumber(amount.toDouble())) return "₹0.00"
    val isNegative = amount < 0L
    val absAmount = if (isNegative) -amount else amount
    val intPart = absAmount.toString()
    val decPart = "00"
    
    val len = intPart.length
    val formatted = if (len <= 3) {
        "${intPart}.${decPart}"
    } else {
        val lastThree = intPart.substring(len - 3)
        val remaining = intPart.substring(0, len - 3)
        val sb = java.lang.StringBuilder()
        val revRemaining = remaining.reversed()
        revRemaining.forEachIndexed { index, c ->
            if (index > 0 && index % 2 == 0) {
                sb.append(",")
            }
            sb.append(c)
        }
        "${sb.reverse()},$lastThree.$decPart"
    }
    return if (isNegative) "-$formatted" else formatted
}

fun formatAmount(amount: Int): String {
    return formatAmount(amount.toLong())
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // --- Active User Configuration ---
    private val _currentUserId = MutableStateFlow<Long>(1L) // Default/initial
    val currentUserId = _currentUserId.asStateFlow()

    val currentUser: StateFlow<User?> = combine(repository.allUsers, _currentUserId) { users, id ->
        users.find { it.id == id } ?: users.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoaded = MutableStateFlow(false)
    val isDatabaseLoaded = _isLoaded.asStateFlow()

    private val _appInitState = MutableStateFlow(AppInitState())
    val appInitState = _appInitState.asStateFlow()

    // --- Global Data Flows ---
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .onEach { users ->
            _isLoaded.value = true
            _appInitState.value = AppInitState(isLoaded = true, usersList = users)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSpaces: StateFlow<List<Space>> = repository.allSpaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSpaceMembers: StateFlow<List<SpaceMember>> = repository.allSpaceMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWallets: StateFlow<List<Wallet>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubscriptions: StateFlow<List<Subscription>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenseSplits: StateFlow<List<ExpenseSplit>> = repository.allExpenseSplits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Pending Expense amount to pass from calculators ---
    private val _pendingExpenseAmount = MutableStateFlow<Double?>(null)
    val pendingExpenseAmount = _pendingExpenseAmount.asStateFlow()

    fun setPendingExpenseAmount(amount: Double) {
        _pendingExpenseAmount.value = amount
    }

    fun clearPendingExpenseAmount() {
        _pendingExpenseAmount.value = null
    }

    // --- Editing Expense State ---
    private val _editingExpense = MutableStateFlow<Expense?>(null)
    val editingExpense = _editingExpense.asStateFlow()

    fun startEditingExpense(expense: Expense) {
        _editingExpense.value = expense
    }

    fun clearEditingExpense() {
        _editingExpense.value = null
    }

    // --- Active Space Details & Calculations ---
    private val _activeSpaceId = MutableStateFlow<Long?>(null)
    val activeSpaceId = _activeSpaceId.asStateFlow()

    val activeSpace: StateFlow<Space?> = _activeSpaceId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getSpaceById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSpaceMembers: StateFlow<List<User>> = _activeSpaceId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMembersOfSpace(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSpaceExpenses: StateFlow<List<Expense>> = _activeSpaceId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getExpensesForSpace(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSpaceSplits: StateFlow<List<ExpenseSplit>> = _activeSpaceId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSplitsForSpace(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Computed Balances & Settlement Logic ---
    
    // Represents a user's net status inside a space
    data class MemberBalance(
        val user: User,
        val totalPaid: Double,
        val totalOwed: Double,
        val netBalance: Double // paid - owed. Positive means they are owed, negative means they owe.
    )

    // Represents a transaction needed to settle up
    data class SettlementTransaction(
        val debtor: User,
        val creditor: User,
        val amount: Double
    )

    // Flow that computes balances for all members in the current active space
    val activeSpaceBalances: StateFlow<List<MemberBalance>> = combine(
        activeSpaceMembers,
        activeSpaceExpenses,
        activeSpaceSplits
    ) { members, expenses, splits ->
        members.map { member ->
            val paidByMember = expenses.filter { it.paidById == member.id && it.category != "Settlement" }.sumOf { it.amount }
            val owedByMember = splits.filter { it.userId == member.id && it.expenseId in expenses.filter { e -> e.category != "Settlement" }.map { e -> e.id } }.sumOf { it.amountOwed }
            
            // Total settlements paid by this member as debtor
            val settlementsPaid = expenses.filter { it.paidById == member.id && it.category == "Settlement" }.sumOf { it.amount }
            // Total settlements received by this member as creditor
            val settlementsReceived = splits.filter { it.userId == member.id && it.expenseId in expenses.filter { e -> e.category == "Settlement" }.map { e -> e.id } }.sumOf { it.amountOwed }

            val netBalance = (paidByMember - owedByMember) + settlementsPaid - settlementsReceived

            MemberBalance(
                user = member,
                totalPaid = paidByMember,
                totalOwed = owedByMember,
                netBalance = netBalance
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow that computes a minimized list of settlement transactions
    val activeSpaceSettlementTransactions: StateFlow<List<SettlementTransaction>> = activeSpaceBalances
        .map { balances ->
            resolveDebts(balances)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall summary of balance for the active user across ALL spaces
    data class OverallBalanceSummary(
        val totalOwedToYou: Double,  // Money you lent that you are owed
        val totalYouOwe: Double,     // Money you borrowed that you owe
        val netBalance: Double
    )

    val overallUserBalance: StateFlow<OverallBalanceSummary> = combine(
        allExpenses,
        allExpenseSplits,
        _currentUserId
    ) { expenses, splits, currentId ->
        // Total I paid across all spaces
        val totalPaidByMe = expenses.filter { it.paidById == currentId }.sumOf { it.amount }
        // Total I owe across all spaces (from splits assigned to me)
        val totalOwedByMe = splits.filter { it.userId == currentId }.sumOf { it.amountOwed }

        val net = totalPaidByMe - totalOwedByMe
        OverallBalanceSummary(
            totalOwedToYou = if (net > 0) net else 0.0,
            totalYouOwe = if (net < 0) -net else 0.0,
            netBalance = net
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallBalanceSummary(0.0, 0.0, 0.0))


    // --- Actions / Mutators ---

    fun selectSpace(spaceId: Long?) {
        _activeSpaceId.value = spaceId
    }

    fun selectCurrentUser(userId: Long) {
        _currentUserId.value = userId
    }

    fun createUser(name: String, email: String) {
        viewModelScope.launch {
            val newId = repository.insertUser(User(name = name, email = email))
            _currentUserId.value = newId
        }
    }

    fun createMultipleUsers(names: List<String>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            var activeId = 0L
            for (name in names) {
                val email = "${name.lowercase().replace(" ", "")}@example.com"
                val id = repository.insertUser(User(name = name, email = email))
                if (activeId == 0L) {
                    activeId = id
                }
            }
            if (activeId != 0L) {
                _currentUserId.value = activeId
            }
            onComplete()
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun updateUserProfile(userId: Long, newName: String, newAvatarUrl: String = "") {
        viewModelScope.launch {
            val existing = repository.allUsers.first().find { it.id == userId } ?: return@launch
            repository.updateUser(existing.copy(name = newName, avatarUrl = newAvatarUrl.ifEmpty { existing.avatarUrl }))
        }
    }

    fun updateUserAvatar(userId: Long, avatarUrl: String) {
        viewModelScope.launch {
            val existing = repository.allUsers.first().find { it.id == userId } ?: return@launch
            repository.updateUser(existing.copy(avatarUrl = avatarUrl))
        }
    }

    fun createSpace(name: String, description: String, memberIds: List<Long>) {
        viewModelScope.launch {
            val allIds = memberIds.toMutableList()
            val currentId = _currentUserId.value
            if (!allIds.contains(currentId)) {
                allIds.add(currentId)
            }
            repository.addSpaceWithMembers(name, description, allIds)
        }
    }

    /**
     * Creates a space with new members specified by name strings.
     * Creates users if needed, then creates the space with all member IDs.
     */
    fun createSpaceWithNewMembers(spaceName: String, spaceDesc: String, memberNames: List<String>, existingMemberIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val allIds = existingMemberIds.toMutableList()
            for (name in memberNames) {
                val email = "${name.lowercase().replace(" ", "")}@example.com"
                val id = repository.insertUser(User(name = name, email = email))
                allIds.add(id)
            }
            // Always include the current active user
            val currentId = _currentUserId.value
            if (!allIds.contains(currentId)) {
                allIds.add(currentId)
            }
            repository.addSpaceWithMembers(spaceName, spaceDesc, allIds)
        }
    }

    fun updateSpaceDetails(spaceId: Long, name: String, description: String) {
        viewModelScope.launch {
            val currentSpace = repository.getSpaceById(spaceId).first()
            if (currentSpace != null) {
                repository.updateSpace(currentSpace.copy(name = name, description = description))
            }
        }
    }


    fun addMemberToSpace(spaceId: Long, userId: Long) {
        viewModelScope.launch {
            repository.addMemberToSpace(spaceId, userId)
        }
    }

    fun createAndAddMemberToSpace(spaceId: Long, name: String, email: String) {
        viewModelScope.launch {
            val userId = repository.insertUser(User(name = name, email = email))
            repository.addMemberToSpace(spaceId, userId)
        }
    }

    fun removeUserFromSpaceAndRecalculate(spaceId: Long, userId: Long) {
        viewModelScope.launch {
            repository.removeUserFromSpaceAndRecalculate(spaceId, userId)
        }
    }

    fun getMembersOfSpace(spaceId: Long): kotlinx.coroutines.flow.Flow<List<User>> = repository.getMembersOfSpace(spaceId)

    fun getSplitsForExpense(expenseId: Long): kotlinx.coroutines.flow.Flow<List<ExpenseSplit>> = repository.getSplitsForExpense(expenseId)

    fun addExpenseEqualSplit(
        spaceId: Long,
        paidById: Long,
        description: String,
        amount: Double,
        category: String,
        participantIds: List<Long>,
        date: Long = System.currentTimeMillis(),
        walletId: Long? = null,
        attachmentUris: List<String> = emptyList(),
        expenseId: Long = 0L
    ) {
        viewModelScope.launch {
            if (participantIds.isEmpty()) return@launch
            
            val size = participantIds.size
            val perPerson = Math.round((amount / size) * 100.0) / 100.0
            val totalAllocated = perPerson * size
            val diff = Math.round((amount - totalAllocated) * 100.0) / 100.0
            
            val expense = Expense(
                id = expenseId,
                spaceId = spaceId,
                paidById = paidById,
                description = description,
                amount = amount,
                category = category,
                date = date,
                walletId = walletId,
                attachmentUris = attachmentUris
            )
            val splits = participantIds.mapIndexed { index, userId ->
                val owe = if (index == size - 1) Math.round((perPerson + diff) * 100.0) / 100.0 else perPerson
                ExpenseSplit(
                    expenseId = expenseId,
                    userId = userId,
                    amountOwed = owe
                )
            }
            if (expenseId > 0L) {
                repository.updateExpenseWithSplits(expense, splits)
            } else {
                repository.addExpenseWithSplits(expense, splits)
            }
        }
    }

    fun addExpenseExactSplit(
        spaceId: Long,
        paidById: Long,
        description: String,
        amount: Double,
        category: String,
        splitsMap: Map<Long, Double>,
        date: Long = System.currentTimeMillis(),
        walletId: Long? = null,
        attachmentUris: List<String> = emptyList(),
        expenseId: Long = 0L
    ) {
        viewModelScope.launch {
            val totalAllocated = splitsMap.values.sum()
            val diff = Math.round((amount - totalAllocated) * 100.0) / 100.0
            
            val expense = Expense(
                id = expenseId,
                spaceId = spaceId,
                paidById = paidById,
                description = description,
                amount = amount,
                category = category,
                date = date,
                walletId = walletId,
                attachmentUris = attachmentUris
            )
            val splits = splitsMap.map { (userId, amountOwed) ->
                ExpenseSplit(
                    expenseId = expenseId,
                    userId = userId,
                    amountOwed = amountOwed
                )
            }.toMutableList()
            
            if (Math.abs(diff) > 0.0) {
                // Adjust the last split
                val lastIdx = splits.size - 1
                if (lastIdx >= 0) {
                    val lastSplit = splits[lastIdx]
                    splits[lastIdx] = lastSplit.copy(amountOwed = Math.round((lastSplit.amountOwed + diff) * 100.0) / 100.0)
                }
            }
            
            if (expenseId > 0L) {
                repository.updateExpenseWithSplits(expense, splits)
            } else {
                repository.addExpenseWithSplits(expense, splits)
            }
        }
    }

    fun addExpenseWithCustomSplits(
        spaceId: Long,
        paidById: Long,
        description: String,
        amount: Double,
        category: String,
        date: Long,
        splits: List<ExpenseSplit>,
        walletId: Long? = null,
        attachmentUris: List<String> = emptyList(),
        expenseId: Long = 0L
    ) {
        viewModelScope.launch {
            val totalAllocated = splits.sumOf { it.amountOwed }
            val diff = Math.round((amount - totalAllocated) * 100.0) / 100.0
            
            val expense = Expense(
                id = expenseId,
                spaceId = spaceId,
                paidById = paidById,
                description = description,
                amount = amount,
                category = category,
                date = date,
                walletId = walletId,
                attachmentUris = attachmentUris
            )
            val targetSplits = splits.mapIndexed { index, split ->
                val owe = if (index == splits.size - 1) Math.round((split.amountOwed + diff) * 100.0) / 100.0 else split.amountOwed
                split.copy(expenseId = expenseId, amountOwed = owe)
            }
            if (expenseId > 0L) {
                repository.updateExpenseWithSplits(expense, targetSplits)
            } else {
                repository.addExpenseWithSplits(expense, targetSplits)
            }
        }
    }

    // --- Wallets Functions ---
    fun insertWallet(name: String, type: String, balance: Double) {
        viewModelScope.launch {
            repository.insertWallet(Wallet(name = name, type = type, balance = balance))
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }

    fun updateWallet(id: Long, name: String, type: String, balance: Double) {
        viewModelScope.launch {
            repository.updateWallet(id, name, type, balance)
        }
    }

    // --- Categories Functions ---
    fun insertCategory(name: String, iconName: String, parentId: Long?, context: android.content.Context) {
        viewModelScope.launch {
            val categories = repository.allCategories.first()
            val exists = categories.any { it.name.equals(name, ignoreCase = true) }
            if (exists) {
                android.widget.Toast.makeText(context, "Category '$name' already exists", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                repository.insertCategory(Category(name = name, iconName = iconName.lowercase(), parentId = parentId))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun updateCategory(oldName: String, category: Category) {
        viewModelScope.launch {
            repository.updateCategory(oldName, category)
        }
    }

    // --- Budget Functions ---
    fun insertBudget(isGlobal: Boolean, categoryName: String?, limitAmount: Double, monthYear: String) {
        viewModelScope.launch {
            repository.insertBudget(
                Budget(
                    isGlobal = isGlobal,
                    categoryName = categoryName,
                    limitAmount = limitAmount,
                    monthYear = monthYear
                )
            )
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // --- Subscription Functions ---
    fun insertSubscription(
        spaceId: Long,
        paidById: Long,
        name: String,
        amount: Double,
        category: String,
        walletId: Long?,
        intervalType: String,
        nextDueDate: Long
    ) {
        viewModelScope.launch {
            repository.insertSubscription(
                Subscription(
                    spaceId = spaceId,
                    paidById = paidById,
                    name = name,
                    amount = amount,
                    category = category,
                    walletId = walletId,
                    intervalType = intervalType,
                    nextDueDate = nextDueDate
                )
            )
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }

    fun settleDebt(spaceId: Long, debtorId: Long, creditorId: Long, amount: Double, walletId: Long, context: Context) {
        viewModelScope.launch {
            try {
                repository.settleDebtTransactionally(spaceId, debtorId, creditorId, amount, walletId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Debt settled successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IllegalStateException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message ?: "Failed to settle debt", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun deleteSpace(space: Space) {
        viewModelScope.launch {
            repository.deleteSpace(space)
        }
    }

    private fun resolveDebts(balances: List<MemberBalance>): List<SettlementTransaction> {
        val transactions = mutableListOf<SettlementTransaction>()

        val creditors = balances.filter { it.netBalance > 0.01 }
            .map { it.user to it.netBalance }
            .toMutableList()
            
        val debtors = balances.filter { it.netBalance < -0.01 }
            .map { it.user to -it.netBalance }
            .toMutableList()

        var cIndex = 0
        var dIndex = 0

        while (cIndex < creditors.size && dIndex < debtors.size) {
            val (creditor, creditAmount) = creditors[cIndex]
            val (debtor, debtAmount) = debtors[dIndex]

            val settledAmount = minOf(creditAmount, debtAmount)
            if (settledAmount > 0.01) {
                transactions.add(SettlementTransaction(debtor, creditor, settledAmount))
            }

            // Update remaining amounts
            creditors[cIndex] = creditor to (creditAmount - settledAmount)
            debtors[dIndex] = debtor to (debtAmount - settledAmount)

            if (creditors[cIndex].second <= 0.01) {
                cIndex++
            }
            if (debtors[dIndex].second <= 0.01) {
                dIndex++
            }
        }

        return transactions
    }

    suspend fun getSharePayloadForSpace(spaceId: Long): SharedSpacePayload? {
        val spaces = repository.allSpaces.first()
        val space = spaces.find { it.id == spaceId } ?: return null
        val members = repository.getMembersOfSpace(spaceId).first()
        val expenses = repository.getExpensesForSpace(spaceId).first()
        val splits = repository.getSplitsForSpace(spaceId).first()

        val sharedUsers = members.map { SharedUser(it.name, it.email, it.avatarUrl) }
        
        // Limit to 50 expenses to keep QR size under 3KB
        val sharedExpenses = expenses.take(50).map { expense ->
            val paidByMember = members.find { it.id == expense.paidById }
            val paidEmail = paidByMember?.email ?: "unknown@example.com"
            
            val expenseSplits = splits.filter { it.expenseId == expense.id }
            val sharedSplits = expenseSplits.mapNotNull { split ->
                val oweMember = members.find { it.id == split.userId }
                if (oweMember != null) {
                    SharedSplit(oweMember.email, split.amountOwed)
                } else {
                    null
                }
            }

            SharedExpense(
                description = expense.description,
                amount = expense.amount,
                date = expense.date,
                category = expense.category,
                paidByUserEmail = paidEmail,
                splits = sharedSplits
            )
        }

        return SharedSpacePayload(
            spaceName = space.name,
            spaceDescription = space.description,
            spaceCreatedAt = space.createdAt,
            members = sharedUsers,
            expenses = sharedExpenses,
            isTruncated = expenses.size > 50
        )
    }

    fun importSharedSpace(payload: SharedSpacePayload, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val sName = payload.spaceName
                val sDesc = payload.spaceDescription
                val sCreated = payload.spaceCreatedAt
                
                val currentSpaces = repository.allSpaces.first()
                var spaceId = currentSpaces.find { it.name == sName && it.createdAt == sCreated }?.id
                if (spaceId == null) {
                    spaceId = repository.insertSpace(Space(name = sName, description = sDesc, createdAt = sCreated))
                }
                val finalSpaceId = spaceId!!

                val emailToLocalUserId = mutableMapOf<String, Long>()
                val existingUsers = repository.allUsers.first()
                
                payload.members.forEach { sharedUser ->
                    val existingUser = existingUsers.find { it.email.lowercase() == sharedUser.email.lowercase() }
                    val localUserId = if (existingUser != null) {
                        existingUser.id
                    } else {
                        repository.insertUser(User(name = sharedUser.name, email = sharedUser.email, avatarUrl = sharedUser.avatarUrl))
                    }
                    emailToLocalUserId[sharedUser.email.lowercase()] = localUserId
                    repository.addMemberToSpace(finalSpaceId, localUserId)
                }

                val existingExpenses = repository.getExpensesForSpace(finalSpaceId).first()

                payload.expenses.forEach { sharedExpense ->
                    val alreadyImported = existingExpenses.any { 
                        it.description == sharedExpense.description && 
                        Math.abs(it.amount - sharedExpense.amount) < 0.01 && 
                        it.date == sharedExpense.date 
                    }
                    
                    if (alreadyImported) return@forEach

                    val paidByLocalUserId = emailToLocalUserId[sharedExpense.paidByUserEmail.lowercase()] 
                        ?: _currentUserId.value

                    val newExpense = Expense(
                        spaceId = finalSpaceId,
                        paidById = paidByLocalUserId,
                        description = sharedExpense.description,
                        amount = sharedExpense.amount,
                        date = sharedExpense.date,
                        category = sharedExpense.category
                    )

                    val splits = sharedExpense.splits.mapNotNull { sharedSplit ->
                        val oweLocalUserId = emailToLocalUserId[sharedSplit.userEmail.lowercase()]
                        if (oweLocalUserId != null) {
                            ExpenseSplit(
                                expenseId = 0,
                                userId = oweLocalUserId,
                                amountOwed = sharedSplit.amountOwed
                            )
                        } else {
                            null
                        }
                    }

                    repository.addExpenseWithSplits(newExpense, splits)
                }

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun restoreDefaultWallets() {
        viewModelScope.launch {
            repository.restoreDefaultWallets()
        }
    }
}

@Serializable
data class SharedSpacePayload(
    @SerialName("sn") val spaceName: String,
    @SerialName("sd") val spaceDescription: String,
    @SerialName("sc") val spaceCreatedAt: Long,
    @SerialName("m") val members: List<SharedUser>,
    @SerialName("e") val expenses: List<SharedExpense>,
    @SerialName("it") val isTruncated: Boolean = false
)

@Serializable
data class SharedUser(
    @SerialName("n") val name: String,
    @SerialName("e") val email: String,
    @SerialName("a") val avatarUrl: String = ""
)

@Serializable
data class SharedExpense(
    @SerialName("d") val description: String,
    @SerialName("a") val amount: Double,
    @SerialName("t") val date: Long,
    @SerialName("c") val category: String,
    @SerialName("p") val paidByUserEmail: String,
    @SerialName("s") val splits: List<SharedSplit>
)

@Serializable
data class SharedSplit(
    @SerialName("u") val userEmail: String,
    @SerialName("o") val amountOwed: Double
)

data class AppInitState(
    val isLoaded: Boolean = false,
    val usersList: List<User> = emptyList()
)

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
