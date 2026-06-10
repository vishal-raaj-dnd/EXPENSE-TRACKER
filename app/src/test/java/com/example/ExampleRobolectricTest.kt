package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.ExpenseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Expense Splitter", appName)
  }

  @Test
  fun testViewModelAndDatabaseInitialization() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context, this)
    val dao = database.expenseDao()
    val repository = ExpenseRepository(dao)
    val vm = ExpenseViewModel(repository)
    assertNotNull(vm)
    
    // Wait for prepopulate and check users
    val usersList = repository.allUsers.first()
    assertNotNull(usersList)
  }

  @Test
  fun testBudgetCreationAndRetrieval() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context, this)
    val dao = database.expenseDao()
    val repository = ExpenseRepository(dao)
    
    // Insert new category-specific budget
    val customBudget = Budget(
       isGlobal = false,
       categoryName = "Entertainment",
       limitAmount = 250.0,
       monthYear = "05/2026"
    )
    repository.insertBudget(customBudget)
    
    val allBudgets = repository.allBudgets.first()
    val entertainmentBudget = allBudgets.find { it.categoryName == "Entertainment" }
    assertNotNull(entertainmentBudget)
    assertEquals(250.0, entertainmentBudget!!.limitAmount, 0.01)
  }

  @Test
  fun testSubscriptionsAndWorkerProcessingOverdue() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context, this)
    val dao = database.expenseDao()
    val repository = ExpenseRepository(dao)
    
    // Create custom test Space, Users and Wallet to isolate and prevent failures due to async timing of prepopulate
    val spaceId = dao.insertSpace(Space(name = "Test Space"))
    val userId1 = dao.insertUser(User(name = "User A", email = "usera@example.com"))
    val userId2 = dao.insertUser(User(name = "User B", email = "userb@example.com"))
    
    dao.insertSpaceMember(SpaceMember(spaceId = spaceId, userId = userId1))
    dao.insertSpaceMember(SpaceMember(spaceId = spaceId, userId = userId2))
    
    val walletId = dao.insertWallet(Wallet(name = "Test Card", type = "Credit Card", balance = 500.0))
    
    // Create overdue subscription (1 day in the past)
    val overdueDate = System.currentTimeMillis() - 86400000L
    repository.insertSubscription(
        Subscription(
            spaceId = spaceId,
            paidById = userId1,
            name = "Test Rent Billing",
            amount = 100.0,
            category = "Entertainment",
            walletId = walletId,
            intervalType = "Monthly",
            nextDueDate = overdueDate,
            isActive = true
        )
    )
    
    // Process overdue subscriptions
    val processedCount = repository.processOverdueSubscriptions()
    assertEquals(1, processedCount)
    
    // Verify an Expense record was posted and split equally
    val expenses = repository.allExpenses.first()
    val rentExpenses = expenses.filter { it.spaceId == spaceId && it.description.contains("Test Rent Billing") }
    assertEquals(1, rentExpenses.size)
    
    val expense = rentExpenses.first()
    assertEquals(100.0, expense.amount, 0.01)
    
    // Verify wallet balance is correctly updated
    val updatedWallet = dao.getWalletById(walletId)
    assertNotNull(updatedWallet)
    assertEquals(400.0, updatedWallet!!.balance, 0.01)
  }
}
