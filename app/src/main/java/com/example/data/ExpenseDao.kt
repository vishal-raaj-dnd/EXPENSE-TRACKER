package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Delete
    suspend fun deleteUser(user: User)

    // --- Spaces ---
    @Query("SELECT * FROM spaces ORDER BY createdAt DESC")
    fun getAllSpaces(): Flow<List<Space>>

    @Query("SELECT * FROM spaces WHERE id = :id")
    fun getSpaceById(id: Long): Flow<Space?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: Space): Long

    @Delete
    suspend fun deleteSpace(space: Space)

    // --- Space Members ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpaceMember(member: SpaceMember): Long

    @Query("SELECT * FROM space_members")
    fun getAllSpaceMembers(): Flow<List<SpaceMember>>

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN space_members sm ON u.id = sm.userId 
        WHERE sm.spaceId = :spaceId
    """)
    fun getMembersOfSpace(spaceId: Long): Flow<List<User>>

    @Query("DELETE FROM space_members WHERE spaceId = :spaceId AND userId = :userId")
    suspend fun deleteSpaceMember(spaceId: Long, userId: Long)

    @Query("DELETE FROM space_members WHERE spaceId = :spaceId")
    suspend fun deleteSpaceMembers(spaceId: Long)

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE spaceId = :spaceId ORDER BY date DESC")
    fun getExpensesForSpace(spaceId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsByExpenseId(expenseId: Long)

    @Query("DELETE FROM expenses WHERE spaceId = :spaceId")
    suspend fun deleteExpensesBySpaceId(spaceId: Long)

    @Query("DELETE FROM expense_splits WHERE expenseId IN (SELECT id FROM expenses WHERE spaceId = :spaceId)")
    suspend fun deleteSplitsBySpaceId(spaceId: Long)

    @Query("UPDATE expenses SET paidById = :newUserId WHERE id = :expenseId")
    suspend fun reassignExpensePayer(expenseId: Long, newUserId: Long)

    // --- Expense Splits ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplit(split: ExpenseSplit): Long

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>>

    @Query("""
        SELECT es.* FROM expense_splits es 
        INNER JOIN expenses e ON es.expenseId = e.id 
        WHERE e.spaceId = :spaceId
    """)
    fun getSplitsForSpace(spaceId: Long): Flow<List<ExpenseSplit>>

    // --- Wallets ---
    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<Wallet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet): Long

    @Delete
    suspend fun deleteWallet(wallet: Wallet)

    @Query("DELETE FROM subscriptions WHERE walletId = :walletId")
    suspend fun deleteSubscriptionsByWalletId(walletId: Long)

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun getWalletCount(): Int

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): Wallet?

    @Query("UPDATE wallets SET balance = balance - :amount WHERE id = :walletId AND balance >= :amount")
    suspend fun deductFromWalletWithCheck(walletId: Long, amount: Double): Int

    @Query("UPDATE wallets SET balance = balance - :amount WHERE id = :walletId")
    suspend fun deductFromWalletNoCheck(walletId: Long, amount: Double): Int

    @Query("UPDATE wallets SET name = :name, type = :type, balance = :balance WHERE id = :id")
    suspend fun updateWallet(id: Long, name: String, type: String, balance: Double)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM budgets WHERE categoryName = :categoryName")
    suspend fun deleteBudgetsByCategoryName(categoryName: String)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    // --- Budgets ---
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): Budget?

    // --- Subscriptions ---
    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1")
    suspend fun getActiveSubscriptionsSync(): List<Subscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Long): Subscription?

    @Query("DELETE FROM subscriptions WHERE spaceId = :spaceId AND paidById = :userId")
    suspend fun deleteSubscriptionsForUserInSpace(spaceId: Long, userId: Long)

    // --- Space members sync for worker ---
    @Query("SELECT userId FROM space_members WHERE spaceId = :spaceId")
    suspend fun getMemberIdsOfSpaceSync(spaceId: Long): List<Long>

    // --- All Splits (for dashboard sync) ---
    @Query("SELECT * FROM expense_splits")
    fun getAllExpenseSplits(): Flow<List<ExpenseSplit>>

    // --- Cascade user deletion helpers ---
    @Query("DELETE FROM expense_splits WHERE userId = :userId AND expenseId IN (SELECT id FROM expenses WHERE spaceId = :spaceId)")
    suspend fun deleteSplitsByUserInSpace(spaceId: Long, userId: Long)

    @Query("SELECT id FROM expenses WHERE spaceId = :spaceId AND paidById = :userId")
    suspend fun getExpenseIdsPaidByUserInSpace(spaceId: Long, userId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitCountForExpense(expenseId: Long): Int

    @Query("SELECT * FROM expenses WHERE spaceId = :spaceId")
    suspend fun getExpensesInSpaceSync(spaceId: Long): List<Expense>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseSync(expenseId: Long): List<ExpenseSplit>

    @Query("UPDATE expense_splits SET amountOwed = :newAmount WHERE id = :splitId")
    suspend fun updateSplitAmount(splitId: Long, newAmount: Double)

    @Query("SELECT * FROM expense_splits WHERE userId = :userId")
    fun getSplitsForUser(userId: Long): Flow<List<ExpenseSplit>>

    // --- Transactional multi-step operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplitSync(split: ExpenseSplit): Long

    @Transaction
    suspend fun insertExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>): Long {
        val expenseId = insertExpense(expense)
        for (split in splits) {
            insertExpenseSplitSync(split.copy(expenseId = expenseId))
        }
        return expenseId
    }

    @Transaction
    suspend fun updateExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>) {
        require(expense.id > 0L) { "Expense ID must be greater than 0 for update" }
        insertExpense(expense)
        deleteSplitsByExpenseId(expense.id)
        for (split in splits) {
            insertExpenseSplitSync(split.copy(expenseId = expense.id))
        }
    }

    @Transaction
    suspend fun deleteExpenseCascade(expenseId: Long) {
        deleteSplitsByExpenseId(expenseId)
        deleteExpenseById(expenseId)
    }

    @Transaction
    suspend fun deleteSpaceCascade(space: Space) {
        deleteSplitsBySpaceId(space.id)
        deleteExpensesBySpaceId(space.id)
        deleteSpaceMembers(space.id)
        deleteSpace(space)
    }

    @Transaction
    suspend fun deleteWalletCascade(wallet: Wallet) {
        deleteSubscriptionsByWalletId(wallet.id)
        deleteWallet(wallet)
    }

    @Transaction
    suspend fun addSpaceWithMembers(name: String, description: String, userIds: List<Long>): Long {
        val spaceId = insertSpace(Space(name = name, description = description))
        for (userId in userIds) {
            insertSpaceMember(SpaceMember(spaceId = spaceId, userId = userId))
        }
        return spaceId
    }

    @Transaction
    suspend fun removeUserFromSpaceAndRecalculate(spaceId: Long, userId: Long) {
        deleteSubscriptionsForUserInSpace(spaceId, userId)

        val paidExpenseIds = getExpenseIdsPaidByUserInSpace(spaceId, userId)
        for (expenseId in paidExpenseIds) {
            val remainingSplits = getSplitsForExpenseSync(expenseId).filter { it.userId != userId }
            if (remainingSplits.isEmpty()) {
                deleteExpenseCascade(expenseId)
            } else {
                val newPayerId = remainingSplits.first().userId
                reassignExpensePayer(expenseId, newPayerId)
            }
        }

        deleteSplitsByUserInSpace(spaceId, userId)

        val remainingExpenses = getExpensesInSpaceSync(spaceId)
        for (expense in remainingExpenses) {
            val remainingSplits = getSplitsForExpenseSync(expense.id)
            if (remainingSplits.isEmpty()) {
                deleteExpenseCascade(expense.id)
            } else {
                val currentTotalOwed = remainingSplits.sumOf { it.amountOwed }
                val shortfall = expense.amount - currentTotalOwed
                if (shortfall > 0.01) {
                    val extraPerPerson = shortfall / remainingSplits.size
                    for (split in remainingSplits) {
                        updateSplitAmount(split.id, split.amountOwed + extraPerPerson)
                    }
                }
            }
        }

        deleteSpaceMember(spaceId, userId)
    }

    // --- Onboarding Reset Helper Queries ---
    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM expense_splits")
    suspend fun deleteAllExpenseSplits()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM space_members")
    suspend fun deleteAllSpaceMembers()

    @Query("DELETE FROM spaces")
    suspend fun deleteAllSpaces()

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAllSubscriptions()

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()

    @Transaction
    suspend fun clearAllData() {
        deleteAllBudgets()
        deleteAllCategories()
        deleteAllExpenseSplits()
        deleteAllExpenses()
        deleteAllSpaceMembers()
        deleteAllSpaces()
        deleteAllSubscriptions()
        deleteAllUsers()
        deleteAllWallets()

        // Re-populate default entities
        val w1 = insertWallet(Wallet(name = "Cash Wallet", type = "Cash", balance = 5000.0))
        val w2 = insertWallet(Wallet(name = "Bank Account", type = "Bank", balance = 25000.0))
        val w3 = insertWallet(Wallet(name = "Credit Card", type = "Credit Card", balance = -2000.0))

        val catEntertainment = insertCategory(Category(name = "Entertainment", iconName = "movie"))
        insertCategory(Category(name = "Movies", iconName = "movie", parentId = catEntertainment))
        
        insertCategory(Category(name = "Extra", iconName = "category"))
        insertCategory(Category(name = "Fees", iconName = "payments"))
        
        val catFood = insertCategory(Category(name = "Food", iconName = "restaurant"))
        insertCategory(Category(name = "Groceries", iconName = "shopping_cart", parentId = catFood))
        insertCategory(Category(name = "Restaurants", iconName = "restaurant", parentId = catFood))
        
        insertCategory(Category(name = "Gifts", iconName = "spa"))
        insertCategory(Category(name = "Hospital", iconName = "spa"))
        insertCategory(Category(name = "Lodge", iconName = "home"))
        
        val catSchool = insertCategory(Category(name = "School & College", iconName = "school"))
        insertCategory(Category(name = "Tuition Fees", iconName = "payments", parentId = catSchool))
        
        insertCategory(Category(name = "Service", iconName = "category"))
        insertCategory(Category(name = "Shopping", iconName = "shopping_cart"))
        insertCategory(Category(name = "Snacks", iconName = "restaurant"))
        insertCategory(Category(name = "Temple", iconName = "home"))
        
        val catTransport = insertCategory(Category(name = "Transport", iconName = "directions_car"))
        insertCategory(Category(name = "Fuel", iconName = "directions_car", parentId = catTransport))

        // Prepopulate some Budgets dynamically for current month
        val currentMonthYear = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.US).format(java.util.Date(System.currentTimeMillis()))
        insertBudget(Budget(isGlobal = true, categoryName = null, limitAmount = 25000.0, monthYear = currentMonthYear))
        insertBudget(Budget(isGlobal = false, categoryName = "Groceries", limitAmount = 8000.0, monthYear = currentMonthYear))
        insertBudget(Budget(isGlobal = false, categoryName = "Fuel", limitAmount = 5000.0, monthYear = currentMonthYear))
    }

    @Transaction
    suspend fun processSubscriptionPayment(
        expense: Expense,
        splits: List<ExpenseSplit>,
        subscription: Subscription
    ) {
        if (expense.walletId != null) {
            val wallet = getWalletById(expense.walletId)
                ?: throw IllegalStateException("Wallet not found")
            if (wallet.type != "Credit Card" && wallet.balance < expense.amount) {
                throw IllegalStateException("Insufficient balance in wallet")
            }
        }
        val expenseId = insertExpenseWithSplits(expense, splits)
        if (expense.walletId != null) {
            val wallet = getWalletById(expense.walletId)
                ?: throw IllegalStateException("Wallet not found")
            val affected = if (wallet.type == "Credit Card") {
                deductFromWalletNoCheck(expense.walletId, expense.amount)
            } else {
                deductFromWalletWithCheck(expense.walletId, expense.amount)
            }
            if (affected == 0) {
                throw IllegalStateException("Insufficient balance in wallet")
            }
        }
        insertSubscription(subscription)
    }

    @Transaction
    suspend fun settleDebtTransactionally(
        spaceId: Long,
        debtorId: Long,
        creditorId: Long,
        amount: Double,
        walletId: Long
    ) {
        val wallet = getWalletById(walletId) ?: throw IllegalStateException("Wallet not found")
        if (wallet.type != "Credit Card" && wallet.balance < amount) {
            throw IllegalStateException("Insufficient balance in wallet")
        }
        val expense = Expense(
            spaceId = spaceId,
            paidById = debtorId,
            description = "Settle Up (Payment to Creditor)",
            amount = amount,
            category = "Settlement",
            walletId = walletId
        )
        val splits = listOf(
            ExpenseSplit(expenseId = 0, userId = creditorId, amountOwed = amount)
        )
        insertExpenseWithSplits(expense, splits)
        val affected = if (wallet.type == "Credit Card") {
            deductFromWalletNoCheck(walletId, amount)
        } else {
            deductFromWalletWithCheck(walletId, amount)
        }
        if (affected == 0) {
            throw IllegalStateException("Insufficient balance in wallet")
        }
    }
}
