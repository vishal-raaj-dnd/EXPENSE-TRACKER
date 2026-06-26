# VISH-tracker (Travel Split) â€” Solutions & Fixes

> All fixes are ordered by severity (Critical â†’ High â†’ Medium â†’ Low).
> Each fix includes exact file and line references.

---

## CRITICAL FIXES

### C1. `resolveDebts` infinite loop (ExpenseViewModel.kt:572-577)

**Fix:** Change strict `<` to `<=` on both index-advancement checks.

```kotlin
// OLD (line 572-577):
if (creditors[cIndex].second < 0.01) { cIndex++ }
if (debtors[dIndex].second < 0.01) { dIndex++ }

// NEW:
if (creditors[cIndex].second <= 0.01) { cIndex++ }
if (debtors[dIndex].second <= 0.01) { dIndex++ }
```

---

### C2. `formatAmount` wrong Indian grouping (ExpenseViewModel.kt:14)

**Fix:** Use `NumberFormat.getCurrencyInstance` with Indian locale, or set `groupingSize` correctly.

```kotlin
// Replace entire formatAmount helper:
private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val decimalFormatter: DecimalFormat = DecimalFormat("0.00")

fun formatAmount(amount: Double): String {
    // Use a pattern that groups correctly for Indian numbering:
    // First group of 3, then groups of 2 from right
    val formatter = DecimalFormat("#,##,##0.00")
    return formatter.format(amount)
}
```

For Indian grouping to work reliably across all Android versions, use ICU4J or a custom formatter:
```kotlin
fun formatAmount(amount: Double): String {
    val s = String.format(Locale.US, "%.2f", amount)
    val parts = s.split(".")
    val intPart = parts[0]
    val decPart = parts[1]
    
    // Build Indian grouping: last 3 digits, then groups of 2
    val result = StringBuilder()
    val rev = intPart.reversed()
    rev.forEachIndexed { index, c ->
        if (index > 0 && (index % 2 == 0)) result.append(",")
        result.append(c)
    }
    return "â‚¹${result.reverse()}${decPart}"
}
```

---

### C3. Member removal deletes entire expenses paid by removed user (ExpenseDao.kt:240-244)

**Fix:** Instead of deleting the expense, reassign `paidById` to another participant.

```kotlin
// OLD (line 240-244):
val expensesPaidByUser = getExpensesPaidByUser(spaceId, userId)
for (exp in expensesPaidByUser) {
    deleteExpenseById(exp.id)
}

// NEW:
val expensesPaidByUser = getExpensesPaidByUser(spaceId, userId)
for (exp in expensesPaidByUser) {
    val remainingSplits = getSplitsForExpenseSync(exp.id)
        .filter { it.userId != userId }
    if (remainingSplits.isEmpty()) {
        deleteExpenseById(exp.id)
    } else {
        // Reassign to the participant who paid the most (or the first remaining)
        val newPayerId = remainingSplits.first().userId
        reassignExpensePayer(exp.id, newPayerId)
    }
}
```

Add a new DAO method:
```kotlin
@Query("UPDATE expenses SET paidById = :newUserId WHERE id = :expenseId")
suspend fun reassignExpensePayer(expenseId: Long, newUserId: Long)
```

---

### C4. Delete parent category orphans children (ExpenseRepository.kt:85-88)

**Fix:** Cascade-delete children or prevent deletion with warning.

```kotlin
// In ExpenseRepository.kt:
suspend fun deleteCategory(category: Category) {
    expenseDao.deleteChildCategories(category.id)  // NEW: delete children first
    expenseDao.deleteBudgetsByCategoryName(category.name)
    expenseDao.deleteCategory(category)
}
```

In ExpenseDao.kt, add:
```kotlin
@Query("DELETE FROM categories WHERE parentId = :categoryId")
suspend fun deleteChildCategories(categoryId: Long)
```

**Alternative (safer):** Show a dialog: "This category has N child categories. Delete all or cancel?"

---

### C5. Wallet deletion orphans expense references (ExpenseDao.kt:223-226)

**Fix:** Update orphaned expenses to use a default/null wallet before deleting.

```kotlin
// In deleteWalletCascade, add before deleting the wallet:
@Query("UPDATE expenses SET walletId = 1 WHERE walletId = :walletId")
suspend fun reassignExpensesWallet(walletId: Long, defaultWalletId: Long = 1)
```

Or make `walletId` nullable on `Expense` entity and set to `null`:
```kotlin
@Query("UPDATE expenses SET walletId = NULL WHERE walletId = :walletId")
suspend fun nullifyExpensesWallet(walletId: Long)
```

---

## HIGH FIXES

