package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupEngine {

    private const val MANIFEST_FILE_NAME = "backup_manifest.json"
    private const val BACKUP_VERSION = 1
    private const val APP_NAME = "TravelSplit"

    suspend fun createFullBackupZip(
        context: Context,
        repository: ExpenseRepository,
        outputUri: Uri
    ): Result<Int> {
        return try {
            val users = repository.getAllUsersSync()
            val spaces = repository.getAllSpacesSync()
            val spaceMembers = repository.getAllSpaceMembersSync()
            val wallets = repository.getAllWalletsSync()
            val categories = repository.getAllCategoriesSync()
            val expenses = repository.getAllExpensesSync()
            val expenseSplits = repository.getAllExpenseSplitsSync()
            val budgets = repository.getAllBudgetsSync()
            val subscriptions = repository.getAllSubscriptionsSync()

            val rootJson = JSONObject()
            rootJson.put("version", BACKUP_VERSION)
            rootJson.put("appName", APP_NAME)
            rootJson.put("exportTimestamp", System.currentTimeMillis())

            val dataJson = JSONObject()

            // 1. Users
            val usersArr = JSONArray()
            users.forEach { u ->
                val obj = JSONObject()
                obj.put("id", u.id)
                obj.put("name", u.name)
                obj.put("email", u.email)
                obj.put("avatarUrl", u.avatarUrl)
                usersArr.put(obj)
            }
            dataJson.put("users", usersArr)

            // 2. Spaces
            val spacesArr = JSONArray()
            spaces.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("description", s.description)
                obj.put("createdAt", s.createdAt)
                spacesArr.put(obj)
            }
            dataJson.put("spaces", spacesArr)

            // 3. Space Members
            val smArr = JSONArray()
            spaceMembers.forEach { sm ->
                val obj = JSONObject()
                obj.put("id", sm.id)
                obj.put("spaceId", sm.spaceId)
                obj.put("userId", sm.userId)
                smArr.put(obj)
            }
            dataJson.put("space_members", smArr)

            // 4. Wallets
            val walletsArr = JSONArray()
            wallets.forEach { w ->
                val obj = JSONObject()
                obj.put("id", w.id)
                obj.put("name", w.name)
                obj.put("type", w.type)
                obj.put("balance", w.balance)
                walletsArr.put(obj)
            }
            dataJson.put("wallets", walletsArr)

            // 5. Categories
            val catsArr = JSONArray()
            categories.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("iconName", c.iconName)
                if (c.parentId != null) obj.put("parentId", c.parentId) else obj.put("parentId", JSONObject.NULL)
                catsArr.put(obj)
            }
            dataJson.put("categories", catsArr)

            // 6. Expense Splits
            val splitsArr = JSONArray()
            expenseSplits.forEach { es ->
                val obj = JSONObject()
                obj.put("id", es.id)
                obj.put("expenseId", es.expenseId)
                obj.put("userId", es.userId)
                obj.put("amountOwed", es.amountOwed)
                splitsArr.put(obj)
            }
            dataJson.put("expense_splits", splitsArr)

            // 7. Budgets
            val budgetsArr = JSONArray()
            budgets.forEach { b ->
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("isGlobal", b.isGlobal)
                if (b.categoryName != null) obj.put("categoryName", b.categoryName) else obj.put("categoryName", JSONObject.NULL)
                obj.put("limitAmount", b.limitAmount)
                obj.put("monthYear", b.monthYear)
                budgetsArr.put(obj)
            }
            dataJson.put("budgets", budgetsArr)

            // 8. Subscriptions
            val subsArr = JSONArray()
            subscriptions.forEach { sub ->
                val obj = JSONObject()
                obj.put("id", sub.id)
                obj.put("spaceId", sub.spaceId)
                obj.put("paidById", sub.paidById)
                obj.put("name", sub.name)
                obj.put("amount", sub.amount)
                obj.put("category", sub.category)
                if (sub.walletId != null) obj.put("walletId", sub.walletId) else obj.put("walletId", JSONObject.NULL)
                obj.put("intervalType", sub.intervalType)
                obj.put("nextDueDate", sub.nextDueDate)
                obj.put("isActive", sub.isActive)
                subsArr.put(obj)
            }
            dataJson.put("subscriptions", subsArr)

            // Open output stream to target SAF Uri
            val outputStream = context.contentResolver.openOutputStream(outputUri)
                ?: return Result.failure(Exception("Could not open destination storage for writing."))

            var totalAttachmentsCount = 0

            ZipOutputStream(outputStream).use { zos ->
                // 9. Expenses and Attachments
                val expensesArr = JSONArray()
                val addedPhotoEntryNames = mutableSetOf<String>()

                expenses.forEach { exp ->
                    val obj = JSONObject()
                    obj.put("id", exp.id)
                    obj.put("spaceId", exp.spaceId)
                    obj.put("paidById", exp.paidById)
                    obj.put("description", exp.description)
                    obj.put("amount", exp.amount)
                    obj.put("date", exp.date)
                    obj.put("category", exp.category)
                    if (exp.walletId != null) obj.put("walletId", exp.walletId) else obj.put("walletId", JSONObject.NULL)

                    val zipAttachmentPaths = JSONArray()

                    exp.attachmentUris.forEachIndexed { idx, uriStr ->
                        if (uriStr.isNotBlank()) {
                            try {
                                val uri = Uri.parse(uriStr)
                                val extension = getExtensionFromUri(context, uri, uriStr)
                                val safeDesc = exp.description.replace("[^a-zA-Z0-9]".toRegex(), "_").take(15)
                                var entryName = "attachments/bill_${exp.id}_${safeDesc}_$idx.$extension"
                                var counter = 1
                                while (addedPhotoEntryNames.contains(entryName)) {
                                    entryName = "attachments/bill_${exp.id}_${safeDesc}_${idx}_$counter.$extension"
                                    counter++
                                }
                                addedPhotoEntryNames.add(entryName)

                                val inputStream: InputStream? = try {
                                    context.contentResolver.openInputStream(uri)
                                } catch (e: Exception) {
                                    val localPath = if (uriStr.startsWith("file://")) uriStr.substring(7) else uriStr
                                    val f = File(localPath)
                                    if (f.exists()) FileInputStream(f) else null
                                }

                                if (inputStream != null) {
                                    zos.putNextEntry(ZipEntry(entryName))
                                    inputStream.use { it.copyTo(zos) }
                                    zos.closeEntry()
                                    zipAttachmentPaths.put(entryName)
                                    totalAttachmentsCount++
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    obj.put("attachmentUris", zipAttachmentPaths)
                    expensesArr.put(obj)
                }
                dataJson.put("expenses", expensesArr)

                rootJson.put("data", dataJson)

                // Write manifest JSON entry
                zos.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
                zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val totalItems = users.size + spaces.size + expenses.size + totalAttachmentsCount
            Result.success(totalItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreFullBackupZip(
        context: Context,
        repository: ExpenseRepository,
        inputUri: Uri
    ): Result<Int> {
        return try {
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return Result.failure(Exception("Could not open selected backup file."))

            var manifestString: String? = null
            val attachmentsDir = File(context.filesDir, "attachments").also { if (!it.exists()) it.mkdirs() }

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory) {
                        if (name == MANIFEST_FILE_NAME) {
                            manifestString = zis.bufferedReader(Charsets.UTF_8).readText()
                        } else if (name.startsWith("attachments/")) {
                            val cleanFileName = name.removePrefix("attachments/").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                            if (cleanFileName.isNotBlank()) {
                                val targetFile = File(attachmentsDir, cleanFileName)
                                FileOutputStream(targetFile).use { fos ->
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (manifestString.isNullOrBlank()) {
                return Result.failure(Exception("Invalid backup ZIP: Missing $MANIFEST_FILE_NAME"))
            }

            val rootJson = JSONObject(manifestString!!)
            if (!rootJson.has("data")) {
                return Result.failure(Exception("Invalid backup file structure: Missing 'data' object."))
            }

            val dataJson = rootJson.getJSONObject("data")

            // 1. Users
            val usersList = mutableListOf<User>()
            val usersArr = dataJson.optJSONArray("users") ?: JSONArray()
            for (i in 0 until usersArr.length()) {
                val obj = usersArr.getJSONObject(i)
                usersList.add(
                    User(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        email = obj.optString("email", ""),
                        avatarUrl = obj.optString("avatarUrl", "")
                    )
                )
            }

            // 2. Spaces
            val spacesList = mutableListOf<Space>()
            val spacesArr = dataJson.optJSONArray("spaces") ?: JSONArray()
            for (i in 0 until spacesArr.length()) {
                val obj = spacesArr.getJSONObject(i)
                spacesList.add(
                    Space(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // 3. Space Members
            val smList = mutableListOf<SpaceMember>()
            val smArr = dataJson.optJSONArray("space_members") ?: JSONArray()
            for (i in 0 until smArr.length()) {
                val obj = smArr.getJSONObject(i)
                smList.add(
                    SpaceMember(
                        id = obj.getLong("id"),
                        spaceId = obj.getLong("spaceId"),
                        userId = obj.getLong("userId")
                    )
                )
            }

            // 4. Wallets
            val walletsList = mutableListOf<Wallet>()
            val walletsArr = dataJson.optJSONArray("wallets") ?: JSONArray()
            for (i in 0 until walletsArr.length()) {
                val obj = walletsArr.getJSONObject(i)
                walletsList.add(
                    Wallet(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        balance = obj.optDouble("balance", 0.0)
                    )
                )
            }

            // 5. Categories
            val categoriesList = mutableListOf<Category>()
            val catsArr = dataJson.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catsArr.length()) {
                val obj = catsArr.getJSONObject(i)
                val parentId = if (obj.isNull("parentId")) null else obj.getLong("parentId")
                categoriesList.add(
                    Category(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        iconName = obj.optString("iconName", "category"),
                        parentId = parentId
                    )
                )
            }

            // 6. Expense Splits
            val splitsList = mutableListOf<ExpenseSplit>()
            val splitsArr = dataJson.optJSONArray("expense_splits") ?: JSONArray()
            for (i in 0 until splitsArr.length()) {
                val obj = splitsArr.getJSONObject(i)
                splitsList.add(
                    ExpenseSplit(
                        id = obj.getLong("id"),
                        expenseId = obj.getLong("expenseId"),
                        userId = obj.getLong("userId"),
                        amountOwed = obj.getDouble("amountOwed")
                    )
                )
            }

            // 7. Budgets
            val budgetsList = mutableListOf<Budget>()
            val budgetsArr = dataJson.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgetsArr.length()) {
                val obj = budgetsArr.getJSONObject(i)
                val catName = if (obj.isNull("categoryName")) null else obj.getString("categoryName")
                budgetsList.add(
                    Budget(
                        id = obj.getLong("id"),
                        isGlobal = obj.getBoolean("isGlobal"),
                        categoryName = catName,
                        limitAmount = obj.getDouble("limitAmount"),
                        monthYear = obj.getString("monthYear")
                    )
                )
            }

            // 8. Subscriptions
            val subsList = mutableListOf<Subscription>()
            val subsArr = dataJson.optJSONArray("subscriptions") ?: JSONArray()
            for (i in 0 until subsArr.length()) {
                val obj = subsArr.getJSONObject(i)
                val walletId = if (obj.isNull("walletId")) null else obj.getLong("walletId")
                subsList.add(
                    Subscription(
                        id = obj.getLong("id"),
                        spaceId = obj.getLong("spaceId"),
                        paidById = obj.getLong("paidById"),
                        name = obj.getString("name"),
                        amount = obj.getDouble("amount"),
                        category = obj.optString("category", "General"),
                        walletId = walletId,
                        intervalType = obj.optString("intervalType", "Monthly"),
                        nextDueDate = obj.optLong("nextDueDate", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }

            // 9. Expenses
            val expensesList = mutableListOf<Expense>()
            val expensesArr = dataJson.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArr.length()) {
                val obj = expensesArr.getJSONObject(i)
                val walletId = if (obj.isNull("walletId")) null else obj.getLong("walletId")
                val attachArr = obj.optJSONArray("attachmentUris") ?: JSONArray()
                val attachUris = mutableListOf<String>()

                for (j in 0 until attachArr.length()) {
                    val rawPath = attachArr.getString(j)
                    if (rawPath.startsWith("attachments/")) {
                        val cleanFileName = rawPath.removePrefix("attachments/").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                        val targetFile = File(attachmentsDir, cleanFileName)
                        if (targetFile.exists()) {
                            attachUris.add(Uri.fromFile(targetFile).toString())
                        }
                    } else if (rawPath.isNotBlank()) {
                        attachUris.add(rawPath)
                    }
                }

                expensesList.add(
                    Expense(
                        id = obj.getLong("id"),
                        spaceId = obj.getLong("spaceId"),
                        paidById = obj.getLong("paidById"),
                        description = obj.getString("description"),
                        amount = obj.getDouble("amount"),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        category = obj.optString("category", "General"),
                        walletId = walletId,
                        attachmentUris = attachUris
                    )
                )
            }

            // Perform transactional full restoration in Room DB
            repository.restoreFullDatabase(
                users = usersList,
                spaces = spacesList,
                spaceMembers = smList,
                wallets = walletsList,
                categories = categoriesList,
                expenses = expensesList,
                expenseSplits = splitsList,
                budgets = budgetsList,
                subscriptions = subsList
            )

            val restoredCount = usersList.size + spacesList.size + expensesList.size
            Result.success(restoredCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun getExtensionFromUri(context: Context, uri: Uri, fallbackPath: String): String {
        return try {
            val type = context.contentResolver.getType(uri)
            when (type) {
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/jpeg", "image/jpg" -> "jpg"
                "application/pdf" -> "pdf"
                else -> {
                    val ext = File(fallbackPath).extension
                    if (ext.isNotBlank()) ext else "jpg"
                }
            }
        } catch (e: Exception) {
            "jpg"
        }
    }
}
