package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    version = 7,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS space_members_new (spaceId INTEGER NOT NULL, userId INTEGER NOT NULL, PRIMARY KEY (spaceId, userId))")
            db.execSQL("INSERT OR IGNORE INTO space_members_new (spaceId, userId) SELECT spaceId, userId FROM space_members")
            db.execSQL("DROP TABLE space_members")
            db.execSQL("ALTER TABLE space_members_new RENAME TO space_members")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_splitter_db"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback())
                .build()
                INSTANCE = instance
                instance.populateDatabaseIfEmpty()
                instance
            }
        }

        private fun AppDatabase.populateDatabaseIfEmpty() {
            kotlinx.coroutines.runBlocking {
                val dao = expenseDao()
                if (dao.getWalletCount() == 0) {
                    populateDatabase(dao)
                }
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
        }
    }

    private suspend fun populateDatabase(dao: ExpenseDao) {
            val w1 = dao.insertWallet(Wallet(name = "Cash Wallet", type = "Cash", balance = 5000.0))
            val w2 = dao.insertWallet(Wallet(name = "Bank Account", type = "Bank", balance = 25000.0))
            val w3 = dao.insertWallet(Wallet(name = "Credit Card", type = "Credit Card", balance = -2000.0))

            val catEntertainment = dao.insertCategory(Category(name = "Entertainment", iconName = "movie"))
            dao.insertCategory(Category(name = "Movies", iconName = "movie", parentId = catEntertainment))

            dao.insertCategory(Category(name = "Extra", iconName = "category"))
            dao.insertCategory(Category(name = "Fees", iconName = "payments"))

            val catFood = dao.insertCategory(Category(name = "Food", iconName = "restaurant"))
            dao.insertCategory(Category(name = "Groceries", iconName = "shopping_cart", parentId = catFood))
            dao.insertCategory(Category(name = "Restaurants", iconName = "restaurant", parentId = catFood))

            dao.insertCategory(Category(name = "Gifts", iconName = "spa"))
            dao.insertCategory(Category(name = "Hospital", iconName = "spa"))
            dao.insertCategory(Category(name = "Lodge", iconName = "home"))

            val catSchool = dao.insertCategory(Category(name = "School & College", iconName = "school"))
            dao.insertCategory(Category(name = "Tuition Fees", iconName = "payments", parentId = catSchool))

            dao.insertCategory(Category(name = "Service", iconName = "category"))
            dao.insertCategory(Category(name = "Shopping", iconName = "shopping_cart"))
            dao.insertCategory(Category(name = "Snacks", iconName = "restaurant"))
            dao.insertCategory(Category(name = "Temple", iconName = "home"))

            val catTransport = dao.insertCategory(Category(name = "Transport", iconName = "directions_car"))
            dao.insertCategory(Category(name = "Fuel", iconName = "directions_car", parentId = catTransport))

            val currentMonthYear = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.US).format(java.util.Date(System.currentTimeMillis()))
            dao.insertBudget(Budget(isGlobal = true, categoryName = null, limitAmount = 25000.0, monthYear = currentMonthYear))
            dao.insertBudget(Budget(isGlobal = false, categoryName = "Groceries", limitAmount = 8000.0, monthYear = currentMonthYear))
            dao.insertBudget(Budget(isGlobal = false, categoryName = "Fuel", limitAmount = 5000.0, monthYear = currentMonthYear))
        }
}
