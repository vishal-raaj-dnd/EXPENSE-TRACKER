# Splitify (VISH-tracker) — Comprehensive Architecture & Codebase Guide

Welcome to the technical explanation document for **Splitify** (also referred to as VISH-tracker or Travel Split). This guide outlines the application's goals, technological stack, package structure, database schema, algorithms, UI navigation flow, and code health.

---

## 1. Overview & Objective

**Splitify** is a production-ready, feature-rich Android mobile application designed to simplify personal budgeting and group financial splitting. Its primary use cases include:
*   **Collaborative Expense Splitting (Spaces):** Allowing groups of users (e.g., flatmates, travel companions, families) to pool expenses.
*   **Debt Resolution & Settlement Engine:** Automatically computing net balances and recommending a minimal set of transactions to clear debts.
*   **Asset Management (Wallets):** Keeping track of available balances across diverse sources like Cash, Bank, Credit Cards, and UPI.
*   **Financial Planning:** Restricting budgets per category and automating recurring subscriptions (e.g., streaming services, rent).
*   **Utility Financial Calculators:** Providing built-in, offline tools for EMI, compound/simple interest, currency denominations (Cash Counter), and GST calculation.
*   **Data Exportability:** Generating formatted Excel ledger sheets and PDF invoices/reports directly on the device.

---

## 2. Tech Stack & Design System

The application is built on top of modern native Android paradigms:

| Layer | Component | Detail / Purpose |
| :--- | :--- | :--- |
| **Language** | **Kotlin** | Utilizing coroutines, Flows, data classes, and type-safe builders. |
| **UI Framework** | **Jetpack Compose** | Declarative rendering engine. UI updates dynamically based on State/Flow collections. |
| **Design System** | **Material Design 3** | Color themes (light & dark mode support), typography scales, and adaptive layouts. |
| **Local Storage** | **Room (SQLite)** | Object Relational Mapping (ORM) to handle local SQLite transactions, indexes, and queries. |
| **Background Processing** | **WorkManager** | Orchestrates automated background syncs for processing subscription renewals. |
| **Image Loading** | **Coil** | Renders receipt attachments asynchronously. |
| **QR Code Engine** | **ZXing** | Generates and parses QR codes containing space payloads (members, expenses, and splits) for offline transfer. |
| **Document Generators** | **Apache POI & PdfDocument** | Assembled in background threads to export `.xlsx` spreadsheets and print native PDF sheets. |

---

## 3. Project Directory Architecture

The repository follows a clean, single-module architecture (MVVM pattern), splitting code into data, UI, utilities, and background worker packages:

```
VISH-tracker/
├── app/
│   └── src/main/
│       ├── java/com/example/
│       │   ├── data/
│       │   │   ├── AppDatabase.kt          <-- Database initialization (Singleton)
│       │   │   ├── Entities.kt             <-- Data models (Table schemas and relationships)
│       │   │   ├── ExpenseDao.kt           <-- SQL queries, insertions, updates, and custom transactions
│       │   │   └── ExpenseRepository.kt    <-- Repository layer exposing Flows and suspend actions
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt            <-- App theme palettes (Light/Dark mode)
│       │   │   │   ├── Theme.kt            <-- Theme configuration applying Material3 shapes and colors
│       │   │   │   └── Type.kt             <-- Typography configurations (Fonts)
│       │   │   ├── BudgetManagerScreen.kt  <-- UI views for category and monthly budgets
│       │   │   ├── CategoryManagerScreen.kt<-- UI views for editing/nesting categories
│       │   │   ├── ExpenseSplitterApp.kt   <-- Main visual scaffold, Navigation drawer, detail dialogs
│       │   │   ├── ExpenseViewModel.kt     <-- ViewModel handling business state, debt calculations
│       │   │   ├── FinancialPlanningScreens.kt <-- UI for monthly subscriptions
│       │   │   └── ToolsScreen.kt          <-- Calculators (EMI, Cash Counter, GST, Interest)
│       │   ├── utils/
│       │   │   └── ExportEngine.kt         <-- PDF print layouts and Apache POI Excel construction
│       │   ├── worker/
│       │   │   └── RecurringExpenseWorker.kt <-- Daily/periodic task execution trigger
│       │   └── MainActivity.kt             <-- Entry Activity, CompositionLocal density provider, theme hook
│       └── AndroidManifest.xml             <-- Manifest (Permissions, Activity setup, FileProvider declaration)
```

---

## 4. Room Database Schema & Entities

The application stores data locally inside a SQLite database managed via Room. Below is the mapping of data entities defined in [Entities.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/data/Entities.kt):

### A. Core User and Space Entities
*   **`User`** (`users`): Represents individuals.
    *   `id` (Long, PK): Auto-generated.
    *   `name` (String): The display name.
    *   `email` (String): The identifier used to map splits and QR code sync.
    *   `avatarUrl` (String): Resource path or color hex code.
