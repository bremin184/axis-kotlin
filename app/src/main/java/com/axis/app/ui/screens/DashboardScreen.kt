package com.axis.app.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.ui.theme.*
import com.axis.app.data.model.Transaction
import com.axis.app.domain.budget.BudgetOverview
import com.axis.app.domain.classifier.CategoryClassifier
import com.axis.app.ui.BalanceMode
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.navigation.Screen
import com.axis.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit,
    onImportClick: () -> Unit,
) {
    val transactions by viewModel.transactions.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val balanceMode by viewModel.balanceMode.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()
    val expenses by viewModel.monthlyExpenses.collectAsState()
    val budgetOverview by viewModel.budgetOverview.collectAsState()
    val healthScoreDetail by viewModel.healthScoreDetail.collectAsState()
    val budgetProgress by viewModel.budgetProgress.collectAsState()
    val healthScoreFactor by viewModel.healthScoreFactor.collectAsState()
    val savingsAccounts by viewModel.savingsAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val morphism = LocalMorphismConfig.current

    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.US) }
    val insights = remember(transactions, budgetOverview, income, expenses) {
        if (transactions.isNotEmpty()) {
            buildInsights(transactions, budgetOverview, income, expenses, formatter, morphism)
        } else {
            emptyList()
        }
    }

    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(morphism.neuShadow.backgroundColor),
        contentPadding = PaddingValues(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l)
    ) {
        // ====== Greeting Header ======
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        hour < 12 -> "Good Morning"
                        hour < 17 -> "Good Afternoon"
                        else -> "Good Evening"
                    }
                    Text(
                        greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        color = morphism.textPrimary
                    )
                    Text(
                        "Your Financial Intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = morphism.textMuted
                    )
                }
                Switch(
                    checked = balanceMode == BalanceMode.PERSONAL,
                    onCheckedChange = { 
                        val newMode = if (it) BalanceMode.PERSONAL else BalanceMode.BUSINESS
                        viewModel.setBalanceMode(newMode) 
                    }
                )
            }
        }

        // ====== Balance Card (Gradient NeuCard) ======
        item {
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.l)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (balanceMode == BalanceMode.PERSONAL) "M-Pesa Balance" else "Business Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = morphism.textPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            "Ksh ${formatter.format(balance)}",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            ),
                            color = morphism.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BalanceStatChip("Income", "+Ksh ${formatter.format(income)}", morphism.successColor)
                        BalanceStatChip("Expenses", "-Ksh ${formatter.format(expenses)}", morphism.dangerColor)
                        
                        val totalSavings = savingsAccounts.sumOf { it.balance }
                        BalanceStatChip("Savings", "Ksh ${formatter.format(totalSavings)}", morphism.primaryColor)
                    }

                    if (savingsAccounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savingsAccounts.forEach { acc ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(morphism.primaryColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "${acc.name}: Ksh ${formatter.format(acc.balance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = morphism.primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====== Action Buttons ======
        item {
            ActionButtons(onImportClick = onImportClick, onNavigate = onNavigate)
        }

        // ====== Budget Ring Section ======
        item {
            val budgetColor = when {
                budgetProgress < 0.7f -> morphism.successColor
                budgetProgress < 0.9f -> morphism.warningColor
                else -> morphism.dangerColor
            }
            NeuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Screen.Budget.route) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeuCircularProgress(
                        percentage = budgetProgress,
                        size = 80.dp,
                        strokeWidth = 10.dp,
                        color = budgetColor
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            "Monthly Budget",
                            style = MaterialTheme.typography.titleMedium,
                            color = morphism.textPrimary
                        )
                        val overview = budgetOverview
                        if (overview != null) {
                            Text(
                                "Ksh ${formatter.format(overview.totalSpent)} of ${formatter.format(overview.totalBudget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                            val overspent = overview.categories.count { it.alertLevel == com.axis.app.domain.budget.BudgetAlertLevel.EXCEEDED }
                            if (overspent > 0) {
                                Text(
                                    "$overspent categories overspent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = morphism.dangerColor
                                )
                            }
                        } else {
                            Text(
                                "Ksh 0 of Ksh 0",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }
                    }
                }
            }
        }

        // ====== Health Score Quick Glance ======
        healthScoreDetail?.let { score ->
            item {
                val healthColor = when {
                    healthScoreFactor > 0.75f -> morphism.successColor
                    healthScoreFactor > 0.45f -> morphism.warningColor
                    else -> morphism.dangerColor
                }
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.Analytics.route) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeuCircularProgress(
                            percentage = healthScoreFactor,
                            size = 56.dp,
                            strokeWidth = 6.dp,
                            color = healthColor
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Financial Health",
                                style = MaterialTheme.typography.titleSmall,
                                color = morphism.textPrimary
                            )
                            Text(
                                score.rating.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "View details",
                            tint = morphism.textMuted
                        )
                    }
                }
            }
        }

        // ====== Insights ======
        if (insights.isNotEmpty()) {
            item {
                SectionHeader(title = "Insights")
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                    contentPadding = PaddingValues(horizontal = spacing.xs)
                ) {
                    items(insights) { insight ->
                        NeuCard(
                            modifier = Modifier
                                .width(280.dp)
                                .then(
                                    if (insight.route != null) {
                                        Modifier.clickable { onNavigate(insight.route) }
                                    } else Modifier
                                ),
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(spacing.s)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(insight.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            insight.icon,
                                            contentDescription = null,
                                            tint = insight.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(spacing.s))
                                    Text(
                                        insight.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = morphism.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    insight.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = morphism.textSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====== Top Spending Categories ======
        if (transactions.isNotEmpty()) {
            item { SectionHeader(title = "Top Spending") }

            val topCategories = transactions
                .filter { !it.isIncome }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(4)

            items(topCategories.toList()) { (category, total) ->
                val meta = CategoryClassifier.CATEGORY_META[category]
                val catColor = try {
                    Color(android.graphics.Color.parseColor(meta?.color ?: "#868E96"))
                } catch (_: Exception) { morphism.textMuted }

                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.Analytics.route) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Category,
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                meta?.label ?: category,
                                style = MaterialTheme.typography.titleSmall,
                                color = morphism.textPrimary
                            )
                        }
                        Text(
                            "Ksh ${formatter.format(total)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = morphism.dangerColor
                        )
                    }
                }
            }
        }

        // ====== Recent Transactions ======
        if (transactions.isNotEmpty()) {
            item { SectionHeader(title = "Recent Transactions") }

            val recent = transactions.take(5)
            items(recent) { tx ->
                TransactionRow(tx, formatter, timeFormat, dateFormat)
            }
        }

        // ====== Empty State ======
        if (transactions.isEmpty() && !isLoading) {
            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = morphism.primaryColor
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Import your M-Pesa SMS",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = morphism.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap the + button to import transactions and get personalized financial insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = morphism.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom spacing
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ActionButtons(
    onNavigate: (String) -> Unit,
    onImportClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.m)
    ) {
        NeuButton(
            onClick = { onNavigate(Screen.Send.route) },
            modifier = Modifier.weight(1f),
            text = "Send"
        )
        NeuButton(
            onClick = { onNavigate(Screen.Receive.route) },
            modifier = Modifier.weight(1f),
            text = "Receive"
        )
        NeuButton(
            onClick = onImportClick,
            modifier = Modifier.weight(1f),
            text = "Import"
        )
    }
}
// ========== Helper Composables ==========

