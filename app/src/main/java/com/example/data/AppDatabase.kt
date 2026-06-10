package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Space::class,
        SpaceMember::class,
        Expense::class,
        ExpenseSplit::class,
        Wallet::class,
        Category::class,
        Budget::class,
        Subscription::class
    ],
    version = 4,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_splitter_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                while (INSTANCE == null) {
                    kotlinx.coroutines.delay(20)
                }
                populateDatabase(INSTANCE!!.expenseDao())
            }
        }

        suspend fun populateDatabase(dao: ExpenseDao) {
            // Prepopulate some sample wallets with Indian Rupee scale values
            val w1 = dao.insertWallet(Wallet(name = "Cash Wallet", type = "Cash", balance = 5000.0))
            val w2 = dao.insertWallet(Wallet(name = "Bank Account", type = "Bank", balance = 25000.0))
            val w3 = dao.insertWallet(Wallet(name = "Credit Card", type = "Credit Card", balance = -2000.0))

            // Prepopulate some sample categories
            val catFood = dao.insertCategory(Category(name = "Food", iconName = "restaurant"))
            dao.insertCategory(Category(name = "Groceries", iconName = "shopping_cart", parentId = catFood))
            dao.insertCategory(Category(name = "Restaurants", iconName = "local_dining", parentId = catFood))

            val catTransport = dao.insertCategory(Category(name = "Transport", iconName = "directions_car"))
            dao.insertCategory(Category(name = "Fuel", iconName = "local_gas_station", parentId = catTransport))
            dao.insertCategory(Category(name = "Taxi", iconName = "local_taxi", parentId = catTransport))

            val catEntertainment = dao.insertCategory(Category(name = "Entertainment", iconName = "movie_creation"))
            dao.insertCategory(Category(name = "Movies", iconName = "movie", parentId = catEntertainment))
            dao.insertCategory(Category(name = "Events", iconName = "event", parentId = catEntertainment))

            val catGeneral = dao.insertCategory(Category(name = "General", iconName = "toll"))

            // Prepopulate some Budgets for "06/2026"
            dao.insertBudget(Budget(isGlobal = true, categoryName = null, limitAmount = 25000.0, monthYear = "06/2026"))
            dao.insertBudget(Budget(isGlobal = false, categoryName = "Groceries", limitAmount = 8000.0, monthYear = "06/2026"))
            dao.insertBudget(Budget(isGlobal = false, categoryName = "Fuel", limitAmount = 5000.0, monthYear = "06/2026"))
        }
    }
}
