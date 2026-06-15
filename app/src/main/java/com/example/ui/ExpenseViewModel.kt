package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // --- Active User Configuration ---
    private val _currentUserId = MutableStateFlow<Long>(1L) // Default/initial
    val currentUserId = _currentUserId.asStateFlow()

    val currentUser: StateFlow<User?> = combine(repository.allUsers, _currentUserId) { users, id ->
        val matched = users.find { it.id == id } ?: users.firstOrNull()
        if (matched != null && _currentUserId.value != matched.id) {
            _currentUserId.value = matched.id
        }
        matched
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
            val paidByMember = expenses.filter { it.paidById == member.id }.sumOf { it.amount }
            val owedByMember = splits.filter { it.userId == member.id }.sumOf { it.amountOwed }
            MemberBalance(
                user = member,
                totalPaid = paidByMember,
                totalOwed = owedByMember,
                netBalance = paidByMember - owedByMember
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
            repository.addSpaceWithMembers(name, description, memberIds)
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
        walletId: Long = 1L,
        attachmentUris: List<String> = emptyList(),
        expenseId: Long = 0L
    ) {
        viewModelScope.launch {
            if (participantIds.isEmpty()) return@launch
            val perPerson = amount / participantIds.size
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
            val splits = participantIds.map { userId ->
                ExpenseSplit(
                    expenseId = expenseId,
                    userId = userId,
                    amountOwed = perPerson
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
        walletId: Long = 1L,
        attachmentUris: List<String> = emptyList(),
        expenseId: Long = 0L
    ) {
        viewModelScope.launch {
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
        walletId: Long = 1L,
        attachmentUris: List<String> = emptyList(),
        expenseId: Long = 0L
    ) {
        viewModelScope.launch {
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
            val targetSplits = splits.map { it.copy(expenseId = expenseId) }
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

    fun updateWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.insertWallet(wallet)
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }

    // --- Categories Functions ---
    fun insertCategory(name: String, iconName: String, parentId: Long?) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, iconName = iconName, parentId = parentId))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
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
        walletId: Long,
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

    fun settleDebt(spaceId: Long, debtorId: Long, creditorId: Long, amount: Double) {
        viewModelScope.launch {
            // Register a settlement expense: Debtor pays Creditor the amount
            // Payer: debtorId
            // Beneficiary (who owes debtor): creditorId
            val expense = Expense(
                spaceId = spaceId,
                paidById = debtorId,
                description = "Settle Up (Payment to Creditor)",
                amount = amount,
                category = "Settlement"
            )
            val splits = listOf(
                ExpenseSplit(expenseId = 0, userId = creditorId, amountOwed = amount)
            )
            repository.addExpenseWithSplits(expense, splits)
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
            if (_activeSpaceId.value == space.id) {
                _activeSpaceId.value = null
            }
        }
    }

    // --- Settling up debts algorithm ---
    private fun resolveDebts(balances: List<MemberBalance>): List<SettlementTransaction> {
        val transactions = mutableListOf<SettlementTransaction>()
        
        // Separate members into positive balances (creditors) and negative balances (debtors)
        // We use a threshold of 0.01 to avoid precision issues with doubles
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

            if (creditors[cIndex].second < 0.01) {
                cIndex++
            }
            if (debtors[dIndex].second < 0.01) {
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
        
        val sharedExpenses = expenses.map { expense ->
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
            expenses = sharedExpenses
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
                    val existingExp = existingExpenses.find { 
                        it.description == sharedExpense.description && 
                        Math.abs(it.amount - sharedExpense.amount) < 0.01 && 
                        it.date == sharedExpense.date 
                    }
                    
                    if (existingExp != null) {
                        repository.deleteExpense(existingExp.id)
                    }

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
}

@Serializable
data class SharedSpacePayload(
    val spaceName: String,
    val spaceDescription: String,
    val spaceCreatedAt: Long,
    val members: List<SharedUser>,
    val expenses: List<SharedExpense>
)

@Serializable
data class SharedUser(
    val name: String,
    val email: String,
    val avatarUrl: String = ""
)

@Serializable
data class SharedExpense(
    val description: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val paidByUserEmail: String,
    val splits: List<SharedSplit>
)

@Serializable
data class SharedSplit(
    val userEmail: String,
    val amountOwed: Double
)

// Helper extenson on Room Dao to query splits of a given user (added inside repo for convenience)
fun ExpenseRepository.getSplitsForUser(userId: Long): Flow<List<ExpenseSplit>> {
    // We can simulate or return a flow from allExpenses + checking splits in DB...
    // Let's keep it extremely clean. Since splits are in rooms and we have DAO we can write a function if needed.
    // For general simplicity, we can also query all splits from database if we want or define inside ExpenseDao.
    // Let's implement active user's overview parameters nicely!
    return kotlinx.coroutines.flow.flowOf(emptyList())
}


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
