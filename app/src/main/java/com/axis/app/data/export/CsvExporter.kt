package com.axis.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.axis.app.data.model.Transaction
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exports transactions to CSV format.
 * Supports sharing via Android share sheet.
 */
class CsvExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val formatter = NumberFormat.getNumberInstance(Locale("en", "KE"))

    /**
     * Export transactions to a CSV file and return its URI for sharing.
     *
     * @param transactions List of transactions to export
     * @param filename Optional custom filename
     * @return File if export succeeded, null otherwise
     */
    fun exportToCsv(
        transactions: List<Transaction>,
        filename: String = "axis_transactions_${System.currentTimeMillis()}.csv"
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, filename)

            FileWriter(file).use { writer ->
                // Header
                writer.appendLine(
                    listOf(
                        "Date",
                        "Transaction Code",
                        "Type",
                        "Category",
                        "Recipient",
                        "Amount (KES)",
                        "Transaction Cost (KES)",
                        "Income",
                        "Balance After (KES)"
                    ).joinToString(",") { "\"$it\"" }
                )

                // Rows
                transactions
                    .sortedByDescending { it.transactionDateTime }
                    .forEach { tx ->
                        writer.appendLine(
                            listOf(
                                dateFormat.format(Date(tx.transactionDateTime)),
                                tx.transactionCode,
                                tx.type,
                                tx.category,
                                tx.recipient ?: "",
                                tx.amount.toString(),
                                tx.transactionCost.toString(),
                                if (tx.isIncome) "Yes" else "No",
                                tx.balanceAfter.toString()
                            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                        )
                    }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a summary CSV alongside the transactions.
     */
    fun exportSummary(
        transactions: List<Transaction>,
        monthlyIncome: Double,
        monthlyExpenses: Double
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "axis_summary_${System.currentTimeMillis()}.csv")

            FileWriter(file).use { writer ->
                writer.appendLine("\"Axis Financial Summary\"")
                writer.appendLine("\"Generated\",\"${dateFormat.format(Date())}\"")
                writer.appendLine()

                writer.appendLine("\"Metric\",\"Value\"")
                writer.appendLine("\"Total Transactions\",\"${transactions.size}\"")
                writer.appendLine("\"Monthly Income\",\"Ksh ${formatter.format(monthlyIncome)}\"")
                writer.appendLine("\"Monthly Expenses\",\"Ksh ${formatter.format(monthlyExpenses)}\"")

                val savingsRate = if (monthlyIncome > 0)
                    ((monthlyIncome - monthlyExpenses) / monthlyIncome * 100).toInt()
                else 0
                writer.appendLine("\"Savings Rate\",\"${savingsRate}%\"")
                writer.appendLine()

                // Category breakdown
                writer.appendLine("\"Category\",\"Total Spent (KES)\",\"Transaction Count\"")
                transactions
                    .filter { !it.isIncome }
                    .groupBy { it.category }
                    .mapValues { (_, txs) -> Pair(txs.sumOf { it.amount }, txs.size) }
                    .entries
                    .sortedByDescending { it.value.first }
                    .forEach { (category, data) ->
                        writer.appendLine("\"$category\",\"${data.first}\",\"${data.second}\"")
                    }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share a file via Android share sheet.
     */
    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Axis Transactions Export")
            putExtra(Intent.EXTRA_TEXT, "Here's your M-Pesa transaction data exported from Axis.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Transactions")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
