package com.axis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.domain.scoring.AnalyticsHelper
import com.axis.app.domain.scoring.HealthRating
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing
import java.text.NumberFormat
import java.util.*

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val healthScoreDetail by viewModel.healthScoreDetail.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()
    val expenses by viewModel.monthlyExpenses.collectAsState()
    val morphism = LocalMorphismConfig.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }

    val savingsRate = com.axis.app.domain.scoring.FinancialHealthCalculator.calculateSavingsPercent(income, expenses).toInt()
    val tips = remember(healthScoreDetail) {
        healthScoreDetail?.let { AnalyticsHelper.generateTips(it) } ?: emptyList()
    }

    val categoryColorMap = remember(transactions) {
        transactions.map { it.category }.distinct().associateWith { cat ->
            val meta = com.axis.app.domain.classifier.CategoryClassifier.CATEGORY_META[cat]
            try {
                Color(android.graphics.Color.parseColor(meta?.color ?: "#868E96"))
            } catch (_: Exception) { Color(0xFF868E96) }
        }
    }

    val categoryTotals = remember(transactions) {
        transactions.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(morphism.neuShadow.backgroundColor),
        contentPadding = PaddingValues(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l)
    ) {
        item {
            Text(
                "Financial Intelligence",
                style = MaterialTheme.typography.headlineSmall,
                color = morphism.textPrimary
            )
        }

        // ====== Quick Stats Stack ======
        item {
            val trend by viewModel.monthlyTrend.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s)
            ) {
                StatCard(
                    title = "Tx Count",
                    value = "${transactions.size}",
                    icon = Icons.Filled.History,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Saved %",
                    value = "$savingsRate%",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f),
                    color = if (savingsRate >= 20) morphism.successColor else morphism.warningColor
                )
                StatCard(
                    title = "Trend",
                    value = "${if (trend >= 0) "+" else ""}${trend.toInt()}%",
                    icon = if (trend <= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f),
                    color = if (trend <= 0) morphism.successColor else morphism.dangerColor
                )
            }
        }

        // ====== Savings Circular Progress ======
        item {
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Financial Health Overview",
                        style = MaterialTheme.typography.titleMedium,
                        color = morphism.textPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        NeuCircularProgress(
                            percentage = (healthScoreDetail?.total?.toFloat() ?: 0f) / 100f,
                            size = 140.dp,
                            strokeWidth = 12.dp,
                            color = when (healthScoreDetail?.rating) {
                                HealthRating.EXCELLENT -> morphism.successColor
                                HealthRating.GOOD -> morphism.primaryColor
                                HealthRating.FAIR -> morphism.warningColor
                                else -> morphism.dangerColor
                            },
                            showText = false
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(healthScoreDetail?.total ?: 0).coerceAtLeast(0)}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = morphism.textPrimary
                            )
                            Text(
                                healthScoreDetail?.rating?.label ?: "N/A",
                                style = MaterialTheme.typography.labelLarge,
                                color = morphism.textMuted
                            )
                        }
                    }
                }
            }
        }

        // ====== Income vs Expense Chart ======
        item {
            SectionHeader(title = "Cash Flow (This Month)")
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                IncomeVsExpenseChart(income = income, expenses = expenses)
            }
        }

        // ====== Category Breakdown Chart ======
        if (categoryTotals.isNotEmpty()) {
            item { SectionHeader(title = "Spending by Category") }
            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CategoryDonutChart(
                            categoryTotals = categoryTotals,
                            colors = categoryColorMap
                        )
                    }
                }
            }
        }

        // ====== Score Breakdown ======
        healthScoreDetail?.let { score ->
            item { SectionHeader(title = "Score Breakdown") }
            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.m)
                    ) {
                        ScoreBreakdownItem("Daily Income", score.incomeScore, color = morphism.primaryColor)
                        ScoreBreakdownItem("Spending Ratio", score.spendingScore, color = morphism.successColor)
                        ScoreBreakdownItem("Debt Exposure", score.debtScore, color = morphism.dangerColor)
                        ScoreBreakdownItem("Budget Adherence", score.budgetScore, color = morphism.warningColor)
                        ScoreBreakdownItem("Savings Consistency", score.savingsScore, color = morphism.primaryColor)
                    }
                }
            }
        }

        // ====== Improvement Tips ======
        if (tips.isNotEmpty()) {
            item { SectionHeader(title = "How to Improve") }
            items(tips) { tip ->
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when(tip.priority) {
                                        com.axis.app.domain.scoring.TipPriority.HIGH -> morphism.dangerColor.copy(alpha = 0.1f)
                                        else -> morphism.warningColor.copy(alpha = 0.1f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (tip.priority == com.axis.app.domain.scoring.TipPriority.HIGH) Icons.Filled.PriorityHigh else Icons.Filled.TipsAndUpdates,
                                contentDescription = null,
                                tint = when(tip.priority) {
                                    com.axis.app.domain.scoring.TipPriority.HIGH -> morphism.dangerColor
                                    else -> morphism.warningColor
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                tip.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = morphism.textPrimary
                            )
                            Text(
                                tip.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = LocalMorphismConfig.current.primaryColor
) {
    val morphism = LocalMorphismConfig.current
    val spacing = LocalSpacing.current
    NeuCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(spacing.s))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = morphism.textPrimary)
            }
        }
    }
}
