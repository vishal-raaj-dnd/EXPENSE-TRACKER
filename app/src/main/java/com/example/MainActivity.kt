package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
        enableEdgeToEdge()

        val sharedPrefs = getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val initialTheme = sharedPrefs.getBoolean("is_dark", true)
        val initialFontScale = sharedPrefs.getFloat("font_scale", 1.0f)

        setContent {
            var isDarkTheme by remember { mutableStateOf(initialTheme) }
            var fontScale by remember { mutableStateOf(initialFontScale) }
            var dbReady by remember { mutableStateOf(false) }
            var repository by remember { mutableStateOf<ExpenseRepository?>(null) }

            LaunchedEffect(Unit) {
                val database = AppDatabase.getDatabase(applicationContext)
                repository = ExpenseRepository(database.expenseDao())

                val recurringWorkRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
                    15, TimeUnit.MINUTES
                ).build()
                WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                    "RecurringExpenseWorker",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    recurringWorkRequest
                )
                dbReady = true
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val originalDensity = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                        density = originalDensity.density,
                        fontScale = originalDensity.fontScale * fontScale
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (dbReady && repository != null) {
                            val vm: ExpenseViewModel = viewModel(
                                factory = ExpenseViewModelFactory(repository!!)
                            )
                            ExpenseSplitterApp(
                                viewModel = vm,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = {
                                    isDarkTheme = !isDarkTheme
                                    sharedPrefs.edit().putBoolean("is_dark", isDarkTheme).apply()
                                },
                                fontScale = fontScale,
                                onFontScaleChange = { newScale ->
                                    fontScale = newScale
                                    sharedPrefs.edit().putFloat("font_scale", newScale).apply()
                                }
                            )
                        } else {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
