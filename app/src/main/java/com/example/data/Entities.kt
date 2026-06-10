package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val avatarUrl: String = ""
)

@Entity(tableName = "spaces")
data class Space(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "space_members")
data class SpaceMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val userId: Long
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val paidById: Long, // ID of User who paid
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String = "General",
    val walletId: Long = 1L,
    val attachmentUris: List<String> = emptyList()
)

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // e.g., "Cash", "Bank", "Credit Card"
    val balance: Double = 0.0
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "category",
    val parentId: Long? = null
)

@Entity(tableName = "expense_splits")
data class ExpenseSplit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val userId: Long, // ID of User who owes money
    val amountOwed: Double
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isGlobal: Boolean,
    val categoryName: String?,
    val limitAmount: Double,
    val monthYear: String // e.g., "05/2026"
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val paidById: Long,
    val name: String,
    val amount: Double,
    val category: String = "General",
    val walletId: Long = 1L,
    val intervalType: String, // "Daily", "Weekly", "Monthly"
    val nextDueDate: Long,
    val isActive: Boolean = true
)
