package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

class RecurringExpenseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RecurringExpenseWorker", "Recurring Expense Worker running...")
        
        val database = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
        val repository = ExpenseRepository(database.expenseDao())

        try {
            val processed = repository.processOverdueSubscriptions()
            Log.d("RecurringExpenseWorker", "Successfully processed $processed recurring transactions!")
            return Result.success()
        } catch (e: Exception) {
            Log.e("RecurringExpenseWorker", "Error processing recurring subscriptions", e)
            return Result.failure()
        }
    }
}