@Composable
private fun BalanceStatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalMorphismConfig.current.textPrimary.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    formatter: NumberFormat,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat
) {
    val morphism = LocalMorphismConfig.current
    val meta = CategoryClassifier.CATEGORY_META[tx.category]
    val catColor = try {
        Color(android.graphics.Color.parseColor(meta?.color ?: "#868E96"))
    } catch (_: Exception) { morphism.textMuted }

    NeuCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (tx.isIncome) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = if (tx.isIncome) morphism.successColor else catColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.recipient ?: tx.type,
                    style = MaterialTheme.typography.titleSmall,
                    color = morphism.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        meta?.label ?: tx.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor
                    )
                    Text("·", color = morphism.textMuted, style = MaterialTheme.typography.labelSmall)
                    Text(
                        timeFormat.format(Date(tx.transactionDateTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (tx.isIncome) "+" else "-"}Ksh ${formatter.format(tx.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (tx.isIncome) morphism.successColor else morphism.dangerColor
                )
                if (tx.transactionCost > 0) {
                    Text(
                        "Fee: ${formatter.format(tx.transactionCost)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                }
            }
        }
    }
}

private fun buildInsights(
    transactions: List<Transaction>,
    budgetOverview: BudgetOverview?,
    income: Double,
    expenses: Double,
    formatter: NumberFormat,
    morphism: MorphismConfig
): List<InsightItem> {
    val insights = mutableListOf<InsightItem>()

    // Savings rate insight
    if (income > 0) {
        val savingsRate = com.axis.app.domain.scoring.FinancialHealthCalculator.calculateSavingsPercent(income, expenses).toInt()
        insights.add(
            InsightItem(
                title = "Savings Rate",
                message = if (savingsRate >= 20) "Great! You're saving ${savingsRate}% of income."
                else "You're only saving ${savingsRate}%. Aim for 20%+ each month.",
                icon = if (savingsRate >= 20) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.Warning,
                color = if (savingsRate >= 20) morphism.successColor else morphism.warningColor
            )
        )
    }

    // Overspent categories
    budgetOverview?.categories?.filter { it.alertLevel == com.axis.app.domain.budget.BudgetAlertLevel.EXCEEDED }?.let { overspent ->
        if (overspent.isNotEmpty()) {
            val worstCategory = overspent.maxByOrNull { item -> item.spent - item.limit }
            insights.add(
                InsightItem(
                    title = "Budget Alert",
                    message = "${overspent.size} budget(s) exceeded. ${worstCategory?.category?.replace('_', ' ')} is Ksh ${formatter.format(worstCategory?.let { it.spent - it.limit } ?: 0.0)} over.",
                    icon = Icons.Filled.Warning,
                    color = morphism.dangerColor
                )
            )
        }
    }

    // Top merchant
    val topMerchant = transactions.filter { !it.isIncome && it.recipient != null }
        .groupBy { it.recipient!! }
        .maxByOrNull { (_, txs) -> txs.size }

    topMerchant?.let { (merchant, txs) ->
        insights.add(
            InsightItem(
                title = "Frequent Spender",
                message = "You've transacted ${txs.size} times with $merchant totaling Ksh ${formatter.format(txs.sumOf { it.amount })}.",
                icon = Icons.Filled.Repeat,
                color = morphism.primaryColor,
                route = Screen.FrequentTransactions.route
            )
        )
    }

    return insights
}

private data class InsightItem(
    val title: String,
    val message: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val route: String? = null
)
