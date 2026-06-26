# VISH-tracker (Travel Split) â€” Complete Bug Report

> Total: **56 bugs** (5 Critical, 14 High, 26 Medium, 11 Low)

---

## CRITICAL (Data Loss / Crash)

### C1. `resolveDebts` infinite loop when floating-point remainder hits exactly 0.01
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:572-577`
- **Condition:** `< 0.01` does not advance index when remainder is *exactly* `0.01` â†’ infinite `while` loop â†’ ANR

### C2. `formatAmount` uses wrong Indian grouping pattern
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:14`
- **Pattern:** `"##,##,##,##0.00"` produces incorrect grouping (`1,23,45,67.89` instead of `12,34,567.89`)
- **Affects:** Every monetary display in the entire app

### C3. Member removal deletes entire expenses paid by removed user (including others' splits)
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:240-244`
- **Issue:** If user B is removed, expenses where B was the payer are **fully deleted**, including all other participants' split contributions

### C4. Deleting a parent category orphans child categories
- **File:** `app/src/main/java/com/example/ui/CategoryManagerScreen.kt:297` â†’ `ExpenseRepository.kt:85-88`
- **Issue:** `deleteCategory` only deletes budgets by name + the category itself. Child categories with `parentId` pointing to the deleted category become orphaned

### C5. Wallet deletion orphans expense wallet references
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:223-226`
- **Issue:** `deleteWalletCascade` deletes subscriptions + wallet but leaves `Expense.walletId` as a dangling reference to a deleted wallet

---

## HIGH (Logic / Data Integrity / Crash)

### H1. Export not working â€” FileProvider path mismatch
- **File:** `app/src/main/res/xml/file_paths.xml:3` + `app/src/main/java/com/example/utils/ExportEngine.kt:86-101`
- **Root cause of user-reported "export options not working"**
- **Issue:** Export files are written to `context.cacheDir/filename`, but FileProvider at `file_paths.xml` only exposes `cache/shared/`. `FileProvider.getUriForFile()` throws `IllegalArgumentException: Failed to find configured root`

### H2. Wallet deduction not wrapped in transaction
- **File:** `app/src/main/java/com/example/data/ExpenseRepository.kt:134-137`
- **Issue:** `deductFromWallet` runs before `insertSubscription(updatedSub)` with no `@Transaction`. DB crash after deduction but before insert â†’ money lost

### H3. `settleDebt` crash with no wallets
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:507`
- **Issue:** Uses `repository.allWallets.first()` â†’ `NoSuchElementException` on empty wallet list

### H4. DB init race condition
- **File:** `app/src/main/java/com/example/data/AppDatabase.kt:65-67`
- **Issue:** `onCreate` fires before `INSTANCE` assignment. `delay(100)` workaround is fragile. Pre-population silently skipped if timing is off

### H5. Subscription `nextDueDate` set 1 second in the past
- **File:** `app/src/main/java/com/example/ui/FinancialPlanningScreens.kt:569`
- **Issue:** `System.currentTimeMillis() - 1000L` â†’ subscription immediately overdue on first worker run

### H6. `Uri.fromFile()` for camera/gallery attachments (deprecated since API 24)
- **File:** `ExpenseSplitterApp.kt:2610` (camera) and `ExpenseSplitterApp.kt:4906` (gallery)
- **Issue:** `Uri.fromFile()` returns `file://` URI â†’ deprecated, may throw `FileUriExposedException` under StrictMode, cannot be shared across apps

### H7. Equal split rounding error â€” total â‰  sum of splits
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:320`
- **Issue:** `amount / size` rounded to 2 decimal places per split â†’ `100/3 = 33.33Ã—3 = 99.99 â‰  100`. 0.01 is lost

### H8. All split modes have rounding errors
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:347-417`
- **Issue:** Exact, percentage, and itemized split modes also have sum-of-splits â‰  total rounding issues

### H9. Settlement always uses first wallet, not user's selected wallet
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:507-508`
- **Issue:** `wallets.first()` â†’ arbitrary wallet selection. No user choice

### H10. Redistribution on member removal excludes payer from shortfall
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:259-260`
- **Issue:** Shortfall redistributed only among non-payers. Payer's share (as a consumer) is ignored â†’ incorrect balances

### H11. No foreign key constraints on any entity
- **File:** `app/src/main/java/com/example/data/Entities.kt` (entire file)
- **Issue:** No `@ForeignKey` â†’ Room does not enforce referential integrity. Manual cascades are incomplete everywhere

