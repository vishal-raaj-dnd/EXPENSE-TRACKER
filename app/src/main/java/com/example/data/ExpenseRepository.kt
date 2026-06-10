package com.example.data

import kotlinx.coroutines.flow.Flow

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
        val spaceId = expenseDao.insertSpace(Space(name = name, description = description))
        for (userId in userIds) {
            expenseDao.insertSpaceMember(SpaceMember(spaceId = spaceId, userId = userId))
        }
        return spaceId
    }

    suspend fun addMemberToSpace(spaceId: Long, userId: Long) {
        expenseDao.insertSpaceMember(SpaceMember(spaceId = spaceId, userId = userId))
    }

    suspend fun removeMemberFromSpace(spaceId: Long, userId: Long) {
        expenseDao.deleteSpaceMember(spaceId, userId)
    }

    suspend fun deleteSpace(space: Space) {
        expenseDao.deleteSpace(space)
    }

    suspend fun addExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>): Long {
        val expenseId = expenseDao.insertExpense(expense)
        for (split in splits) {
            // Save each split referencing the correct expense id
            expenseDao.insertExpenseSplit(split.copy(expenseId = expenseId))
        }
        return expenseId
    }

    suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpenseById(expenseId)
        expenseDao.deleteSplitsByExpenseId(expenseId)
    }

    // --- Wallets Write Ops ---
    suspend fun insertWallet(wallet: Wallet): Long {
        return expenseDao.insertWallet(wallet)
    }

    suspend fun deleteWallet(wallet: Wallet) {
        expenseDao.deleteWallet(wallet)
    }

    // --- Categories Write Ops ---
    suspend fun insertCategory(category: Category): Long {
        return expenseDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
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
                if (memberIds.isNotEmpty()) {
                    val splitAmount = sub.amount / memberIds.size

                    // 2. Insert expense
                    val expense = Expense(
                        spaceId = sub.spaceId,
                        paidById = sub.paidById,
                        description = "Recurring: ${sub.name}",
                        amount = sub.amount,
                        category = sub.category,
                        walletId = sub.walletId,
                        date = sub.nextDueDate // Use the scheduled due date
                    )
                    val expenseId = expenseDao.insertExpense(expense)

                    // 3. Insert splits for all members of the space
                    for (memberId in memberIds) {
                        expenseDao.insertExpenseSplit(
                            ExpenseSplit(
                                expenseId = expenseId,
                                userId = memberId,
                                amountOwed = splitAmount
                            )
                        )
                    }

                    // 4. Update wallet balance
                    val wallet = expenseDao.getWalletById(sub.walletId)
                    if (wallet != null) {
                        expenseDao.insertWallet(wallet.copy(balance = wallet.balance - sub.amount))
                    }
                }

                // 5. Calculate next due date
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
                expenseDao.insertSubscription(updatedSub)
                processedCount++
            }
        }
        return processedCount
    }
}
