package com.axis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.ui.theme.LocalMorphismConfig
import java.text.NumberFormat
import java.util.Locale
import com.axis.app.ui.theme.CategoryColorRegistry

@Composable
fun IncomeVsExpenseChart(
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier
) {
    val morphism = LocalMorphismConfig.current
    val formatter = rememberNumberFormatter()
    val maxVal = maxOf(income, expenses).coerceAtLeast(1.0)
    
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padding = 40.dp.toPx()
                val chartWidth = size.width - (padding * 2)
                val chartHeight = size.height - 40.dp.toPx()
                val barWidth = 60.dp.toPx()
                
                // Draw Grid Lines (minimal)
                drawLine(
                    color = morphism.textMuted.copy(alpha = 0.2f),
                    start = Offset(padding, chartHeight),
                    end = Offset(size.width - padding, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // Income Bar
                val incomeHeight = (income / maxVal * chartHeight).toFloat()
                drawRect(
                    color = morphism.successColor,
                    topLeft = Offset(padding + (chartWidth / 4) - (barWidth / 2), chartHeight - incomeHeight),
                    size = Size(barWidth, incomeHeight)
                )

                // Expense Bar
                val expenseHeight = (expenses / maxVal * chartHeight).toFloat()
                drawRect(
                    color = morphism.dangerColor,
                    topLeft = Offset(padding + (3 * chartWidth / 4) - (barWidth / 2), chartHeight - expenseHeight),
                    size = Size(barWidth, expenseHeight)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Income", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                Text("Ksh ${formatter.format(income)}", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Expenses", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                Text("Ksh ${formatter.format(expenses)}", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categoryTotals: Map<String, Double>,
    colors: Map<String, Color>? = null,
    modifier: Modifier = Modifier
) {
    val morphism = LocalMorphismConfig.current
    val total = categoryTotals.values.sum().coerceAtLeast(1.0)
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            val strokeWidth = 30.dp.toPx()
            
            categoryTotals.forEach { (category, value) ->
                val sweepAngle = (value / total * 360).toFloat()
                drawArc(
                    color = colors?.get(category) ?: CategoryColorRegistry.getColor(category),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total Spend", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
            Text("Ksh ${NumberFormat.getNumberInstance(Locale("en", "KE")).format(total)}", 
                style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
fun ScoreBreakdownItem(
    label: String,
    score: Int,
    maxScore: Int = 20,
    color: Color,
    modifier: Modifier = Modifier
) {
    val morphism = LocalMorphismConfig.current
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = morphism.textPrimary)
            Text("$score / $maxScore", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            val trackWidth = size.width
            val progressWidth = (score.toFloat() / maxScore.toFloat()) * trackWidth
            
            drawRoundRect(
                color = morphism.textMuted.copy(alpha = 0.1f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = color,
                size = Size(progressWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
}

@Composable
private fun rememberNumberFormatter() = androidx.compose.runtime.remember {
    NumberFormat.getNumberInstance(Locale("en", "KE"))
}
