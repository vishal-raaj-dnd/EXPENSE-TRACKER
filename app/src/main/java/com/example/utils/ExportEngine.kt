package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.example.data.Expense
import com.example.data.User
import com.example.data.Wallet
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportEngine {

    // ========================================================================
    // LIGHTWEIGHT XLSX WRITER (No Apache POI)
    // Generates valid .xlsx files using ZipOutputStream + OpenXML inline strings
    // ========================================================================

    private fun escapeXml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")

    private fun buildContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun buildRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildWorkbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Ledger" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun buildWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

    private fun buildSheetXml(rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")
        rows.forEachIndexed { rowIdx, cells ->
            sb.append("""<row r="${rowIdx + 1}">""")
            cells.forEachIndexed { colIdx, cellValue ->
                val colLetter = ('A' + colIdx)
                val cellRef = "$colLetter${rowIdx + 1}"
                // Try to write numbers as numbers, otherwise inline string
                val numVal = cellValue.toDoubleOrNull()
                if (numVal != null) {
                    sb.append("""<c r="$cellRef"><v>$numVal</v></c>""")
                } else {
                    sb.append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(cellValue)}</t></is></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun writeXlsxFile(context: Context, filename: String, rows: List<List<String>>): File {
        val file = File(context.cacheDir, filename)
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun addEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            addEntry("[Content_Types].xml", buildContentTypesXml())
            addEntry("_rels/.rels", buildRelsXml())
            addEntry("xl/workbook.xml", buildWorkbookXml())
            addEntry("xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
            addEntry("xl/worksheets/sheet1.xml", buildSheetXml(rows))
        }
        return file
    }

    /**
     * Export Space transactions to XLSX format using lightweight ZIP-based writer
     */
    fun exportSpaceToExcel(
        context: Context,
        spaceName: String,
        expenses: List<Expense>,
        members: List<User>,
        wallets: List<Wallet>
    ): File {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val rows = mutableListOf<List<String>>()

        // Title row
        rows.add(listOf("TRANSACTION LEDGER OF SPACE: ${spaceName.uppercase()}"))
        rows.add(emptyList()) // blank row

        // Header row
        rows.add(listOf("Date", "Category", "Description", "Paid By", "Amount (₹)", "Funding Wallet"))

        var totalAmount = 0.0
        expenses.forEach { exp ->
            val payerName = members.find { it.id == exp.paidById }?.name ?: "Unknown"
            val walletName = wallets.find { it.id == exp.walletId }?.name ?: "Wallet ${exp.walletId}"
            rows.add(listOf(
                sdf.format(Date(exp.date)),
                exp.category,
                exp.description,
                payerName,
                exp.amount.toString(),
                walletName
            ))
            totalAmount += exp.amount
        }

        rows.add(emptyList()) // blank row
        rows.add(listOf("", "", "", "TOTAL SPENT:", totalAmount.toString()))

        return writeXlsxFile(
            context,
            "ledger_${spaceName.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.xlsx",
            rows
        )
    }

    /**
     * Export Wallet ledger as formatted XLSX
     */
    fun exportWalletToExcel(
        context: Context,
        wallet: Wallet,
        expenses: List<Expense>,
        members: List<User>
    ): File {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val walletExpenses = expenses.filter { it.walletId == wallet.id }
        val rows = mutableListOf<List<String>>()

        rows.add(listOf("LEDGER OF WALLET: ${wallet.name.uppercase()} (${wallet.type})"))
        rows.add(emptyList())
        rows.add(listOf("Date", "Category", "Description", "Paid By", "Amount (₹)"))

        var totalAmount = 0.0
        walletExpenses.forEach { exp ->
            val payerName = members.find { it.id == exp.paidById }?.name ?: "Unknown"
            rows.add(listOf(
                sdf.format(Date(exp.date)),
                exp.category,
                exp.description,
                payerName,
                exp.amount.toString()
            ))
            totalAmount += exp.amount
        }

        rows.add(emptyList())
        rows.add(listOf("", "", "", "TOTAL FUNDED:", totalAmount.toString()))

        return writeXlsxFile(
            context,
            "ledger_wallet_${wallet.name.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.xlsx",
            rows
        )
    }

    /**
     * CSV Exporter for Space Ledgers
     */
    fun exportSpaceToCSV(
        context: Context,
        spaceName: String,
        expenses: List<Expense>,
        members: List<User>,
        wallets: List<Wallet>
    ): File {
        val sb = StringBuilder()
        sb.append("Date,Category,Description,Paid By,Amount,Funding Wallet\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        expenses.forEach { exp ->
            val dateStr = sdf.format(Date(exp.date))
            val categoryStr = exp.category.replace("\"", "\"\"")
            val descStr = exp.description.replace("\"", "\"\"")
            val payerName = (members.find { it.id == exp.paidById }?.name ?: "Unknown").replace("\"", "\"\"")
            val walletName = (wallets.find { it.id == exp.walletId }?.name ?: "Wallet ${exp.walletId}").replace("\"", "\"\"")

            sb.append("\"$dateStr\",\"$categoryStr\",\"$descStr\",\"$payerName\",${exp.amount},\"$walletName\"\n")
        }

        val file = File(context.cacheDir, "ledger_${spaceName.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())
        return file
    }

    /**
     * CSV Exporter for Wallet Ledgers
     */
    fun exportWalletToCSV(
        context: Context,
        wallet: Wallet,
        expenses: List<Expense>,
        members: List<User>
    ): File {
        val sb = StringBuilder()
        sb.append("Date,Category,Description,Paid By,Amount\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val walletExpenses = expenses.filter { it.walletId == wallet.id }
        walletExpenses.forEach { exp ->
            val dateStr = sdf.format(Date(exp.date))
            val categoryStr = exp.category.replace("\"", "\"\"")
            val descStr = exp.description.replace("\"", "\"\"")
            val payerName = (members.find { it.id == exp.paidById }?.name ?: "Unknown").replace("\"", "\"\"")

            sb.append("\"$dateStr\",\"$categoryStr\",\"$descStr\",\"$payerName\",${exp.amount}\n")
        }

        val file = File(context.cacheDir, "ledger_wallet_${wallet.name.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())
        return file
    }

    /**
     * Beautiful Native PDF Financial Report Builder with KPI metrics and horizontal bar charts
     */
    fun generateSpaceReportPDF(
        context: Context,
        spaceName: String,
        expenses: List<Expense>,
        members: List<User>,
        wallets: List<Wallet>
    ): File {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val headerPaint = Paint().apply {
            color = Color.WHITE
            isFakeBoldText = true
            textSize = 11f
        }

        val primaryColorPaint = Paint().apply {
            color = Color.parseColor("#1B2D4F")
        }

        val accentColorPaint = Paint().apply {
            color = Color.parseColor("#0C8F6E")
        }

        val lightGrayPaint = Paint().apply {
            color = Color.parseColor("#F3F4F6")
        }

        val chartPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
        }

        // Draw Header Banner
        canvas.drawRect(Rect(0, 0, 595, 80), primaryColorPaint)
        canvas.drawText("FINANCIAL AUDIT REPORT", 25f, 42f, Paint(headerPaint).apply { textSize = 18f })
        canvas.drawText("Generated locally by Travel Split", 25f, 62f, Paint(headerPaint).apply { textSize = 10f })

        var y = 110f

        // Document Metadata
        canvas.drawText("Group/Space Name: $spaceName", 25f, y, Paint(titlePaint).apply { textSize = 14f })
        y += 20f
        val genSdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.US)
        canvas.drawText("Export Date: ${genSdf.format(Date())}", 25f, y, subTitlePaint)
        y += 30f

        // Total Spent KPI block
        val totalSpent = expenses.sumOf { it.amount }
        canvas.drawRoundRect(25f, y, 570f, y + 60f, 15f, 15f, lightGrayPaint)
        canvas.drawText("TOTAL GROUP SPENDING", 40f, y + 25f, Paint(subTitlePaint).apply { isFakeBoldText = true; color = Color.GRAY; textSize = 9f })
        canvas.drawText("₹${String.format(Locale.US, "%.2f", totalSpent)}", 40f, y + 50f, Paint(titlePaint).apply { textSize = 20f; color = Color.parseColor("#1B2D4F") })

        y += 85f

        // Visual category proportional charts
        canvas.drawText("CATEGORY SPENDING BREAKDOWN", 25f, y, Paint(titlePaint).apply { textSize = 11f })
        y += 15f

        val categoriesMap = expenses.groupBy { it.category }.mapValues { it.value.sumOf { it.amount } }
        if (categoriesMap.isNotEmpty()) {
            categoriesMap.forEach { (cat, amt) ->
                val pct = if (totalSpent > 0.0) (amt / totalSpent).toFloat() else 0f
                canvas.drawText(cat, 25f, y + 10f, bodyPaint)

                // Outer progress container track
                canvas.drawRoundRect(180f, y + 2f, 450f, y + 10f, 4f, 4f, chartPaint)
                // Filled progress bar representing percentage
                val barWidth = 270f * pct
                val barEnd = 180f + if (barWidth < 4f) 4f else barWidth
                canvas.drawRoundRect(180f, y + 2f, barEnd, y + 10f, 4f, 4f, accentColorPaint)

                canvas.drawText("${String.format(Locale.US, "%.1f", pct * 100f)}% (₹${String.format(Locale.US, "%.2f", amt)})", 460f, y + 10f, Paint(bodyPaint).apply { isFakeBoldText = true })
                y += 18f
            }
        } else {
            canvas.drawText("No categorized transaction records available.", 25f, y + 10f, subTitlePaint)
            y += 18f
        }

        y += 20f

        // Table List
        canvas.drawText("ITEMIZED TRANSACTION HISTORY", 25f, y, Paint(titlePaint).apply { textSize = 11f })
        y += 15f

        val colX = floatArrayOf(25f, 120f, 220f, 340f, 460f)
        canvas.drawRect(25f, y, 570f, y + 22f, primaryColorPaint)
        canvas.drawText("Date", colX[0] + 5f, y + 15f, Paint(headerPaint).apply { textSize = 9f })
        canvas.drawText("Category", colX[1], y + 15f, headerPaint)
        canvas.drawText("Description", colX[2], y + 15f, headerPaint)
        canvas.drawText("Paid By", colX[3], y + 15f, headerPaint)
        canvas.drawText("Amount", colX[4], y + 15f, headerPaint)
        y += 22f

        val rowPaint = Paint().apply { color = Color.WHITE }
        val altRowPaint = Paint().apply { color = Color.parseColor("#F9FAFB") }
        val cellBorderPaint = Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        val listSdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        expenses.forEachIndexed { index, exp ->
            if (y > 780f) {
                // Perform complete elegant page-break sequence
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                canvas.drawRect(Rect(0, 0, 595, 40), primaryColorPaint)
                canvas.drawText("ITEMIZED TRANSACTION HISTORY (Continued)", 25f, 25f, Paint(headerPaint).apply { textSize = 12f })

                y = 55f
                canvas.drawRect(25f, y, 570f, y + 22f, primaryColorPaint)
                canvas.drawText("Date", colX[0] + 5f, y + 15f, Paint(headerPaint).apply { textSize = 9f })
                canvas.drawText("Category", colX[1], y + 15f, headerPaint)
                canvas.drawText("Description", colX[2], y + 15f, headerPaint)
                canvas.drawText("Paid By", colX[3], y + 15f, headerPaint)
                canvas.drawText("Amount", colX[4], y + 15f, headerPaint)
                y += 22f
            }

            canvas.drawRect(25f, y, 570f, y + 18f, if (index % 2 == 0) rowPaint else altRowPaint)
            canvas.drawLine(25f, y + 18f, 570f, y + 18f, cellBorderPaint)

            val dateText = listSdf.format(Date(exp.date))
            val categoryText = if (exp.category.length > 15) exp.category.take(13) + ".." else exp.category
            val descText = if (exp.description.length > 20) exp.description.take(18) + ".." else exp.description
            val payerName = members.find { it.id == exp.paidById }?.name ?: "Unknown"
            val amtText = "₹${String.format(Locale.US, "%.2f", exp.amount)}"

            canvas.drawText(dateText, colX[0] + 5f, y + 13f, bodyPaint)
            canvas.drawText(categoryText, colX[1], y + 13f, bodyPaint)
            canvas.drawText(descText, colX[2], y + 13f, bodyPaint)
            canvas.drawText(payerName, colX[3], y + 13f, bodyPaint)
            canvas.drawText(amtText, colX[4], y + 13f, Paint(bodyPaint).apply { isFakeBoldText = true })
            y += 18f
        }

        // Draw Footer
        if (y > 780f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
        }
        canvas.drawLine(25f, y + 10f, 570f, y + 10f, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        canvas.drawText("End of Audit Report. Total items parsed: ${expenses.size}", 25f, y + 25f, Paint(subTitlePaint).apply { textSize = 8f; color = Color.GRAY })

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "report_${spaceName.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use {
            pdfDocument.writeTo(it)
        }
        pdfDocument.close()
        return file
    }

    /**
     * Beautiful Native PDF financial report for funding Wallet
     */
    fun generateWalletReportPDF(
        context: Context,
        wallet: Wallet,
        expenses: List<Expense>,
        members: List<User>
    ): File {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val headerPaint = Paint().apply {
            color = Color.WHITE
            isFakeBoldText = true
            textSize = 11f
        }

        val primaryColorPaint = Paint().apply {
            color = Color.parseColor("#0C6B58") // Stylish Sea-Green theme for Wallets
        }

        val accentColorPaint = Paint().apply {
            color = Color.parseColor("#E37B31")
        }

        val lightGrayPaint = Paint().apply {
            color = Color.parseColor("#F3F4F6")
        }

        val chartPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
        }

        // Draw Header Banner
        canvas.drawRect(Rect(0, 0, 595, 80), primaryColorPaint)
        canvas.drawText("WALLET FINANCIAL REPORT", 25f, 42f, Paint(headerPaint).apply { textSize = 18f })
        canvas.drawText("Generated locally by Travel Split", 25f, 62f, Paint(headerPaint).apply { textSize = 10f })

        var y = 110f

        // Metadata
        canvas.drawText("Wallet Profile: ${wallet.name} (${wallet.type})", 25f, y, Paint(titlePaint).apply { textSize = 14f })
        y += 20f
        val genSdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.US)
        canvas.drawText("Export Date: ${genSdf.format(Date())}", 25f, y, subTitlePaint)
        y += 30f

        // Balance & Totals KPI Block
        val walletExpenses = expenses.filter { it.walletId == wallet.id }
        val totalSpent = walletExpenses.sumOf { it.amount }

        canvas.drawRoundRect(25f, y, 280f, y + 60f, 15f, 15f, lightGrayPaint)
        canvas.drawText("CURRENT LEDGER BALANCE", 40f, y + 23f, Paint(subTitlePaint).apply { isFakeBoldText = true; color = Color.GRAY; textSize = 8f })
        canvas.drawText("₹${String.format(Locale.US, "%.2f", wallet.balance)}", 40f, y + 48f, Paint(titlePaint).apply { textSize = 18f; color = Color.parseColor("#0C6B58") })

        canvas.drawRoundRect(310f, y, 570f, y + 60f, 15f, 15f, lightGrayPaint)
        canvas.drawText("TOTAL SPENT / FUNDED", 325f, y + 23f, Paint(subTitlePaint).apply { isFakeBoldText = true; color = Color.GRAY; textSize = 8f })
        canvas.drawText("₹${String.format(Locale.US, "%.2f", totalSpent)}", 325f, y + 48f, Paint(titlePaint).apply { textSize = 18f; color = Color.parseColor("#E37B31") })

        y += 85f

        // Progress breakdown charts
        canvas.drawText("CATEGORY SPENDING BREAKDOWN", 25f, y, Paint(titlePaint).apply { textSize = 11f })
        y += 15f

        val categoriesMap = walletExpenses.groupBy { it.category }.mapValues { it.value.sumOf { it.amount } }
        if (categoriesMap.isNotEmpty()) {
            categoriesMap.forEach { (cat, amt) ->
                val pct = if (totalSpent > 0.0) (amt / totalSpent).toFloat() else 0f
                canvas.drawText(cat, 25f, y + 10f, bodyPaint)

                canvas.drawRoundRect(180f, y + 2f, 450f, y + 10f, 4f, 4f, chartPaint)
                val barWidth = 270f * pct
                val barEnd = 180f + if (barWidth < 4f) 4f else barWidth
                canvas.drawRoundRect(180f, y + 2f, barEnd, y + 10f, 4f, 4f, accentColorPaint)

                canvas.drawText("${String.format(Locale.US, "%.1f", pct * 100f)}% (₹${String.format(Locale.US, "%.2f", amt)})", 460f, y + 10f, Paint(bodyPaint).apply { isFakeBoldText = true })
                y += 18f
            }
        } else {
            canvas.drawText("No categorized transactions funded under this wallet.", 25f, y + 10f, subTitlePaint)
            y += 18f
        }

        y += 20f

        // Table
        canvas.drawText("ITEMIZED TRANSACTION HISTORY", 25f, y, Paint(titlePaint).apply { textSize = 11f })
        y += 15f

        val colX = floatArrayOf(25f, 130f, 250f, 420f, 490f)
        canvas.drawRect(25f, y, 570f, y + 22f, primaryColorPaint)
        canvas.drawText("Date", colX[0] + 5f, y + 15f, Paint(headerPaint).apply { textSize = 9f })
        canvas.drawText("Category", colX[1], y + 15f, headerPaint)
        canvas.drawText("Description", colX[2], y + 15f, headerPaint)
        canvas.drawText("Paid By", colX[3], y + 15f, headerPaint)
        canvas.drawText("Amount", colX[4], y + 15f, headerPaint)
        y += 22f

        val rowPaint = Paint().apply { color = Color.WHITE }
        val altRowPaint = Paint().apply { color = Color.parseColor("#F9FAFB") }
        val cellBorderPaint = Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        val listSdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        walletExpenses.forEachIndexed { index, exp ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                canvas.drawRect(Rect(0, 0, 595, 40), primaryColorPaint)
                canvas.drawText("ITEMIZED WALLET TRANSACTIONS (Continued)", 25f, 25f, Paint(headerPaint).apply { textSize = 12f })

                y = 55f
                canvas.drawRect(25f, y, 570f, y + 22f, primaryColorPaint)
                canvas.drawText("Date", colX[0] + 5f, y + 15f, Paint(headerPaint).apply { textSize = 9f })
                canvas.drawText("Category", colX[1], y + 15f, headerPaint)
                canvas.drawText("Description", colX[2], y + 15f, headerPaint)
                canvas.drawText("Paid By", colX[3], y + 15f, headerPaint)
                canvas.drawText("Amount", colX[4], y + 15f, headerPaint)
                y += 22f
            }

            canvas.drawRect(25f, y, 570f, y + 18f, if (index % 2 == 0) rowPaint else altRowPaint)
            canvas.drawLine(25f, y + 18f, 570f, y + 18f, cellBorderPaint)

            val dateText = listSdf.format(Date(exp.date))
            val categoryText = if (exp.category.length > 15) exp.category.take(13) + ".." else exp.category
            val descText = if (exp.description.length > 22) exp.description.take(20) + ".." else exp.description
            val payerName = members.find { it.id == exp.paidById }?.name ?: "Unknown"
            val amtText = "₹${String.format(Locale.US, "%.2f", exp.amount)}"

            canvas.drawText(dateText, colX[0] + 5f, y + 13f, bodyPaint)
            canvas.drawText(categoryText, colX[1], y + 13f, bodyPaint)
            canvas.drawText(descText, colX[2], y + 13f, bodyPaint)
            canvas.drawText(payerName, colX[3], y + 13f, bodyPaint)
            canvas.drawText(amtText, colX[4], y + 13f, Paint(bodyPaint).apply { isFakeBoldText = true })
            y += 18f
        }

        // Footer
        if (y > 780f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
        }
        canvas.drawLine(25f, y + 10f, 570f, y + 10f, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        canvas.drawText("End of Audit Report. Total items parsed: ${walletExpenses.size}", 25f, y + 25f, Paint(subTitlePaint).apply { textSize = 8f; color = Color.GRAY })

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "report_wallet_${wallet.name.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use {
            pdfDocument.writeTo(it)
        }
        pdfDocument.close()
        return file
    }
}
