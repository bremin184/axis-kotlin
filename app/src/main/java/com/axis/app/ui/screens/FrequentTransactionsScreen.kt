package com.axis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.NeuCard
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequentTransactionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val morphism = LocalMorphismConfig.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }

    val frequentMerchants = remember(transactions) {
        transactions.filter { !it.isIncome && it.recipient != null }
            .groupBy { it.recipient!! }
            .mapValues { (_, txs) -> 
                Pair(txs.size, txs.sumOf { it.amount })
            }
            .entries
            .sortedByDescending { it.value.first }
            .take(5)
    }

    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l, vertical = spacing.s)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = morphism.textPrimary)
                }
                Text(
                    "Frequent Spenders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                    color = morphism.textPrimary
                )
            }
        },
        containerColor = morphism.neuShadow.backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.l)
        ) {
            item {
                Text(
                    "You interact with these merchants the most. Identifying frequent spending helps in automating your budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = morphism.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(frequentMerchants) { (merchant, stats) ->
                val (count, total) = stats
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(morphism.primaryColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = morphism.primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(spacing.l))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                merchant,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = morphism.textPrimary
                            )
                            Text(
                                "$count transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }
                        Text(
                            "Ksh ${formatter.format(total)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = morphism.dangerColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (frequentMerchants.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No frequent merchants found yet.", color = morphism.textMuted)
                    }
                }
            }
        }
    }
}
