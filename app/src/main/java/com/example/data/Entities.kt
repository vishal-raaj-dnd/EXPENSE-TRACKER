package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

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

@Entity(
    tableName = "space_members",
    foreignKeys = [
        ForeignKey(
            entity = Space::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["spaceId", "userId"], unique = true)
    ]
)
data class SpaceMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val userId: Long
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Space::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["paidById"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("spaceId"),
        Index("paidById"),
        Index("walletId")
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val paidById: Long, // ID of User who paid
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String = "General",
    val walletId: Long? = null,
    val attachmentUris: List<String> = emptyList()
)

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // e.g., "Cash", "Bank", "Credit Card"
    val balance: Double = 0.0
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index("parentId")
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "category",
    val parentId: Long? = null
)

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = Expense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("expenseId"),
        Index("userId")
    ]
)
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

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = Space::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["paidById"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("spaceId"),
        Index("paidById"),
        Index("walletId")
    ]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val paidById: Long,
    val name: String,
    val amount: Double,
    val category: String = "General",
    val walletId: Long? = null,
    val intervalType: String, // "Daily", "Weekly", "Monthly"
    val nextDueDate: Long,
    val isActive: Boolean = true
)