### H1. Export not working â€” FileProvider path mismatch

**Fix:** Change `file_paths.xml` to expose the entire cache directory:

```xml
<!-- res/xml/file_paths.xml -->
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="cache" path="." />
</paths>
```

**Alternative (cleaner):** Write export files to `cache/shared/` subdirectory:
```kotlin
// In ExportEngine.kt, change writeXlsxFile:
val sharedDir = File(context.cacheDir, "shared").also { it.mkdirs() }
val file = File(sharedDir, filename)
```
And similarly for CSV (`File(context.cacheDir, ...)` â†’ `File(sharedDir, ...)`) and PDF.

---

### H2. Wallet deduction not wrapped in transaction (ExpenseRepository.kt:134-137)

**Fix:** Wrap in a `@Transaction` DAO method.

```kotlin
// In ExpenseDao.kt, add:
@Transaction
suspend fun processSubscriptionPayment(sub: Subscription, expense: Expense, splits: List<ExpenseSplit>) {
    insertExpenseWithSplits(expense, splits)
    deductFromWallet(sub.walletId, sub.amount)
    insertSubscription(sub)
}
```

Then call `expenseDao.processSubscriptionPayment(...)` from the repository instead of three separate calls.

---

### H3. `settleDebt` crash with no wallets (ExpenseViewModel.kt:507)

**Fix:** Guard against empty wallet list.

```kotlin
// OLD:
val wallets = repository.allWallets.first()
val walletId = wallets.firstOrNull()?.id ?: 1L

// NEW:
val wallets = repository.allWallets.first()
if (wallets.isEmpty()) {
    // Show error toast
    return@launch
}
val walletId = wallets.first().id
```

---

### H4. DB init race condition (AppDatabase.kt:65-67)

**Fix:** Pass `ExpenseDao` directly to the callback instead of reading from `INSTANCE`.

```kotlin
// In AppDatabase, change:
.addCallback(AppDatabaseCallback(scope))

// To pass the database after building:
val instance = Room.databaseBuilder(...)
    .build()
INSTANCE = instance
// Now populate in a coroutine
scope.launch(Dispatchers.IO) {
    instance.expenseDao()?.let { populateDatabase(it) }
}
```

Or simply use `addCallback` with a properly initialized scope:
```kotlin
private class AppDatabaseCallback(
    private val scope: CoroutineScope,
    private val dao: Lazy<ExpenseDao>  // Use Lazy to avoid init ordering issues
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch(Dispatchers.IO) {
            delay(100)
            dao.get().let { populateDatabase(it) }
        }
    }
}
```

---

### H5. Subscription `nextDueDate` set 1 second in the past (FinancialPlanningScreens.kt:569)

**Fix:** Set to one month in the future (or user-selected date).

```kotlin
// OLD:
nextDueDate = System.currentTimeMillis() - 1000L

// NEW (30 days from now):
nextDueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
```

---

### H6. `Uri.fromFile()` deprecated for attachments (ExpenseSplitterApp.kt:2610, 4906)

**Fix:** Use `FileProvider.getUriForFile()`.

```kotlin
// OLD (line 2610):
android.net.Uri.fromFile(persistentFile).toString()

// NEW:
val uri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    persistentFile
)
uri.toString()
```

Same fix for `copyUriToLocal` at line 4906:
```kotlin
// OLD:
return Uri.fromFile(file).toString()

// NEW:
val uri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    file
)
return uri.toString()
```

---

### H7. Equal split rounding error (ExpenseViewModel.kt:320)

**Fix:** Distribute the remainder to the last participant.

```kotlin
// OLD:
val perPerson = amount / participantIds.size
val splits = participantIds.map { ExpenseSplit(expenseId = 0, userId = it, amountOwed = perPerson) }

// NEW:
val size = participantIds.size
val perPerson = (amount * 100 / size) / 100.0  // floor to 2 decimal places
val remainder = Math.round((amount - perPerson * (size - 1)) * 100.0) / 100.0

val splits = participantIds.mapIndexed { index, userId ->
    ExpenseSplit(
        expenseId = 0,
        userId = userId,
        amountOwed = if (index == size - 1) remainder else perPerson
    )
}
```

---

### H8. All split modes rounding errors (ExpenseViewModel.kt:347-417)

**Fix:** Apply the same remainder-distribution pattern in all split modes.

For exact split mode, validate that `splitsMap.values.sum() == amount` (within 0.01 tolerance).