### H12. XLSX stores amounts as text strings, not numbers
- **File:** `app/src/main/java/com/example/utils/ExportEngine.kt:73-78`
- **Issue:** `formatAmount()` returns comma-formatted string â†’ `toDoubleOrNull()` returns null â†’ stored as inline string. Cannot be summed in Excel

### H13. `deleteUser` has no cascade â€” orphans everywhere
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:20`
- **Issue:** `@Delete` removes user but leaves orphaned `space_members`, `expense`, `expense_split` rows

### H14. `noNegativeBalance` protection missing on wallet deduction
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:110-111`
- **Issue:** `UPDATE wallets SET balance = balance - :amount` has no `WHERE balance >= :amount` guard. Can go arbitrarily negative

---

## MEDIUM

### M1. Integer overflow in Cash Counter denomination multiplication
- **File:** `app/src/main/java/com/example/ui/ToolsScreen.kt:968`
- **Issue:** `Int Ã— Int` overflows for large counts (e.g., 2000 Ã— 2M)

### M2. EMI calculator silently discards fractional year input
- **File:** `app/src/main/java/com/example/ui/ToolsScreen.kt:141`
- **Issue:** `toIntOrNull()` converts 0.5 â†’ null â†’ 0 â†’ triggers `years <= 0` guard â†’ shows â‚¹0 EMI with no error

### M3. Cash counter "Delete" has no confirmation
- **File:** `app/src/main/java/com/example/ui/ToolsScreen.kt:1470-1482`
- **Issue:** Destructive clear with no confirmation dialog â†’ accidental data loss

### M4. New wallet silently defaults balance to 0.0 on invalid input
- **File:** `app/src/main/java/com/example/ui/WalletManagerScreen.kt:205`
- **Issue:** `toDoubleOrNull() ?: 0.0` â€” garbage input silently becomes 0.0

### M5. Wallet editing loses precision on `balance.toString()`
- **File:** `app/src/main/java/com/example/ui/WalletManagerScreen.kt:294`
- **Issue:** `0.1 + 0.2 = 0.30000000000000004` displayed as-is

### M6. Category Manager: no duplicate name check
- **File:** `app/src/main/java/com/example/ui/CategoryManagerScreen.kt:234`
- **Issue:** Multiple categories with same name can be created, breaking budget-to-category mapping

### M7. Expression parser division by zero returns 0.0 silently
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:144`
- **Issue:** `1/0 = 0.0` instead of error

### M8. Expression parser silently skips invalid characters
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:168-172`
- **Issue:** `1+a` â†’ `1+0 = 1.0`. No error signaled

### M9. Expression parser ignores mismatched parentheses
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:152-155`
- **Issue:** `(1+2` = 3.0, `1+2)` = 3.0. No error

### M10. QR scanner has TOCTOU race condition on `isProcessing` guard
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:670-691`
- **Issue:** Two concurrent barcode scans can both pass `!isProcessing` check â†’ double import

### M11. Create Date filter sorts by `id`, not by `date` field
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:1811`
- **Issue:** `sortedByDescending { it.id }` â€” user-customizable `date` field ignored

### M12. No onboarding reset mechanism
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:366`
- **Issue:** Once `onboarding_completed` is set, no way to restart onboarding or reset app data

### M13. Theme toggle not persisted across restarts
- **File:** `app/src/main/java/com/example/MainActivity.kt:45`
- **Issue:** `remember { mutableStateOf(true) }` resets on Activity recreation

### M14. DB init + WorkManager setup on main thread â†’ ANR risk
- **File:** `app/src/main/java/com/example/MainActivity.kt:29-41`
- **Issue:** Room `databaseBuilder.build()` runs synchronously on main thread

### M15. XLSX column letter overflow for >26 columns
- **File:** `app/src/main/java/com/example/utils/ExportEngine.kt:70-71`
- **Issue:** `('A' + colIdx)` produces `'['` for column 26 â†’ invalid XML

### M16. `processOverdueSubscriptions` expense insert + wallet deduction not atomic
- **File:** `app/src/main/java/com/example/data/ExpenseRepository.kt:134-137`
- **Issue:** Crash between insert and deduction â†’ wallet/expense mismatch

