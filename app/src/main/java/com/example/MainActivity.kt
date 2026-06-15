package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.ui.ExpenseSplitterApp
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.RecurringExpenseWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database and Repository
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = ExpenseRepository(database.expenseDao())
        
        // Enqueue Unique Periodic Work for Subscription processing
        val recurringWorkRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "RecurringExpenseWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringWorkRequest
        )

        enableEdgeToEdge()
        setContent {
            val isDarkThemeState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkThemeState.value) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val vm: ExpenseViewModel = viewModel(
                        factory = ExpenseViewModelFactory(repository)
                    )
                    ExpenseSplitterApp(viewModel = vm, isDarkTheme = isDarkThemeState.value, onThemeToggle = { isDarkThemeState.value = !isDarkThemeState.value })
                }
            }
        }
    }
}

