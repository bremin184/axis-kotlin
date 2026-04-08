package com.axis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axis.app.domain.budget.BudgetAlertLevel
import com.axis.app.domain.budget.CategoryBudgetStatus
import com.axis.app.domain.classifier.CategoryClassifier
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.axis.app.data.model.Budget
import com.axis.app.data.model.CategoryType
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun BudgetScreen(viewModel: MainViewModel) {
    val budgetOverview by viewModel.budgetOverview.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()
    val morphism = LocalMorphismConfig.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }
    val suggestions = remember(budgetOverview, income) {
        budgetOverview?.let { viewModel.budgetEngine.generateSuggestions(it, income) } ?: emptyList()
    }

    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(morphism.neuShadow.backgroundColor),
        contentPadding = PaddingValues(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l)
    ) {
        // Header
        item {
            var showAddBudgetDialog by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Budget",
                    style = MaterialTheme.typography.headlineMedium,
                    color = morphism.textPrimary
                )
                NeuButton(
                    onClick = { showAddBudgetDialog = true },
                    text = "Add",
                    modifier = Modifier.width(80.dp)
                )
            }

            if (showAddBudgetDialog) {
                AddBudgetDialog(
                    onDismiss = { showAddBudgetDialog = false },
                    onConfirm = { name, limit ->
                        viewModel.addCustomCategory(name, CategoryType.EXPENSE_ONLY)
                        viewModel.updateBudgetLimit(name, limit)
                        showAddBudgetDialog = false
                    }
                )
            }
        }

        // Overall Budget Ring
        budgetOverview?.let { overview ->
            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeuCircularProgress(
                            percentage = overview.overallPercentage,
                            size = 100.dp,
                            strokeWidth = 10.dp,
                            showText = false
                        )
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(
                                "Overall",
                                style = MaterialTheme.typography.titleMedium,
                                color = morphism.textPrimary
                            )
                            Text(
                                "Ksh ${formatter.format(overview.totalSpent)} spent",
                                style = MaterialTheme.typography.bodyMedium,
                                color = morphism.textSecondary
                            )
                            Text(
                                "of Ksh ${formatter.format(overview.totalBudget)} total",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }
                    }
                }
            }

            // Income vs Expense summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.m)
                ) {
                    NeuCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Income", style = MaterialTheme.typography.labelMedium, color = morphism.textMuted)
                            Spacer(Modifier.height(spacing.xs))
                            Text(
                                "Ksh ${formatter.format(income)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = morphism.successColor
                            )
                        }
                    }
                    NeuCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Expenses", style = MaterialTheme.typography.labelMedium, color = morphism.textMuted)
                            Spacer(Modifier.height(spacing.xs))
                            Text(
                                "Ksh ${formatter.format(overview.totalSpent)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = morphism.dangerColor
                            )
                        }
                    }
                }
            }

            // Category Budgets
            item { SectionHeader(title = "Category Budgets") }

            items(overview.categories) { status ->
                CategoryBudgetCard(
                    status = status,
                    formatter = formatter,
                    onLimitChange = { newLimit ->
                        viewModel.updateBudgetLimit(status.category, newLimit)
                    }
                )
            }

            // AI Suggestions
            if (suggestions.isNotEmpty()) {
                item { SectionHeader(title = "AI Suggestions") }
                items(suggestions) { suggestion ->
                    NeuCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                when (suggestion.type) {
                                    com.axis.app.domain.budget.SuggestionType.REALLOCATION -> Icons.Filled.SwapHoriz
                                    com.axis.app.domain.budget.SuggestionType.WARNING -> Icons.Filled.Warning
                                    com.axis.app.domain.budget.SuggestionType.SAVINGS_RATE -> Icons.AutoMirrored.Filled.TrendingUp
                                },
                                contentDescription = null,
                                tint = when (suggestion.type) {
                                    com.axis.app.domain.budget.SuggestionType.REALLOCATION -> morphism.primaryColor
                                    com.axis.app.domain.budget.SuggestionType.WARNING -> morphism.warningColor
                                    com.axis.app.domain.budget.SuggestionType.SAVINGS_RATE -> morphism.successColor
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                suggestion.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Empty state
        if (budgetOverview == null) {
            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.PieChart, null, modifier = Modifier.size(48.dp), tint = morphism.textMuted)
                        Spacer(Modifier.height(spacing.s))
                        Text("Import transactions to view budgets", color = morphism.textMuted)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CategoryBudgetCard(
    status: CategoryBudgetStatus,
    formatter: NumberFormat,
    onLimitChange: (Double) -> Unit
) {
    val meta = CategoryClassifier.CATEGORY_META[status.category]
    val morphism = LocalMorphismConfig.current
    val spacing = LocalSpacing.current
    val catColor = try {
        Color(android.graphics.Color.parseColor(meta?.color ?: "#868E96"))
    } catch (_: Exception) { morphism.textMuted }

    var isEditing by remember { mutableStateOf(false) }
    
    // Limits State
    var limitAmount by remember(status.limit) { mutableFloatStateOf(status.limit.toFloat()) }
    var limitText by remember(status.limit) { mutableStateOf(status.limit.toInt().toString()) }

    val alertColor = when (status.alertLevel) {
        BudgetAlertLevel.EXCEEDED -> morphism.dangerColor
        BudgetAlertLevel.WARNING -> morphism.warningColor
        BudgetAlertLevel.WATCH -> morphism.primaryColor
        BudgetAlertLevel.ON_TRACK -> morphism.successColor
    }

    NeuCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Category, null, tint = catColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(spacing.s))
                    Column {
                        Text(
                            meta?.label ?: status.category,
                            style = MaterialTheme.typography.titleSmall,
                            color = morphism.textPrimary
                        )
                        Text(
                            "Ksh ${formatter.format(status.spent)} spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = morphism.textMuted
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${status.percentage}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = alertColor
                    )
                    Text(
                        "of Ksh ${formatter.format(status.limit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                }
            }

            // Progress Bar
            NeuProgressBar(
                progress = (status.percentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )

            // Edit Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { isEditing = !isEditing },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isEditing) "Cancel" else "Edit Limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.primaryColor
                    )
                }
            }

            // Editing Section
            if (isEditing) {
                val dynamicMax = max(50000f, status.limit.toFloat() * 1.5f)
                
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                    Text("Monthly Limit", style = MaterialTheme.typography.labelMedium, color = morphism.textPrimary)
                    
                    Slider(
                        value = limitAmount,
                        onValueChange = { 
                            limitAmount = it
                            limitText = it.toInt().toString()
                        },
                        valueRange = 0f..dynamicMax,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = catColor, activeTrackColor = catColor)
                    )

                    NeuTextField(
                        value = limitText,
                        onValueChange = { newLimit: String ->
                            limitText = newLimit
                            newLimit.toDoubleOrNull()?.let { amt ->
                                limitAmount = amt.toFloat()
                            }
                        },
                        placeholder = "Amount",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(spacing.xs))

                    NeuButton(
                        onClick = {
                            onLimitChange(limitAmount.toDouble())
                            isEditing = false
                        },
                        text = "Save Limit",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    val spacing = LocalSpacing.current
    val morphism = LocalMorphismConfig.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Budget Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                NeuTextField(
                    value = name,
                    onValueChange = { newName: String -> name = newName },
                    placeholder = "Category Name (e.g. Rent, Gift)"
                )
                NeuTextField(
                    value = limitText,
                    onValueChange = { newLimit: String -> limitText = newLimit },
                    placeholder = "Monthly Limit"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, limit)
                    }
                }
            ) {
                Text("Add", color = morphism.primaryColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = morphism.textMuted)
            }
        }
    )
}
