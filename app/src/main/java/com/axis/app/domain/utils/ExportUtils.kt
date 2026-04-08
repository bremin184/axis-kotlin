package com.axis.app.domain.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class FinancialSummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val savingsRate: Double,
    val categoryBreakdown: Map<String, Double>,
    val mode: String
)

data class ProfessionalAnalysis(
    val userName: String,
    val monthlyIncomeTrend: Double,
    val expenseRatio: Double,
    val savingsEfficiency: Int,
    val topCategories: List<Pair<String, Double>>,
    val healthScore: Int,
    val riskIndicators: List<String>,
    val recommendations: List<String>
)

object ExportUtils {

    suspend fun exportToCsv(context: Context, summary: FinancialSummary): File? = withContext(Dispatchers.IO) {
        val fileName = "Axis_Summary_${System.currentTimeMillis()}.csv"
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext null
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, fileName)
        
        try {
            val writer = FileOutputStream(file).bufferedWriter()
            writer.write("Parameter,Value\n")
            writer.write("Mode,${summary.mode}\n")
            writer.write("Total Income,${summary.totalIncome}\n")
            writer.write("Total Expenses,${summary.totalExpenses}\n")
            writer.write("Savings Rate,${String.format("%.1f", summary.savingsRate)}%\n")
            writer.write("\nCategory Breakdown\n")
            summary.categoryBreakdown.forEach { (cat, amount) ->
                writer.write("$cat,$amount\n")
            }
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportToPdf(context: Context, analysis: ProfessionalAnalysis): File? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        
        // Header Branding
        paint.color = Color.parseColor("#2563EB")
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("AXIS FINANCIAL ANALYSIS", 50f, 60f, paint)
        
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Report for ${analysis.userName} | $date", 50f, 80f, paint)
        
        // Accent Lines
        paint.color = Color.parseColor("#2563EB")
        paint.strokeWidth = 1.5f
        canvas.drawLine(50f, 95f, 545f, 95f, paint)
        
        // Summary Block
        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Financial Health Score: ${analysis.healthScore}/100", 50f, 135f, paint)
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Monthly Income Trend: ${String.format("%.1f", analysis.monthlyIncomeTrend)}%", 50f, 160f, paint)
        canvas.drawText("Expense Ratio: ${String.format("%.1f", analysis.expenseRatio * 100)}%", 50f, 180f, paint)
        canvas.drawText("Savings Efficiency: ${analysis.savingsEfficiency}%", 50f, 200f, paint)
        
        // Top 5 Categories
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Top Expense Categories", 50f, 245f, paint)
        
        paint.textSize = 11f
        paint.isFakeBoldText = false
        var y = 270f
        analysis.topCategories.take(5).forEach { (cat, amount) ->
            canvas.drawText("• $cat: Ksh ${String.format("%,.2f", amount)}", 70f, y, paint)
            y += 20f
        }
        
        // Risk Factors Section
        y += 15f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Risk Indicators", 50f, y, paint)
        y += 25f
        paint.textSize = 11f
        paint.isFakeBoldText = false
        if (analysis.riskIndicators.isEmpty()) {
            canvas.drawText("No significant risks detected.", 70f, y, paint)
            y += 20f
        } else {
            analysis.riskIndicators.forEach { risk ->
                canvas.drawText("! $risk", 70f, y, paint)
                y += 20f
            }
        }
        
        // Strategic Recommendations
        y += 15f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Improvement Recommendations", 50f, y, paint)
        y += 25f
        paint.textSize = 11f
        paint.isFakeBoldText = false
        analysis.recommendations.forEach { rec ->
            canvas.drawText("→ $rec", 70f, y, paint)
            y += 20f
        }
        
        // Footer Notes
        paint.color = Color.LTGRAY
        paint.textSize = 9f
        canvas.drawText("Generated by Axis Financial Intelligence. All data calculations subject to SMS accuracy.", 50f, 810f, paint)

        pdfDocument.finishPage(page)
        
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext null
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, "Axis_Analysis_${System.currentTimeMillis()}.pdf")
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val type = if (file.name.endsWith(".pdf")) "application/pdf" else "text/csv"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            this.type = type
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share axis report"))
    }
}