For percentage split mode:
```kotlin
val totalPercentage = percentageMap.values.sum()
if (Math.abs(totalPercentage - 100.0) > 0.01) {
    // Show error
    return@launch
}
// Convert to amounts and apply rounding correction
val splits = percentageMap.map { (userId, pct) ->
    val rawAmount = amount * pct / 100.0
    ExpenseSplit(expenseId = 0, userId = userId, amountOwed = Math.round(rawAmount * 100.0) / 100.0)
}
val totalAllocated = splits.sumOf { it.amountOwed }
val diff = Math.round((amount - totalAllocated) * 100.0) / 100.0
if (Math.abs(diff) > 0.001) {
    // Add/subtract difference from the largest split
    val maxSplit = splits.maxByOrNull { it.amountOwed }
    // adjust maxSplit by diff
}
```

---

### H9. Settlement uses wrong wallet (ExpenseViewModel.kt:507-508)

**Fix:** Include a wallet selector in the settlement flow.

```kotlin
// In settleDebt, add walletId parameter:
fun settleDebt(spaceId: Long, debtorId: Long, creditorId: Long, amount: Double, walletId: Long) {
    viewModelScope.launch {
        val wallets = repository.allWallets.first()
        if (wallets.isEmpty()) {
            // Error: no wallets
            return@launch
        }
        // Use the provided walletId (passed from UI)
        val expense = Expense(
            spaceId = spaceId,
            paidById = debtorId,
            description = "Settle Up (Payment to Creditor)",
            amount = amount,
            walletId = walletId,  // Use the passed walletId
            // ...
        )
        // ...
    }
}
```

---

### H10. Redistribution excludes payer from shortfall (ExpenseDao.kt:259-260)

**Fix:** Redistribute among ALL remaining participants, including the payer.

```kotlin
// OLD:
val nonPayerSplits = remainingSplits.filter { it.userId != expense.paidById }
if (nonPayerSplits.isNotEmpty()) {
    val extraPerPerson = shortfall / nonPayerSplits.size
    for (split in nonPayerSplits) {
        updateSplitAmount(split.id, split.amountOwed + extraPerPerson)
    }
}

// NEW:
if (remainingSplits.isNotEmpty()) {
    val extraPerPerson = shortfall / remainingSplits.size
    for (split in remainingSplits) {
        updateSplitAmount(split.id, split.amountOwed + extraPerPerson)
    }
}
```

---

### H11. No foreign key constraints (Entities.kt)

**Fix:** Add `@ForeignKey` annotations to all relevant entities.

```kotlin
// Example for Expense:
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
    ]
)
```

Note: Adding foreign keys requires a database migration (version bump). Consider adding in next schema update.

---

### H12. XLSX stores amounts as text (ExportEngine.kt:73-78)

**Fix:** Write raw numeric values (without commas) for XLSX number cells.

```kotlin
// In buildSheetXml, change:
val numVal = cellValue.toDoubleOrNull()
if (numVal != null) {
    sb.append("""<c r="$cellRef"><v>$numVal</v></c>""")
} else {
    // ...
}

// For XLSX, pass FORMATTED values for display and RAW values for computation.
// Better approach: change exportSpaceToExcel to pass raw amounts (Double) 
// formatted only for display. Use a wrapper class:
data class XlsxCell(val displayValue: String, val numericValue: Double? = null)
```

For a quick fix, remove the comma from `formatAmount` in XLSX export:
```kotlin
// In exportSpaceToExcel, use a separate format without commas:
private fun xlsxAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)
```

---

### H13. `deleteUser` no cascade (ExpenseDao.kt:20)

**Fix:** Add a `@Transaction` cascade method.

```kotlin
@Transaction
suspend fun deleteUserCascade(user: User) {
    // Remove from spaces
    deleteUserFromAllSpaces(user.id)
    // Delete or reassign expenses they paid
    // Delete or nullify their expense splits
    deleteUser(user)  // The original @Delete
}

@Query("DELETE FROM space_members WHERE userId = :userId")
suspend fun deleteUserFromAllSpaces(userId: Long)

@Query("DELETE FROM expense_splits WHERE userId = :userId")
suspend fun deleteUserSplits(userId: Long)
```

Called from repository:
```kotlin
suspend fun deleteUser(user: User) {
    expenseDao.deleteUserCascade(user)
}
```

---

### H14. No negative balance protection on wallet deduction (ExpenseDao.kt:110-111)

**Fix:** Add `WHERE balance >= :amount` guard and check affected rows.

```kotlin
// OLD:
@Query("UPDATE wallets SET balance = balance - :amount WHERE id = :walletId")
suspend fun deductFromWallet(walletId: Long, amount: Double)

// NEW:
@Query("UPDATE wallets SET balance = balance - :amount WHERE id = :walletId AND balance >= :amount")
suspend fun deductFromWallet(walletId: Long, amount: Double): Int  // Returns rows affected

// In repository:
val affected = expenseDao.deductFromWallet(walletId, amount)
if (affected == 0) {
    throw InsufficientBalanceException("Insufficient balance in wallet")
}
```