*   **`Space`** (`spaces`): Represents a group folder (e.g., "Trip to Manali").
    *   `id` (Long, PK): Auto-generated.
    *   `name` (String): Group title.
    *   `description` (String): Descriptive notes.
    *   `createdAt` (Long): Timestamp.
*   **`SpaceMember`** (`space_members`): Junction table establishing a many-to-many relationship between users and spaces.
    *   Contains Foreign Keys referencing `Space` and `User` with `ON DELETE CASCADE`.
    *   Unique composite index on `(spaceId, userId)`.

### B. Expense and Ledger Entities
*   **`Expense`** (`expenses`): A ledger entry of a payment made within a Space.
    *   `id` (Long, PK): Auto-generated.
    *   `spaceId` (Long): Links to parent Space (Cascade delete).
    *   `paidById` (Long): Links to paying `User` (Cascade delete).
    *   `description` (String): Expense title.
    *   `amount` (Double): Total expense cost.
    *   `date` (Long): Transaction time.
    *   `category` (String): Category label.
    *   `walletId` (Long?): Links to the source `Wallet` (Nullified if wallet is deleted).
    *   `attachmentUris` (List<String>): List of local image URIs (serialized via Room TypeConverters).
*   **`ExpenseSplit`** (`expense_splits`): Records the breakdown of who owes what for a particular expense.
    *   `id` (Long, PK): Auto-generated.
    *   `expenseId` (Long): FK pointing to parent `Expense` (Cascade delete).
    *   `userId` (Long): FK pointing to debtor `User` (Cascade delete).
    *   `amountOwed` (Double): Owed fraction.

### C. Budgeting, Subscriptions, and Asset Entities
*   **`Wallet`** (`wallets`): Cash flow/payment sources.
    *   `id` (Long, PK): Auto-generated.
    *   `name` (String): Card, UPI, cash bag name.
    *   `type` (String): Card, Cash, Bank, etc.
    *   `balance` (Double): Current funds.
*   **`Category`** (`categories`): Supports hierarchical sub-categories.
    *   `id` (Long, PK): Auto-generated.
    *   `name` (String): Unique category tag.
    *   `iconName` (String): Icon resource key.
    *   `parentId` (Long?): Self-referential FK targeting the parent Category.
*   **`Budget`** (`budgets`): Target constraints.
    *   `isGlobal` (Boolean): Defines if it monitors global spending or category-only.
    *   `categoryName` (String?): Scoped category target.
    *   `limitAmount` (Double): Limit threshold.
    *   `monthYear` (String): Format `"MM/yyyy"`.
*   **`Subscription`** (`subscriptions`): Recurring periodic transactions.
    *   `spaceId` (Long), `paidById` (Long), `walletId` (Long?): Associated split targets.
    *   `amount` (Double), `category` (String): Transaction payload.
    *   `intervalType` (String): Renewal interval (`"Daily"`, `"Weekly"`, `"Monthly"`).
    *   `nextDueDate` (Long): Millisecond schedule target.
    *   `isActive` (Boolean): Switch toggled to pause background runs.

---

## 5. Architectural Breakdown & Code Deep Dive

### A. Startup & Initialization ([MainActivity.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/MainActivity.kt))
*   Retrieves theme preferences (`is_dark`) and user-selected custom font scaling from `SharedPreferences`.
*   Asynchronously initializes `AppDatabase` inside a `LaunchedEffect`.
*   Uses Android **WorkManager** to schedule a unique periodic background worker (`RecurringExpenseWorker`) configured to run every 15 minutes to process any outstanding subscription charges.
*   Uses Compose `CompositionLocalProvider` to adjust font density dynamically across screens based on app settings.

### B. Business State & Calculation Logic ([ExpenseViewModel.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/ui/ExpenseViewModel.kt))
The ViewModel acts as the core logical processing hub of the app, connecting Compose views to the underlying Repository.

#### The Debt Resolution Algorithm (`resolveDebts`)
To resolve who owes whom in a Space, `resolveDebts` aggregates all expenses and splits:
1.  Computes the net balance for each member: $\text{Balance} = \text{Amount Paid} - \text{Amount Owed}$.
2.  Filters members into **creditors** (balance $> 0.01$) and **debtors** (balance $< -0.01$).
3.  Implements a greedy algorithm to pair debtors with creditors:
    *   Takes the first debtor and first creditor.
    *   Determines the transaction amount: $\text{settledAmount} = \min(\text{creditAmount}, \text{debtAmount})$.
    *   Updates the remaining amounts in both arrays.
    *   If a member's net balance falls below $\le 0.01$ (rounded), it increments the index pointer.
    *   Appends the simplified transfer object to a transaction list.

