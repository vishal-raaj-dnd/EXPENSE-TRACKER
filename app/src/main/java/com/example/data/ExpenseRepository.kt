package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // --- Flows ---
    val allUsers: Flow<List<User>> = expenseDao.getAllUsers()
    val allSpaces: Flow<List<Space>> = expenseDao.getAllSpaces()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allSpaceMembers: Flow<List<SpaceMember>> = expenseDao.getAllSpaceMembers()
    val allWallets: Flow<List<Wallet>> = expenseDao.getAllWallets()
    val allCategories: Flow<List<Category>> = expenseDao.getAllCategories()
    val allBudgets: Flow<List<Budget>> = expenseDao.getAllBudgets()
    val allSubscriptions: Flow<List<Subscription>> = expenseDao.getAllSubscriptions()

    // --- Space Specific Query Methods ---
    fun getSpaceById(spaceId: Long): Flow<Space?> = expenseDao.getSpaceById(spaceId)
    
    fun getMembersOfSpace(spaceId: Long): Flow<List<User>> = expenseDao.getMembersOfSpace(spaceId)
    
    fun getExpensesForSpace(spaceId: Long): Flow<List<Expense>> = expenseDao.getExpensesForSpace(spaceId)
    
    fun getSplitsForSpace(spaceId: Long): Flow<List<ExpenseSplit>> = expenseDao.getSplitsForSpace(spaceId)
    
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> = expenseDao.getSplitsForExpense(expenseId)

    fun getSplitsForUser(userId: Long): Flow<List<ExpenseSplit>> = expenseDao.getSplitsForUser(userId)

    // --- Insertions / Write Operations ---
    suspend fun insertUser(user: User): Long {
        return expenseDao.insertUser(user)
    }

    suspend fun deleteUser(user: User) {
        expenseDao.deleteUser(user)
    }

    suspend fun insertSpace(space: Space): Long {
        return expenseDao.insertSpace(space)
    }

    suspend fun addSpaceWithMembers(name: String, description: String, userIds: List<Long>): Long {
        return expenseDao.addSpaceWithMembers(name, description, userIds)
    }

    suspend fun addMemberToSpace(spaceId: Long, userId: Long) {
        expenseDao.insertSpaceMember(SpaceMember(spaceId = spaceId, userId = userId))
    }

    suspend fun removeMemberFromSpace(spaceId: Long, userId: Long) {
        expenseDao.deleteSpaceMember(spaceId, userId)
    }

    suspend fun deleteSpace(space: Space) {
        expenseDao.deleteSpaceCascade(space)
    }

    suspend fun addExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>): Long {
        return expenseDao.insertExpenseWithSplits(expense, splits)
    }

    suspend fun updateExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>) {
        expenseDao.updateExpenseWithSplits(expense, splits)
    }

    suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpenseCascade(expenseId)
    }

    // --- Wallets Write Ops ---
    suspend fun insertWallet(wallet: Wallet): Long {
        return expenseDao.insertWallet(wallet)
    }

    suspend fun deleteWallet(wallet: Wallet) {
        expenseDao.deleteWalletCascade(wallet)
    }

    suspend fun updateWallet(id: Long, name: String, type: String, balance: Double) {
        expenseDao.updateWallet(id, name, type, balance)
    }

    // --- Categories Write Ops ---
    suspend fun insertCategory(category: Category): Long {
        return expenseDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        // Delete budgets of child categories first to prevent orphaned budgets
        val categories = expenseDao.getAllCategories().first()
        val children = categories.filter { it.parentId == category.id }
        for (child in children) {
            expenseDao.deleteBudgetsByCategoryName(child.name)
        }
        expenseDao.deleteBudgetsByCategoryName(category.name)
        expenseDao.deleteCategory(category)
    }

    // --- Budgets Write Ops ---
    suspend fun insertBudget(budget: Budget): Long {
        return expenseDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        expenseDao.deleteBudget(budget)
    }

    // --- Subscriptions Write Ops ---
    suspend fun insertSubscription(subscription: Subscription): Long {
        return expenseDao.insertSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        expenseDao.deleteSubscription(subscription)
    }

    suspend fun processOverdueSubscriptions(): Int {
        val subscriptions = expenseDao.getActiveSubscriptionsSync()
        val now = System.currentTimeMillis()
        var processedCount = 0

        for (sub in subscriptions) {
            if (now >= sub.nextDueDate && sub.isActive) {
                // 1. Get members of this space to calculate split
                val memberIds = expenseDao.getMemberIdsOfSpaceSync(sub.spaceId)
                if (memberIds.isEmpty()) continue

                // Round split amounts correctly (H7 / H8)
                val size = memberIds.size
                val perPerson = Math.round((sub.amount / size) * 100.0) / 100.0
                val totalAllocated = perPerson * size
                val diff = Math.round((sub.amount - totalAllocated) * 100.0) / 100.0

                val splits = memberIds.mapIndexed { index, memberId ->
                    val owe = if (index == size - 1) Math.round((perPerson + diff) * 100.0) / 100.0 else perPerson
                    ExpenseSplit(expenseId = 0, userId = memberId, amountOwed = owe)
                }

                // 2. Prepare expense object
                val expense = Expense(
                    spaceId = sub.spaceId,
                    paidById = sub.paidById,
                    description = "Recurring: ${sub.name}",
                    amount = sub.amount,
                    category = sub.category,
                    walletId = sub.walletId,
                    date = sub.nextDueDate // Use the scheduled due date
                )

                // 3. Calculate next due date
                val calendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = sub.nextDueDate
                }
                when (sub.intervalType.lowercase()) {
                    "daily" -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    "weekly" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                    "monthly" -> calendar.add(java.util.Calendar.MONTH, 1)
                    else -> calendar.add(java.util.Calendar.MONTH, 1)
                }

                val updatedSub = sub.copy(nextDueDate = calendar.timeInMillis)

                // 4. Run transactional process (H2, M16)
                try {
                    expenseDao.processSubscriptionPayment(expense, splits, updatedSub)
                    processedCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return processedCount
    }

    // --- All Splits Flow (for dashboard sync) ---
    val allExpenseSplits: Flow<List<ExpenseSplit>> = expenseDao.getAllExpenseSplits()

    suspend fun settleDebtTransactionally(
        spaceId: Long,
        debtorId: Long,
        creditorId: Long,
        amount: Double,
        walletId: Long
    ) {
        expenseDao.settleDebtTransactionally(spaceId, debtorId, creditorId, amount, walletId)
    }

    suspend fun deductFromWallet(walletId: Long, amount: Double) {
        val wallet = expenseDao.getWalletById(walletId) ?: throw IllegalStateException("Wallet not found")
        val affected = if (wallet.type == "Credit Card") {
            expenseDao.deductFromWalletNoCheck(walletId, amount)
        } else {
            expenseDao.deductFromWalletWithCheck(walletId, amount)
        }
        if (affected == 0) {
            throw IllegalStateException("Insufficient balance in wallet")
        }
    }

    // --- Update User (for profile editing) ---
    suspend fun updateUser(user: User) {
        expenseDao.insertUser(user)
    }

    /**
     * Removes a user from a space and recalculates all affected expense splits.
     * 1. Expenses paid by deleted user -> reassign payer or delete if empty
     * 2. Expenses where deleted user was a split participant -> remove their split
     *    and redistribute evenly among remaining participants
     * 3. Remove the SpaceMember entry
     */
    suspend fun removeUserFromSpaceAndRecalculate(spaceId: Long, userId: Long) {
        expenseDao.removeUserFromSpaceAndRecalculate(spaceId, userId)
    }

    // --- Onboarding Reset ---
    suspend fun clearAllData() {
        expenseDao.clearAllData()
    }
}