---

## MEDIUM FIXES

### M1. Integer overflow in Cash Counter (ToolsScreen.kt:968)

**Fix:** Cast to `Long` before multiplication:
```kotlin
val subtotals = denominations.associateWith { it.toLong() * (counts[it] ?: 0) }
```

### M2. EMI silently discards fractional years (ToolsScreen.kt:141)

**Fix:** Use `toDoubleOrNull()` and convert to months:
```kotlin
val years = yearsText.toDoubleOrNull() ?: 0.0
if (years <= 0.0) { /* error */ }
val n = (years * 12).toInt()  // months
```

### M3. Cash counter "Delete" no confirmation (ToolsScreen.kt:1470-1482)

**Fix:** Add `AlertDialog` confirmation before clearing.

### M4. New wallet invalid input â†’ 0.0 silently (WalletManagerScreen.kt:205)

**Fix:** Show error text/Toast instead of silently defaulting:
```kotlin
val bal = walletBalance.toDoubleOrNull()
if (bal == null) {
    Toast.makeText(context, "Invalid balance amount", Toast.LENGTH_SHORT).show()
    return@launch
}
```

### M5. Wallet editing loses precision (WalletManagerScreen.kt:294)

**Fix:**
```kotlin
walletBalance = String.format(Locale.US, "%.2f", wallet.balance)
```

### M6. No duplicate category name check (CategoryManagerScreen.kt:234)

**Fix:** Add a query + check before inserting:
```kotlin
@Query("SELECT COUNT(*) FROM categories WHERE name = :name")
suspend fun getCategoryCountByName(name: String): Int
```

### M7. Division by zero â†’ 0.0 (ExpenseSplitterApp.kt:144)

**Fix:**
```kotlin
left = if (right != 0.0) left / right else Double.NaN
```

### M8. Invalid chars silently skipped (ExpenseSplitterApp.kt:168-172)

**Fix:** Throw on invalid characters, let the catch block at line 121-123 handle it.

### M9. Mismatched parentheses ignored (ExpenseSplitterApp.kt:152-155)

**Fix:** Track paren depth and validate at end.

### M10. QR scanner TOCTOU race (ExpenseSplitterApp.kt:670-691)

**Fix:** Use `AtomicBoolean`:
```kotlin
val isProcessing = remember { mutableStateOf(AtomicBoolean(false)) }
```

### M11. Create Date filter uses `id` (ExpenseSplitterApp.kt:1811)

**Fix:** Use `expense.date` instead of `expense.id`:
```kotlin
val sorted = expenses.sortedByDescending { it.date }
```

### M12. No onboarding reset (ExpenseSplitterApp.kt:366)

**Fix:** Add a "Reset App" button in Profile screen:
```kotlin
// Clears DB and SharedPreferences
viewModel.clearAllData()
preferences.edit().clear().apply()
```

### M13. Theme toggle not persisted (MainActivity.kt:45)

**Fix:** Store in SharedPreferences:
```kotlin
val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
// On change: prefs.edit().putBoolean("dark", isDarkTheme).apply()
```

### M14. DB init on main thread (MainActivity.kt:29-41)

**Fix:** Wrap in `lifecycleScope.launch(Dispatchers.IO)`:
```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
    val repository = ExpenseRepository(database.expenseDao())
    // Post back to main thread for UI
    withContext(Dispatchers.Main) {
        // ... launch compose
    }
}
```

### M15. XLSX column letter overflow (ExportEngine.kt:70-71)

**Fix:** Implement proper base-26 column letter logic:
```kotlin
fun columnLetter(index: Int): String {
    var i = index
    val sb = StringBuilder()
    while (i >= 0) {
        sb.append(('A' + (i % 26)))
        i = i / 26 - 1
    }
    return sb.reverse().toString()
}
```

### M16. Expense insert + wallet deduction not atomic (ExpenseRepository.kt:134-137)

**Fix:** Same as H2 â€” wrap in `@Transaction`.

### M17. No subscription cleanup on member removal (ExpenseDao.kt:238-272)

**Fix:**
```kotlin
@Query("DELETE FROM subscriptions WHERE spaceId = :spaceId AND paidById = :userId")
suspend fun deleteSubscriptionsForUserInSpace(spaceId: Long, userId: Long)
```

### M18. Hardcoded `walletId = 1L` (Entities.kt:37,82)

**Fix:** Make `walletId` nullable:
```kotlin
val walletId: Long? = null
```