### M17. Member removal doesn't clean up subscriptions
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:238-272`
- **Issue:** Removed user's subscriptions in the space remain as orphaned records

### M18. Hardcoded `walletId = 1L` default on Expense and Subscription
- **File:** `app/src/main/java/com/example/data/Entities.kt:37,82`
- **Issue:** Assumes wallet ID 1 always exists. If deleted, new records reference non-existent wallet

### M19. Budget deletion by category name may over-delete
- **File:** `app/src/main/java/com/example/data/ExpenseRepository.kt:85-87`
- **Issue:** Multiple categories with same name (allowed) â†’ `deleteBudgetsByCategoryName` deletes budgets for ALL of them

### M20. Wallet update via `insertWallet` (REPLACE) is fragile
- **File:** `app/src/main/java/com/example/ui/WalletManagerScreen.kt:208-213`
- **Issue:** No dedicated `@Update` query; uses `@Insert(onConflict = REPLACE)`. Fields not in `copy()` revert to defaults

### M21. `CategoryManagerScreen` icon name case sensitivity
- **File:** `app/src/main/java/com/example/ui/CategoryManagerScreen.kt:357`
- **Issue:** Icon lookup uses `lowercase()` but icon is stored as-entered; mismatches with uppercase names

### M22. QR code may exceed capacity for large spaces
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:583-623`
- **Issue:** Large spaces with many expenses/splits produce JSON that exceeds QR code capacity (~3KB)

### M23. TITLE filter is useless â€” groups everything under one header
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:1825-1826`
- **Issue:** `groupBy { "Expenses (A-Z)" }` puts all expenses in a single group

### M24. Receipt item names not trimmed
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:3707`
- **Issue:** Leading/trailing whitespace in item names stored as-is

### M25. Calculator "=" button replaces expression with result
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:302-304`
- **Issue:** Cannot continue building on previous expression

### M26. No validation for unrealistic tax slab rates in GST calculator
- **File:** `app/src/main/java/com/example/ui/ToolsScreen.kt:665-678`
- **Issue:** 200% tax rate accepted silently

---

## LOW

### L1. `RecurringExpenseWorker` creates uncancelled `CoroutineScope`
- **File:** `app/src/main/java/com/example/worker/RecurringExpenseWorker.kt:20`
- **Issue:** Scope leaked on every worker invocation

### L2. `WorkManager KEEP` policy prevents interval updates
- **File:** `app/src/main/java/com/example/MainActivity.kt:37-41`
- **Issue:** App updates with different interval are ignored

### L3. `|||` delimiter in Converters fragile for URIs containing `|||`
- **File:** `app/src/main/java/com/example/data/Converters.kt:7-16`
- **Issue:** Ambiguous split if URI contains triple pipe

### L4. `SpaceMember` composite PK has no single ID column
- **File:** `app/src/main/java/com/example/data/Entities.kt:22-26`
- **Issue:** Cannot use generic `@Delete` / `@Update` on SpaceMember

### L5. `Long.formatAmount` loses precision for values > 2^53
- **File:** `app/src/main/java/com/example/ui/ExpenseViewModel.kt:18-26`
- **Issue:** `Long` â†’ `Double` cast

### L6. Inconsistent year parsing: EMI uses `Int`, Interest uses `Double`
- **File:** `app/src/main/java/com/example/ui/ToolsScreen.kt:141 vs 367`
- **Issue:** Fractional tenure works in Interest tab but fails in EMI tab

### L7. Expense row uses `?.firstOrNull()` on non-null `attachmentUris`
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:1668`
- **Issue:** Misleading nullable access on non-null type

### L8. No `id > 0L` guard in `updateExpenseWithSplits`
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:200-206`
- **Issue:** If called with `id = 0`, creates duplicate row

### L9. Magic number `5` for max attachments not extracted to constant
- **File:** `app/src/main/java/com/example/ui/ExpenseSplitterApp.kt:2644`
- **Issue:** Fragile if limit changes

### L10. Category sorting relies on insertion order, not explicit `sortOrder`
- **File:** `app/src/main/java/com/example/data/ExpenseDao.kt:114-115`
- **Issue:** `getAllCategories()` returns unsorted; UI relies on DB insertion order

### L11. `ToolsScreen` compound interest formula â€” OK but spreads across code
- **File:** `app/src/main/java/com/example/ui/ToolsScreen.kt:377`
- **Issue:** Formula is actually correct but could use clearer variable naming

---

## Root Cause of "Export Not Working"

| # | Root Cause | File | Fix |
|---|-----------|------|-----|
| H1 | FileProvider `file_paths.xml` only exposes `cache/shared/` but files are written to `cache/` root | `res/xml/file_paths.xml:3` + `ExportEngine.kt:86-101` | Change path to `.` or write files under `shared/` subdirectory |
| H12 | XLSX stores amounts as text (comma-formatted strings) | `ExportEngine.kt:73-78` | Write raw numeric values for XLSX cells |