```kotlin
private fun resolveDebts(balances: List<MemberBalance>): List<SettlementTransaction> {
    val transactions = mutableListOf<SettlementTransaction>()
    val creditors = balances.filter { it.netBalance > 0.01 }
        .map { it.user to it.netBalance }.toMutableList()
    val debtors = balances.filter { it.netBalance < -0.01 }
        .map { it.user to -it.netBalance }.toMutableList()

    var cIndex = 0
    var dIndex = 0

    while (cIndex < creditors.size && dIndex < debtors.size) {
        val (creditor, creditAmount) = creditors[cIndex]
        val (debtor, debtAmount) = debtors[dIndex]

        val settledAmount = minOf(creditAmount, debtAmount)
        if (settledAmount > 0.01) {
            transactions.add(SettlementTransaction(debtor, creditor, settledAmount))
        }

        creditors[cIndex] = creditor to (creditAmount - settledAmount)
        debtors[dIndex] = debtor to (debtAmount - settledAmount)

        if (creditors[cIndex].second <= 0.01) { cIndex++ }
        if (debtors[dIndex].second <= 0.01) { dIndex++ }
    }
    return transactions
}
```

### C. Subscription Automation ([RecurringExpenseWorker.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/worker/RecurringExpenseWorker.kt) & [ExpenseRepository.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/data/ExpenseRepository.kt))
*   **Orchestration:** Android's WorkManager triggers `RecurringExpenseWorker` periodically.
*   **Check:** The worker fetches all active subscriptions where `nextDueDate` is in the past.
*   **Calculation:**
    *   Determines the member count in the target Space.
    *   Calculates equal splits per person, accounting for decimal division remainder logic.
    *   Constructs a new `Expense` entry with the prefix `"Recurring: <Subscription Name>"`.
*   **Deduction:** If a `walletId` is attached, the transaction updates the wallet balance and records the new expense.
*   **Reschedule:** Advances the subscription's `nextDueDate` by 1 day, 1 week, or 1 month using `java.util.Calendar`.

### D. Layout Engine & Navigation ([ExpenseSplitterApp.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/ui/ExpenseSplitterApp.kt))
*   Contains the application's central scaffold with a Navigation Drawer for switching views (Dashboard, Wallets, Calculators, Budget manager).
*   Integrates modern Material Design 3 cards, bottom sheets, full-width FAB controllers, dynamic search dialogs, and detail models for transactions.
*   Integrates interactive Compose charts:
    *   *Line Chart:* Displays historical daily spend progression.
    *   *Donut Pie Chart:* Displays proportional categories breakdown.

### E. File Generators ([ExportEngine.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/utils/ExportEngine.kt))
*   **Excel Export (`.xlsx`):** Uses the Apache POI library to structure sheet files, columns, cell fonts, background header fills, currency borders, and formulas.
*   **PDF Export:** Utilizes Android's native graphics Canvas API on `PdfDocument` to paint standard document page layouts, invoices, and summaries.
*   **Sharing:** Utilizes Android's `FileProvider` architecture. The file is temporarily written to the application's cache directory and shared via an `Intent.ACTION_SEND` chooser, allowing files to be sent to external platforms (e.g. WhatsApp, Email) securely.

---

## 6. Notable Architecture Issues & Quality Checklist

As detailed in [bugs.md](file:///c:/Users/bdurk/Downloads/VISH-tracker/bugs.md) and [solutions.md](file:///c:/Users/bdurk/Downloads/VISH-tracker/solutions.md), developers should remain aware of several legacy implementation issues:

### A. Data Cascades & Orphaned Records
*   **Problem:** User and category deletions originally orphaned dependent data because the schema lacked proper foreign key annotations.
*   **Fix:** Updated database schemas are being structured in [Entities.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/data/Entities.kt) to enforce foreign key restrictions, ensuring child elements (like splits or sub-categories) are deleted cascadingly or re-assigned correctly.

### B. Currency & Decimal Rounding Errors
*   **Problem:** Splitting currency (e.g., ₹100 split among 3 members) can lead to rounding discrepancies (₹33.33 × 3 = ₹99.99). The missing ₹0.01 needs to be allocated to the payer or final split member.
*   **Fix:** Current transactional functions utilize rounding remainders, allocating the fractional differences to the last member's split balance so that $\sum \text{Splits} = \text{Total Amount}$ exactly.

### C. FileProvider Path Mismatches
*   **Problem:** Mismatches in `file_paths.xml` caches and file paths during exporting would throw `FileUriExposedException` on modern API levels.
*   **Fix:** Ensure internal cache directories target paths matching the declared FileProvider XML metadata.

---

## 7. How to Build & Run locally

### Building Debug APK:
```bash
./gradlew assembleDebug
```
*   **Output location:** `app/build/outputs/apk/debug/app-debug.apk`

### Testing Room Queries:
You can verify the queries in `ExpenseDaoTest.kt` or examine the schema version increments inside [AppDatabase.kt](file:///c:/Users/bdurk/Downloads/VISH-tracker/app/src/main/java/com/example/data/AppDatabase.kt).
