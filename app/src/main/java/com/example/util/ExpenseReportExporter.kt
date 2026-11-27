package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExpenseReportExporter {
    private const val TAG = "ExpenseReportExporter"

    private fun resolveLanguage(lang: Localization.Language?, isEnglish: Boolean): Localization.Language {
        return lang ?: if (isEnglish) Localization.Language.ENGLISH else Localization.Language.GREEK
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
        return "\"$clean\""
    }

    private fun htmlEscape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun formatAmount(amount: Double, currencySymbol: String): String {
        return "${String.format(Locale.US, "%.2f", amount)} $currencySymbol"
    }

    fun generateCsv(
        expenses: List<Expense>,
        periodTitle: String = "Όλα τα Έξοδα",
        incomes: List<Income> = emptyList(),
        members: List<FamilyMember> = emptyList(),
        currencySymbol: String = CurrencyFormatter.currentSymbol,
        householdName: String = "",
        isEnglish: Boolean = false,
        lang: Localization.Language? = null
    ): String {
        val resolvedLang = resolveLanguage(lang, isEnglish)
        val totalExpenses = expenses.sumOf { it.amount }
        val totalIncomes = incomes.sumOf { it.amount }
        val netBalance = totalIncomes - totalExpenses
        val savingsRate = if (totalIncomes > 0.0) ((netBalance / totalIncomes) * 100.0) else null

        val headerTitle = if (householdName.isNotBlank()) "FINSAY - $householdName ($periodTitle)" else "FINSAY - $periodTitle"
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens international characters cleanly
        sb.append('\uFEFF')
        sb.append("${csvEscape(headerTitle)}\n\n")

        // 1. Financial Summary
        sb.append("${Localization.tr(resolvedLang, "FINANCIAL SUMMARY", "ΟΙΚΟΝΟΜΙΚΗ ΣΥΝΟΨΗ")}\n")
        sb.append("${Localization.tr(resolvedLang, "Metric", "Δείκτης")},${Localization.tr(resolvedLang, "Amount", "Ποσό")} ($currencySymbol)\n")
        sb.append("${csvEscape(Localization.tr(resolvedLang, "Total Income", "Σύνολο Εσόδων"))},${String.format(Locale.US, "%.2f", totalIncomes)}\n")
        sb.append("${csvEscape(Localization.tr(resolvedLang, "Total Expenses", "Σύνολο Εξόδων"))},${String.format(Locale.US, "%.2f", totalExpenses)}\n")
        sb.append("${csvEscape(Localization.tr(resolvedLang, "Net Balance", "Καθαρό Υπόλοιπο"))},${String.format(Locale.US, "%.2f", netBalance)}\n")
        if (savingsRate != null) {
            sb.append("${csvEscape(Localization.tr(resolvedLang, "Savings Rate", "Ποσοστό Αποταμίευσης"))},${String.format(Locale.US, "%.1f%%", savingsRate)}\n")
        }
        sb.append("\n")

        // 2. Expenses Section
        val dateCol = Localization.tr(resolvedLang, "Date", "Ημερομηνία")
        val titleCol = Localization.tr(resolvedLang, "Title", "Τίτλος")
        val catCol = Localization.tr(resolvedLang, "Category", "Κατηγορία")
        val payerCol = Localization.tr(resolvedLang, "Payer", "Πληρωτής")
        val membersCol = Localization.tr(resolvedLang, "Members", "Μέλη")
        val memberCol = Localization.tr(resolvedLang, "Member", "Μέλος")
        val amountCol = Localization.tr(resolvedLang, "Amount", "Ποσό")
        val noteCol = Localization.tr(resolvedLang, "Note", "Σημείωση")

        sb.append("${Localization.tr(resolvedLang, "EXPENSES", "ΕΞΟΔΑ")} (${expenses.size})\n")
        sb.append("$dateCol,$titleCol,$catCol,$payerCol,$membersCol,$amountCol ($currencySymbol),$noteCol\n")
        for (expense in expenses) {
            val date = dateSdf.format(Date(expense.timestamp))
            val amount = String.format(Locale.US, "%.2f", expense.amount)
            val payer = expense.payerMemberName.ifBlank { "-" }
            sb.append(
                "${csvEscape(date)},${csvEscape(expense.title)},${csvEscape(expense.categoryName)}," +
                    "${csvEscape(payer)},${csvEscape(expense.memberNamesDisplay)},$amount,${csvEscape(expense.note)}\n"
            )
        }
        sb.append("${Localization.tr(resolvedLang, "TOTAL EXPENSES", "ΣΥΝΟΛΟ ΕΞΟΔΩΝ")},,,,,${String.format(Locale.US, "%.2f", totalExpenses)},\n")

        // 3. Incomes Section (when present)
        if (incomes.isNotEmpty()) {
            sb.append("\n")
            sb.append("${Localization.tr(resolvedLang, "INCOMES", "ΕΣΟΔΑ")} (${incomes.size})\n")
            sb.append("$dateCol,$titleCol,$catCol,$memberCol,$amountCol ($currencySymbol),$noteCol\n")
            for (income in incomes) {
                val date = dateSdf.format(Date(income.timestamp))
                val amount = String.format(Locale.US, "%.2f", income.amount)
                val memberName = income.memberName.ifBlank { "-" }
                sb.append(
                    "${csvEscape(date)},${csvEscape(income.title)},${csvEscape(income.category)}," +
                        "${csvEscape(memberName)},$amount,${csvEscape(income.note)}\n"
                )
            }
            sb.append("${Localization.tr(resolvedLang, "TOTAL INCOMES", "ΣΥΝΟΛΟ ΕΣΟΔΩΝ")},,,,${String.format(Locale.US, "%.2f", totalIncomes)},\n")
        }

        // 4. Member Settlement / Settle-Up Section (when multi-member & expenses exist)
        if (members.size > 1 && expenses.isNotEmpty()) {
            val settleResult = SettleUpCalculator.calculate(expenses, members)
            val paidCol = Localization.tr(resolvedLang, "Paid", "Πλήρωσε")
            val shareCol = Localization.tr(resolvedLang, "Fair Share", "Μερίδιο")
            val balanceCol = Localization.tr(resolvedLang, "Net Balance", "Υπόλοιπο")
            sb.append("\n")
            sb.append("${Localization.tr(resolvedLang, "MEMBER SETTLEMENT (SETTLE UP)", "ΕΚΚΑΘΑΡΙΣΗ ΜΕΛΩΝ")}\n")
            sb.append("$memberCol,$paidCol ($currencySymbol),$shareCol ($currencySymbol),$balanceCol ($currencySymbol)\n")
            for (s in settleResult.memberSummaries) {
                sb.append(
                    "${csvEscape(s.member.name)},${String.format(Locale.US, "%.2f", s.totalPaid)}," +
                        "${String.format(Locale.US, "%.2f", s.totalShare)},${String.format(Locale.US, "%.2f", s.netBalance)}\n"
                )
            }
            sb.append("\n")
            sb.append("${Localization.tr(resolvedLang, "RECOMMENDED TRANSFERS", "ΠΡΟΤΕΙΝΟΜΕΝΕΣ ΜΕΤΑΦΟΡΕΣ")}\n")
            if (settleResult.transfers.isEmpty()) {
                sb.append("${csvEscape(Localization.tr(resolvedLang, "All balances are settled up!", "Όλοι οι λογαριασμοί είναι ισοσκελισμένοι!"))}\n")
            } else {
                val fromCol = Localization.tr(resolvedLang, "From", "Από")
                val toCol = Localization.tr(resolvedLang, "To", "Προς")
                sb.append("$fromCol,$toCol,$amountCol ($currencySymbol)\n")
                for (t in settleResult.transfers) {
                    sb.append("${csvEscape(t.fromMember.name)},${csvEscape(t.toMember.name)},${String.format(Locale.US, "%.2f", t.amount)}\n")
                }
            }
        }

        return sb.toString()
    }

    private fun buildDateRangeFileSuffix(
        expenses: List<Expense>,
        incomes: List<Income> = emptyList(),
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): String {
        val fileDateSdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        val allTimestamps = expenses.map { it.timestamp } + incomes.map { it.timestamp }
        val startMs: Long
        val endMs: Long
        if (startDateMillis != null && endDateMillis != null) {
            startMs = minOf(startDateMillis, endDateMillis)
            endMs = maxOf(startDateMillis, endDateMillis)
        } else if (allTimestamps.isNotEmpty()) {
            startMs = allTimestamps.min()
            endMs = allTimestamps.max()
        } else {
            val now = System.currentTimeMillis()
            startMs = now
            endMs = now
        }
        val startStr = fileDateSdf.format(Date(startMs))
        val endStr = fileDateSdf.format(Date(endMs))
        return if (startStr == endStr) startStr else "$startStr-$endStr"
    }

    fun exportCsv(
        context: Context,
        expenses: List<Expense>,
        periodTitle: String = "Όλα τα Έξοδα",
        startDateMillis: Long? = null,
        endDateMillis: Long? = null,
        incomes: List<Income> = emptyList(),
        members: List<FamilyMember> = emptyList(),
        currencySymbol: String = CurrencyFormatter.currentSymbol,
        householdName: String = "",
        isEnglish: Boolean = false,
        lang: Localization.Language? = null
    ) {
        val resolvedLang = resolveLanguage(lang, isEnglish)
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateRange = buildDateRangeFileSuffix(expenses, incomes, startDateMillis, endDateMillis)
            val file = File(exportDir, "Finsay_Report_${dateRange}.csv")
            val csvContent = generateCsv(
                expenses = expenses,
                periodTitle = periodTitle,
                incomes = incomes,
                members = members,
                currencySymbol = currencySymbol,
                householdName = householdName,
                lang = resolvedLang
            )
            file.writeBytes(csvContent.toByteArray(Charsets.UTF_8))
            val shareTitle = Localization.trFormat(
                resolvedLang,
                "Finsay Financial Report (CSV) - %1\$s",
                "Οικονομική Αναφορά Finsay (CSV) - %1\$s",
                periodTitle
            )
            shareFile(context, file, "text/csv", shareTitle, resolvedLang)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV: ${e.message}", e)
            Handler(Looper.getMainLooper()).post {
                val unknown = Localization.tr(resolvedLang, "unknown", "άγνωστο")
                val msg = Localization.trFormat(
                    resolvedLang,
                    "CSV export error: %1\$s",
                    "Σφάλμα εξαγωγής CSV: %1\$s",
                    e.localizedMessage ?: unknown
                )
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportExcel(
        context: Context,
        expenses: List<Expense>,
        periodTitle: String = "Όλα τα Έξοδα",
        startDateMillis: Long? = null,
        endDateMillis: Long? = null,
        incomes: List<Income> = emptyList(),
        members: List<FamilyMember> = emptyList(),
        currencySymbol: String = CurrencyFormatter.currentSymbol,
        householdName: String = "",
        isEnglish: Boolean = false,
        lang: Localization.Language? = null
    ) {
        val resolvedLang = resolveLanguage(lang, isEnglish)
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateRange = buildDateRangeFileSuffix(expenses, incomes, startDateMillis, endDateMillis)
            val file = File(exportDir, "Finsay_Report_${dateRange}.xls")
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val totalExpenses = expenses.sumOf { it.amount }
            val totalIncomes = incomes.sumOf { it.amount }
            val netBalance = totalIncomes - totalExpenses
            val savingsRate = if (totalIncomes > 0.0) ((netBalance / totalIncomes) * 100.0) else null
            val headerTitle = if (householdName.isNotBlank()) "FINSAY - ${htmlEscape(householdName)} (${htmlEscape(periodTitle)})" else "FINSAY - ${htmlEscape(periodTitle)}"

            val dateCol = htmlEscape(Localization.tr(resolvedLang, "Date", "Ημερομηνία"))
            val titleCol = htmlEscape(Localization.tr(resolvedLang, "Title", "Τίτλος"))
            val catCol = htmlEscape(Localization.tr(resolvedLang, "Category", "Κατηγορία"))
            val payerCol = htmlEscape(Localization.tr(resolvedLang, "Payer", "Πληρωτής"))
            val membersCol = htmlEscape(Localization.tr(resolvedLang, "Members", "Μέλη"))
            val memberCol = htmlEscape(Localization.tr(resolvedLang, "Member", "Μέλος"))
            val amountCol = htmlEscape(Localization.tr(resolvedLang, "Amount", "Ποσό"))
            val noteCol = htmlEscape(Localization.tr(resolvedLang, "Note", "Σημείωση"))
            val safeCurr = htmlEscape(currencySymbol)

            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            sb.append("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n")
            sb.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n")
            sb.append("<style>\n")
            sb.append("body { font-family: Arial, sans-serif; color: #0F172A; }\n")
            sb.append("h2 { color: #047857; margin-bottom: 6px; }\n")
            sb.append("h3 { color: #065F46; margin-top: 22px; margin-bottom: 8px; }\n")
            sb.append("table { border-collapse: collapse; width: 100%; margin-bottom: 14px; }\n")
            sb.append("th { background-color: #047857; color: white; border: 1px solid #cbd5e1; padding: 8px; text-align: left; }\n")
            sb.append("th.income { background-color: #0D9488; }\n")
            sb.append("th.settle { background-color: #1E40AF; }\n")
            sb.append("td { border: 1px solid #e2e8f0; padding: 7px 8px; }\n")
            sb.append(".amount { text-align: right; white-space: nowrap; }\n")
            sb.append(".pos { color: #059669; font-weight: bold; }\n")
            sb.append(".neg { color: #DC2626; font-weight: bold; }\n")
            sb.append(".total { font-weight: bold; background-color: #F1F5F9; }\n")
            sb.append("</style></head><body>\n")
            sb.append("<h2>$headerTitle</h2>\n")

            // 1. Financial Summary Table
            sb.append("<h3>${htmlEscape(Localization.tr(resolvedLang, "Financial Summary", "Οικονομική Σύνοψη"))}</h3>\n")
            sb.append("<table><thead><tr><th>${htmlEscape(Localization.tr(resolvedLang, "Metric", "Δείκτης"))}</th><th class=\"amount\">$amountCol ($safeCurr)</th></tr></thead><tbody>\n")
            sb.append("<tr><td>${htmlEscape(Localization.tr(resolvedLang, "Total Income", "Σύνολο Εσόδων"))}</td><td class=\"amount pos\">${htmlEscape(formatAmount(totalIncomes, currencySymbol))}</td></tr>\n")
            sb.append("<tr><td>${htmlEscape(Localization.tr(resolvedLang, "Total Expenses", "Σύνολο Εξόδων"))}</td><td class=\"amount neg\">${htmlEscape(formatAmount(totalExpenses, currencySymbol))}</td></tr>\n")
            val netClass = if (netBalance >= 0.0) "pos" else "neg"
            val netPrefix = if (netBalance > 0.0) "+" else ""
            sb.append("<tr class=\"total\"><td>${htmlEscape(Localization.tr(resolvedLang, "Net Balance", "Καθαρό Υπόλοιπο"))}</td><td class=\"amount $netClass\">$netPrefix${htmlEscape(formatAmount(netBalance, currencySymbol))}</td></tr>\n")
            if (savingsRate != null) {
                sb.append("<tr><td>${htmlEscape(Localization.tr(resolvedLang, "Savings Rate", "Ποσοστό Αποταμίευσης"))}</td><td class=\"amount\">${String.format(Locale.US, "%.1f%%", savingsRate)}</td></tr>\n")
            }
            sb.append("</tbody></table>\n")

            // 2. Expenses Table
            sb.append("<h3>${htmlEscape(Localization.tr(resolvedLang, "Expenses", "Έξοδα"))} (${expenses.size})</h3>\n")
            sb.append("<table>\n<thead><tr>")
            sb.append("<th>$dateCol</th><th>$titleCol</th><th>$catCol</th><th>$payerCol</th><th>$membersCol</th><th class=\"amount\">$amountCol ($safeCurr)</th><th>$noteCol</th>")
            sb.append("</tr></thead>\n<tbody>\n")

            for (expense in expenses) {
                val date = dateSdf.format(Date(expense.timestamp))
                val amount = formatAmount(expense.amount, currencySymbol)
                val payer = expense.payerMemberName.ifBlank { "-" }
                sb.append(
                    "<tr><td>$date</td><td>${htmlEscape(expense.title)}</td><td>${htmlEscape(expense.categoryName)}</td>" +
                        "<td>${htmlEscape(payer)}</td><td>${htmlEscape(expense.memberNamesDisplay)}</td>" +
                        "<td class=\"amount\">${htmlEscape(amount)}</td><td>${htmlEscape(expense.note)}</td></tr>\n"
                )
            }
            sb.append(
                "<tr class=\"total\"><td colspan=\"5\">${htmlEscape(Localization.tr(resolvedLang, "TOTAL EXPENSES", "ΣΥΝΟΛΟ ΕΞΟΔΩΝ"))}</td>" +
                    "<td class=\"amount\">${htmlEscape(formatAmount(totalExpenses, currencySymbol))}</td><td></td></tr>\n"
            )
            sb.append("</tbody></table>\n")

            // 3. Incomes Table
            if (incomes.isNotEmpty()) {
                sb.append("<h3>${htmlEscape(Localization.tr(resolvedLang, "Incomes", "Έσοδα"))} (${incomes.size})</h3>\n")
                sb.append("<table>\n<thead><tr>")
                sb.append("<th class=\"income\">$dateCol</th><th class=\"income\">$titleCol</th><th class=\"income\">$catCol</th><th class=\"income\">$memberCol</th><th class=\"income amount\">$amountCol ($safeCurr)</th><th class=\"income\">$noteCol</th>")
                sb.append("</tr></thead>\n<tbody>\n")
                for (income in incomes) {
                    val date = dateSdf.format(Date(income.timestamp))
                    val amount = formatAmount(income.amount, currencySymbol)
                    val memberName = income.memberName.ifBlank { "-" }
                    sb.append(
                        "<tr><td>$date</td><td>${htmlEscape(income.title)}</td><td>${htmlEscape(income.category)}</td>" +
                            "<td>${htmlEscape(memberName)}</td><td class=\"amount pos\">${htmlEscape(amount)}</td><td>${htmlEscape(income.note)}</td></tr>\n"
                    )
                }
                sb.append(
                    "<tr class=\"total\"><td colspan=\"4\">${htmlEscape(Localization.tr(resolvedLang, "TOTAL INCOMES", "ΣΥΝΟΛΟ ΕΣΟΔΩΝ"))}</td>" +
                        "<td class=\"amount pos\">${htmlEscape(formatAmount(totalIncomes, currencySymbol))}</td><td></td></tr>\n"
                )
                sb.append("</tbody></table>\n")
            }

            // 4. Member Settlement / Settle-Up Section
            if (members.size > 1 && expenses.isNotEmpty()) {
                val settleResult = SettleUpCalculator.calculate(expenses, members)
                val paidCol = htmlEscape(Localization.tr(resolvedLang, "Paid", "Πλήρωσε"))
                val shareCol = htmlEscape(Localization.tr(resolvedLang, "Fair Share", "Μερίδιο"))
                val balanceCol = htmlEscape(Localization.tr(resolvedLang, "Net Balance", "Υπόλοιπο"))
                sb.append("<h3>${htmlEscape(Localization.tr(resolvedLang, "Member Settlement (Settle Up)", "Εκκαθάριση Μελών"))}</h3>\n")
                sb.append("<table><thead><tr>")
                sb.append("<th class=\"settle\">$memberCol</th><th class=\"settle amount\">$paidCol ($safeCurr)</th><th class=\"settle amount\">$shareCol ($safeCurr)</th><th class=\"settle amount\">$balanceCol ($safeCurr)</th>")
                sb.append("</tr></thead><tbody>\n")
                for (s in settleResult.memberSummaries) {
                    val cls = when {
                        s.netBalance > 0.01 -> "pos"
                        s.netBalance < -0.01 -> "neg"
                        else -> ""
                    }
                    val prefix = if (s.netBalance > 0.01) "+" else ""
                    sb.append(
                        "<tr><td>${htmlEscape(s.member.name)}</td><td class=\"amount\">${htmlEscape(formatAmount(s.totalPaid, currencySymbol))}</td>" +
                            "<td class=\"amount\">${htmlEscape(formatAmount(s.totalShare, currencySymbol))}</td>" +
                            "<td class=\"amount $cls\">$prefix${htmlEscape(formatAmount(s.netBalance, currencySymbol))}</td></tr>\n"
                    )
                }
                sb.append("</tbody></table>\n")

                sb.append("<h3>${htmlEscape(Localization.tr(resolvedLang, "Recommended Transfers", "Προτεινόμενες Μεταφορές"))}</h3>\n")
                if (settleResult.transfers.isEmpty()) {
                    sb.append("<p class=\"pos\">${htmlEscape(Localization.tr(resolvedLang, "All balances are settled up!", "Όλοι οι λογαριασμοί είναι απόλυτα ισοσκελισμένοι!"))}</p>\n")
                } else {
                    val fromCol = htmlEscape(Localization.tr(resolvedLang, "From", "Από"))
                    val toCol = htmlEscape(Localization.tr(resolvedLang, "To", "Προς"))
                    sb.append("<table><thead><tr><th class=\"settle\">$fromCol</th><th class=\"settle\">$toCol</th><th class=\"settle amount\">$amountCol ($safeCurr)</th></tr></thead><tbody>\n")
                    for (t in settleResult.transfers) {
                        sb.append("<tr><td>${htmlEscape(t.fromMember.name)}</td><td>${htmlEscape(t.toMember.name)}</td><td class=\"amount\"><b>${htmlEscape(formatAmount(t.amount, currencySymbol))}</b></td></tr>\n")
                    }
                    sb.append("</tbody></table>\n")
                }
            }

            sb.append("</body></html>")

            file.writeBytes(sb.toString().toByteArray(Charsets.UTF_8))
            val shareTitle = Localization.trFormat(
                resolvedLang,
                "Finsay Financial Report (Excel) - %1\$s",
                "Οικονομική Αναφορά Finsay (Excel) - %1\$s",
                periodTitle
            )
            shareFile(context, file, "application/vnd.ms-excel", shareTitle, resolvedLang)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting Excel: ${e.message}", e)
            Handler(Looper.getMainLooper()).post {
                val unknown = Localization.tr(resolvedLang, "unknown", "άγνωστο")
                val msg = Localization.trFormat(
                    resolvedLang,
                    "Excel export error: %1\$s",
                    "Σφάλμα εξαγωγής Excel: %1\$s",
                    e.localizedMessage ?: unknown
                )
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportPdf(
        context: Context,
        expenses: List<Expense>,
        periodTitle: String = "Όλα τα Έξοδα",
        startDateMillis: Long? = null,
        endDateMillis: Long? = null,
        incomes: List<Income> = emptyList(),
        members: List<FamilyMember> = emptyList(),
        currencySymbol: String = CurrencyFormatter.currentSymbol,
        householdName: String = "",
        isEnglish: Boolean = false,
        lang: Localization.Language? = null
    ) {
        val resolvedLang = resolveLanguage(lang, isEnglish)
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateRange = buildDateRangeFileSuffix(expenses, incomes, startDateMillis, endDateMillis)
            val file = File(exportDir, "Finsay_Report_${dateRange}.pdf")

            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val marginX = 36f
            val contentRight = pageWidth - marginX
            val bottomLimit = pageHeight - 46f

            val paint = Paint().apply { isAntiAlias = true }
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val titleHeader = if (householdName.isNotBlank()) "Finsay • $householdName" else "Finsay"

            var pageNumber = 0
            var currentPage: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 0f

            fun startNewPage() {
                currentPage?.let { pdfDocument.finishPage(it) }
                pageNumber += 1
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                currentPage = page
                canvas = page.canvas

                // Top header bar
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#047857")
                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                page.canvas.drawText(titleHeader, marginX, 44f, paint)

                paint.color = Color.parseColor("#475569")
                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                page.canvas.drawText(periodTitle, marginX, 59f, paint)

                val pageLabel = Localization.trFormat(resolvedLang, "Page %1\$d", "Σελίδα %1\$d", pageNumber)
                val pageLabelW = paint.measureText(pageLabel)
                page.canvas.drawText(pageLabel, contentRight - pageLabelW, 44f, paint)

                paint.color = Color.parseColor("#CBD5E1")
                paint.strokeWidth = 1f
                page.canvas.drawLine(marginX, 68f, contentRight, 68f, paint)
                y = 86f
            }

            fun ensureSpace(neededHeight: Float, onNewPage: (() -> Unit)? = null) {
                if (currentPage == null) {
                    startNewPage()
                } else if (y + neededHeight > bottomLimit) {
                    startNewPage()
                    onNewPage?.invoke()
                }
            }

            fun drawSectionTitle(title: String, accentHex: String = "#047857") {
                ensureSpace(34f)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor(accentHex)
                paint.textSize = 13f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas!!.drawText(title, marginX, y, paint)
                y += 7f
                paint.color = Color.parseColor(accentHex)
                paint.strokeWidth = 1.2f
                canvas!!.drawLine(marginX, y, contentRight, y, paint)
                y += 16f
            }

            fun clipText(text: String, maxChars: Int): String {
                return if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
            }

            startNewPage()

            // 1. Financial Summary Box
            val totalExpenses = expenses.sumOf { it.amount }
            val totalIncomes = incomes.sumOf { it.amount }
            val netBalance = totalIncomes - totalExpenses
            val savingsRate = if (totalIncomes > 0.0) ((netBalance / totalIncomes) * 100.0) else null

            val boxRect = RectF(marginX, y, contentRight, y + 66f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F0FDF4")
            canvas!!.drawRoundRect(boxRect, 10f, 10f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#A7F3D0")
            paint.strokeWidth = 1f
            canvas!!.drawRoundRect(boxRect, 10f, 10f, paint)

            val colW = (contentRight - marginX) / 3f
            paint.style = Paint.Style.FILL

            // Col 1: Total Income
            paint.color = Color.parseColor("#475569")
            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            canvas!!.drawText(Localization.tr(resolvedLang, "TOTAL INCOME", "ΣΥΝΟΛΟ ΕΣΟΔΩΝ"), marginX + 12f, y + 20f, paint)
            paint.color = Color.parseColor("#059669")
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas!!.drawText(formatAmount(totalIncomes, currencySymbol), marginX + 12f, y + 40f, paint)

            // Col 2: Total Expenses
            paint.color = Color.parseColor("#475569")
            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            canvas!!.drawText(Localization.tr(resolvedLang, "TOTAL EXPENSES", "ΣΥΝΟΛΟ ΕΞΟΔΩΝ"), marginX + colW + 12f, y + 20f, paint)
            paint.color = Color.parseColor("#DC2626")
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas!!.drawText(formatAmount(totalExpenses, currencySymbol), marginX + colW + 12f, y + 40f, paint)

            // Col 3: Net Balance
            paint.color = Color.parseColor("#475569")
            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            canvas!!.drawText(Localization.tr(resolvedLang, "NET BALANCE", "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ"), marginX + colW * 2 + 12f, y + 20f, paint)
            paint.color = if (netBalance >= 0.0) Color.parseColor("#047857") else Color.parseColor("#DC2626")
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val netSign = if (netBalance > 0.0) "+" else ""
            canvas!!.drawText("$netSign${formatAmount(netBalance, currencySymbol)}", marginX + colW * 2 + 12f, y + 40f, paint)

            if (savingsRate != null) {
                paint.color = Color.parseColor("#334155")
                paint.textSize = 9f
                paint.typeface = Typeface.DEFAULT
                val rateText = Localization.trFormat(
                    resolvedLang,
                    "Savings rate: %1\$s",
                    "Ποσοστό αποταμίευσης: %1\$s",
                    String.format(Locale.US, "%.1f%%", savingsRate)
                )
                canvas!!.drawText(rateText, marginX + 12f, y + 57f, paint)
            }
            y += 84f

            val dateShort = Localization.tr(resolvedLang, "Date", "Ημερ/νία")
            val titleHeaderCol = Localization.tr(resolvedLang, "Title", "Περιγραφή")
            val catHeaderCol = Localization.tr(resolvedLang, "Category", "Κατηγορία")
            val payerHeaderCol = Localization.tr(resolvedLang, "Payer", "Πληρωτής")
            val memberHeaderCol = Localization.tr(resolvedLang, "Member", "Μέλος")
            val amountHeaderCol = Localization.tr(resolvedLang, "Amount", "Ποσό")

            // 2. Expenses Section
            fun drawExpenseTableHeader() {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas!!.drawText(dateShort, marginX, y, paint)
                canvas!!.drawText(titleHeaderCol, 104f, y, paint)
                canvas!!.drawText(catHeaderCol, 252f, y, paint)
                canvas!!.drawText(payerHeaderCol, 364f, y, paint)
                canvas!!.drawText(amountHeaderCol, 476f, y, paint)
                y += 6f
                paint.color = Color.parseColor("#CBD5E1")
                paint.strokeWidth = 0.8f
                canvas!!.drawLine(marginX, y, contentRight, y, paint)
                y += 14f
            }

            drawSectionTitle("${Localization.tr(resolvedLang, "Expenses", "Έξοδα")} (${expenses.size})")
            if (expenses.isEmpty()) {
                paint.color = Color.parseColor("#64748B")
                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                canvas!!.drawText(Localization.tr(resolvedLang, "No expenses in this period.", "Δεν υπάρχουν έξοδα για την περίοδο."), marginX, y, paint)
                y += 20f
            } else {
                drawExpenseTableHeader()
                for (exp in expenses) {
                    ensureSpace(19f) { drawExpenseTableHeader() }
                    paint.style = Paint.Style.FILL
                    paint.typeface = Typeface.DEFAULT
                    paint.textSize = 9.5f
                    paint.color = Color.parseColor("#0F172A")

                    canvas!!.drawText(dateSdf.format(Date(exp.timestamp)), marginX, y, paint)
                    canvas!!.drawText(clipText(exp.title, 24), 104f, y, paint)
                    canvas!!.drawText(clipText(exp.categoryName, 17), 252f, y, paint)
                    canvas!!.drawText(clipText(exp.payerMemberName.ifBlank { "-" }, 16), 364f, y, paint)
                    canvas!!.drawText(formatAmount(exp.amount, currencySymbol), 476f, y, paint)
                    y += 17f
                }
                ensureSpace(24f)
                paint.color = Color.parseColor("#047857")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10.5f
                val totalLabel = "${Localization.tr(resolvedLang, "Total Expenses", "Σύνολο Εξόδων")}: ${formatAmount(totalExpenses, currencySymbol)}"
                canvas!!.drawText(totalLabel, 360f, y + 4f, paint)
                y += 26f
            }

            // 3. Incomes Section (when present)
            if (incomes.isNotEmpty()) {
                fun drawIncomeTableHeader() {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#1E293B")
                    paint.textSize = 9.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas!!.drawText(dateShort, marginX, y, paint)
                    canvas!!.drawText(titleHeaderCol, 104f, y, paint)
                    canvas!!.drawText(catHeaderCol, 258f, y, paint)
                    canvas!!.drawText(memberHeaderCol, 372f, y, paint)
                    canvas!!.drawText(amountHeaderCol, 476f, y, paint)
                    y += 6f
                    paint.color = Color.parseColor("#CBD5E1")
                    paint.strokeWidth = 0.8f
                    canvas!!.drawLine(marginX, y, contentRight, y, paint)
                    y += 14f
                }

                drawSectionTitle("${Localization.tr(resolvedLang, "Incomes", "Έσοδα")} (${incomes.size})", "#0D9488")
                drawIncomeTableHeader()
                for (inc in incomes) {
                    ensureSpace(19f) { drawIncomeTableHeader() }
                    paint.style = Paint.Style.FILL
                    paint.typeface = Typeface.DEFAULT
                    paint.textSize = 9.5f
                    paint.color = Color.parseColor("#0F172A")

                    canvas!!.drawText(dateSdf.format(Date(inc.timestamp)), marginX, y, paint)
                    canvas!!.drawText(clipText(inc.title, 24), 104f, y, paint)
                    canvas!!.drawText(clipText(inc.category, 17), 258f, y, paint)
                    canvas!!.drawText(clipText(inc.memberName.ifBlank { "-" }, 15), 372f, y, paint)
                    paint.color = Color.parseColor("#059669")
                    canvas!!.drawText("+${formatAmount(inc.amount, currencySymbol)}", 476f, y, paint)
                    y += 17f
                }
                ensureSpace(24f)
                paint.color = Color.parseColor("#0D9488")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10.5f
                val totalIncLabel = "${Localization.tr(resolvedLang, "Total Incomes", "Σύνολο Εσόδων")}: ${formatAmount(totalIncomes, currencySymbol)}"
                canvas!!.drawText(totalIncLabel, 360f, y + 4f, paint)
                y += 26f
            }

            // 4. Member Settlement / Settle-Up Section
            if (members.size > 1 && expenses.isNotEmpty()) {
                val settleResult = SettleUpCalculator.calculate(expenses, members)
                fun drawSettleHeader() {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#1E293B")
                    paint.textSize = 9.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas!!.drawText(memberHeaderCol, marginX, y, paint)
                    canvas!!.drawText(Localization.tr(resolvedLang, "Paid", "Πλήρωσε"), 220f, y, paint)
                    canvas!!.drawText(Localization.tr(resolvedLang, "Fair Share", "Μερίδιο"), 335f, y, paint)
                    canvas!!.drawText(Localization.tr(resolvedLang, "Net Balance", "Υπόλοιπο"), 455f, y, paint)
                    y += 6f
                    paint.color = Color.parseColor("#CBD5E1")
                    paint.strokeWidth = 0.8f
                    canvas!!.drawLine(marginX, y, contentRight, y, paint)
                    y += 14f
                }

                drawSectionTitle(Localization.tr(resolvedLang, "Member Settlement (Settle Up)", "Εκκαθάριση Μελών"), "#1E40AF")
                drawSettleHeader()
                for (s in settleResult.memberSummaries) {
                    ensureSpace(19f) { drawSettleHeader() }
                    paint.style = Paint.Style.FILL
                    paint.typeface = Typeface.DEFAULT
                    paint.textSize = 9.5f
                    paint.color = Color.parseColor("#0F172A")
                    canvas!!.drawText(clipText(s.member.name, 26), marginX, y, paint)
                    canvas!!.drawText(formatAmount(s.totalPaid, currencySymbol), 220f, y, paint)
                    canvas!!.drawText(formatAmount(s.totalShare, currencySymbol), 335f, y, paint)
                    paint.color = when {
                        s.netBalance > 0.01 -> Color.parseColor("#059669")
                        s.netBalance < -0.01 -> Color.parseColor("#DC2626")
                        else -> Color.parseColor("#475569")
                    }
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val prefix = if (s.netBalance > 0.01) "+" else ""
                    canvas!!.drawText("$prefix${formatAmount(s.netBalance, currencySymbol)}", 455f, y, paint)
                    y += 17f
                }

                y += 8f
                ensureSpace(28f)
                paint.color = Color.parseColor("#1E40AF")
                paint.textSize = 10.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas!!.drawText(Localization.tr(resolvedLang, "Recommended Transfers", "Προτεινόμενες Μεταφορές"), marginX, y, paint)
                y += 16f

                if (settleResult.transfers.isEmpty()) {
                    paint.color = Color.parseColor("#059669")
                    paint.textSize = 10f
                    paint.typeface = Typeface.DEFAULT
                    canvas!!.drawText(
                        Localization.tr(resolvedLang, "All balances are settled up!", "Όλοι οι λογαριασμοί είναι απόλυτα ισοσκελισμένοι!"),
                        marginX,
                        y,
                        paint
                    )
                    y += 18f
                } else {
                    for (t in settleResult.transfers) {
                        ensureSpace(18f)
                        paint.color = Color.parseColor("#0F172A")
                        paint.textSize = 10f
                        paint.typeface = Typeface.DEFAULT
                        canvas!!.drawText(
                            "${t.fromMember.name}  ->  ${t.toMember.name}:  ${formatAmount(t.amount, currencySymbol)}",
                            marginX + 6f,
                            y,
                            paint
                        )
                        y += 17f
                    }
                }
            }

            currentPage?.let { pdfDocument.finishPage(it) }

            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            Handler(Looper.getMainLooper()).post {
                val shareTitle = Localization.trFormat(
                    resolvedLang,
                    "Finsay Financial Report (PDF) - %1\$s",
                    "Οικονομική Αναφορά Finsay (PDF) - %1\$s",
                    periodTitle
                )
                shareFile(context, file, "application/pdf", shareTitle, resolvedLang)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting PDF: ${e.message}", e)
            Handler(Looper.getMainLooper()).post {
                val msg = Localization.trFormat(
                    resolvedLang,
                    "PDF creation error: %1\$s",
                    "Σφάλμα κατά τη δημιουργία PDF: %1\$s",
                    e.message ?: ""
                )
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        title: String,
        lang: Localization.Language
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                clipData = ClipData.newRawUri(title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                clipData = ClipData.newRawUri(title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}", e)
            val unknown = Localization.tr(lang, "unknown", "άγνωστο")
            val msg = Localization.trFormat(
                lang,
                "File sharing error: %1\$s",
                "Σφάλμα κοινοποίησης αρχείου: %1\$s",
                e.localizedMessage ?: unknown
            )
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
