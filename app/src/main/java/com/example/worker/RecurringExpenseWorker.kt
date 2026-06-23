package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository

class RecurringExpenseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RecurringExpenseWorker", "Recurring Expense Worker running...")

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())

        return try {
            val processed = repository.processOverdueSubscriptions()
            Log.d("RecurringExpenseWorker", "Successfully processed $processed recurring transactions!")
            Result.success()
        } catch (e: Exception) {
            Log.e("RecurringExpenseWorker", "Error processing recurring subscriptions", e)
            Result.failure()
        }
    }
}
