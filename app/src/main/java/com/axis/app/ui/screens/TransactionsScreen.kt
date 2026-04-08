package com.axis.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axis.app.data.model.Transaction
import com.axis.app.domain.classifier.CategoryClassifier
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val morphism = LocalMorphismConfig.current
    
    var selectedTxForCategory by remember { mutableStateOf<Transaction?>(null) }

    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }

    // Filter transactions
    val filtered = remember(transactions, searchQuery, selectedCategory) {
        transactions.filter { tx ->
            val matchesSearch = searchQuery.isBlank() ||
                    tx.recipient?.contains(searchQuery, ignoreCase = true) == true ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.type.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || tx.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    // Group by date
    val grouped = remember(filtered) {
        val now = Calendar.getInstance()
        val today = now.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0)
        val todayMs = today.timeInMillis

        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayMs = yesterday.timeInMillis

        val weekStart = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        val weekMs = weekStart.timeInMillis

        filtered.groupBy { tx ->
            when {
                tx.transactionDateTime >= todayMs -> "Today"
                tx.transactionDateTime >= yesterdayMs -> "Yesterday"
                tx.transactionDateTime >= weekMs -> "This Week"
                else -> dateFormat.format(Date(tx.transactionDateTime))
            }
        }
    }

    // All unique categories for filter chips
    val categories = remember(transactions) {
        transactions.map { it.category }.distinct().sorted()
    }

    val spacing = LocalSpacing.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(morphism.neuShadow.backgroundColor),
            contentPadding = PaddingValues(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.m)
        ) {
            // Search bar
            item {
                NeuTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = "Search transactions...",
                    leadingIcon = {
                        Icon(Icons.Filled.Search, "Search", tint = morphism.textMuted, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        NeuChip(
                            text = "All",
                            selected = selectedCategory == null,
                            onClick = { viewModel.updateCategoryFilter(null) }
                        )
                    }
                    items(categories) { cat ->
                        val meta = CategoryClassifier.CATEGORY_META[cat]
                        NeuChip(
                            text = meta?.label ?: cat,
                            selected = selectedCategory == cat,
                            onClick = { viewModel.updateCategoryFilter(if (selectedCategory == cat) null else cat) }
                        )
                    }
                }
            }

            // Transaction count
            item {
                Text(
                    "${filtered.size} transactions",
                    style = MaterialTheme.typography.labelMedium,
                    color = morphism.textMuted
                )
            }

            // Grouped transactions
            grouped.forEach { (dateLabel, txs) ->
                item {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = morphism.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(txs, key = { it.transactionCode }) { tx ->
                    TransactionItem(
                        tx = tx, 
                        formatter = formatter, 
                        timeFormat = timeFormat,
                        onLongClick = { selectedTxForCategory = tx }
                    )
                }
            }

            // Empty state
            if (filtered.isEmpty()) {
                item {
                    NeuCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(48.dp), tint = morphism.textMuted)
                            Spacer(Modifier.height(spacing.s))
                            Text(
                                if (searchQuery.isNotBlank()) "No matching transactions" else "No transactions yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = morphism.textMuted
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        // Recategorization Sheet
        selectedTxForCategory?.let { tx ->
            RecategorizationSheet(
                transaction = tx,
                onCategorize = { newCat, _ ->
                    viewModel.categorizeTransaction(tx.id, newCat, tx.recipient, tx.rawMessage)
                    selectedTxForCategory = null
                },
                onDismiss = { selectedTxForCategory = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    tx: Transaction,
    formatter: NumberFormat,
    timeFormat: SimpleDateFormat,
    onLongClick: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val meta = CategoryClassifier.CATEGORY_META[tx.category]
    val catColor = try {
        Color(android.graphics.Color.parseColor(meta?.color ?: "#868E96"))
    } catch (_: Exception) { morphism.textMuted }

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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
