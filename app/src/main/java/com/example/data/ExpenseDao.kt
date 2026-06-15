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

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): Wallet?

    // --- Categories ---
    @Query("SELECT * FROM categories")
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
    suspend fun getExpenseIdsPayedByUserInSpace(spaceId: Long, userId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitCountForExpense(expenseId: Long): Int

    @Query("SELECT * FROM expenses WHERE spaceId = :spaceId")
    suspend fun getExpensesInSpaceSync(spaceId: Long): List<Expense>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseSync(expenseId: Long): List<ExpenseSplit>

    @Query("UPDATE expense_splits SET amountOwed = :newAmount WHERE id = :splitId")
    suspend fun updateSplitAmount(splitId: Long, newAmount: Double)
}