### M19. Budget deletion by name may over-delete (ExpenseRepository.kt:85-87)

**Fix:** Add unique constraint on category name, or delete budgets by category ID.

### M20. Wallet update via insert REPLACE (WalletManagerScreen.kt:208-213)

**Fix:** Add a dedicated `@Update` or `@Query` in DAO:
```kotlin
@Query("UPDATE wallets SET name = :name, type = :type, balance = :balance WHERE id = :id")
suspend fun updateWallet(id: Long, name: String, type: String, balance: Double)
```

### M21. Icon name case sensitivity (CategoryManagerScreen.kt:357)

**Fix:** Normalize on save:
```kotlin
iconName = selectedIconName.lowercase()
```

### M22. QR code exceeds capacity for large spaces (ExpenseViewModel.kt:583-623)

**Fix:** Implement multi-QR fallback or server-based sharing.

### M23. TITLE filter useless (ExpenseSplitterApp.kt:1825-1826)

**Fix:** Group by first letter:
```kotlin
sorted.groupBy { it.description.firstOrNull()?.uppercase() ?: "#" }
```

### M24. Item names not trimmed (ExpenseSplitterApp.kt:3707)

**Fix:**
```kotlin
name = nextItemName.trim()
```

### M25. Calculator "=" replaces expression (ExpenseSplitterApp.kt:302-304)

**Fix:** Keep expression and show result separately:
```kotlin
expression = "$expression = $resultText"  // or keep expression separate
```

### M26. No validation for unrealistic tax rates (ToolsScreen.kt:665-678)

**Fix:** Cap at 100%:
```kotlin
val clampedRate = taxRate.coerceIn(0.0, 100.0)
```

---

## LOW FIXES

### L1. CoroutineScope leak in worker (RecurringExpenseWorker.kt:20)

**Fix:** Use `this` (CoroutineWorker implements CoroutineScope) or a shared scope.

### L2. KEEP policy prevents interval updates (MainActivity.kt:37-41)

**Fix:** Change to `CANCEL_AND_REENQUEUE`.

### L3. `|||` delimiter fragile (Converters.kt:7-16)

**Fix:** Use `kotlinx.serialization.json.Json` for proper serialization.

### L4. Composite PK no single ID (Entities.kt:22-26)

**Fix:** Add auto-generated `id` primary key, keep unique constraint on `(spaceId, userId)`.

### L5. Longâ†’Double precision loss (ExpenseViewModel.kt:18-26)

**Fix:** Use `DecimalFormat("##,##,##,##0")` for Long/Int overloads.

### L6. Inconsistent year parsing (ToolsScreen.kt:141 vs 367)

**Fix:** Use `toDoubleOrNull()` consistently.

### L7. Unnecessary nullable access (ExpenseSplitterApp.kt:1668)

**Fix:** Remove `?`: `expense.attachmentUris.firstOrNull()`.

### L8. No `id > 0L` guard (ExpenseDao.kt:200-206)

**Fix:** Add `require(expense.id > 0L)`.

### L9. Magic number 5 (ExpenseSplitterApp.kt:2644)

**Fix:** Extract constant: `const val MAX_ATTACHMENTS = 5`.

### L10. Category sorting (ExpenseDao.kt:114-115)

**Fix:** Add `ORDER BY name ASC` or add `sortOrder` column.

---

## EXPORT FIX SUMMARY (User-Reported Issue)

The "export options not working" issue has **two root causes**:

| # | Problem | File | Fix | Impact |
|---|---------|------|-----|--------|
| 1 | FileProvider can't find export files | `res/xml/file_paths.xml:3` | Change `<cache-path ... path="shared/" />` to `<cache-path ... path="." />` OR write files to `cache/shared/` | Without this, ALL exports crash silently (catch block in `shareFile` shows "Sharing failed") |
| 2 | XLSX amounts stored as text | `ExportEngine.kt:73-78` | Write raw numeric values (no commas) for number cells | Without this, exported XLSX files have amounts as text â€” cannot sum/formula in Excel |

**Recommended quick fix for #1** (change file_paths.xml):
```xml
<cache-path name="cache" path="." />
```

**Recommended complete fix for #1** (write to shared subdirectory â€” more secure):
```kotlin
// In ExportEngine, update writeXlsxFile:
val sharedDir = File(context.cacheDir, "shared").also { it.mkdirs() }
val file = File(sharedDir, filename)
```
Apply same pattern to `exportSpaceToCSV` (line 242), `generateSpaceReportPDF` (line 502), `exportWalletToCSV` (line 269), and `generateWalletReportPDF` (line 689).
