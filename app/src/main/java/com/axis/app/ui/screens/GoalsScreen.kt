package com.axis.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.axis.app.data.model.SavingsAccount
import com.axis.app.data.model.SavingsGoal
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.*
import com.axis.app.ui.theme.LocalMorphismConfig
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val accounts by viewModel.savingsAccounts.collectAsState()
    val morphism = LocalMorphismConfig.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }
    
    var showAddGoal by remember { mutableStateOf(false) }

    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l, vertical = spacing.s)
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = morphism.textPrimary
                    )
                }
                Text(
                    "Savings & Goals",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                    color = morphism.textPrimary
                )
                IconButton(
                    onClick = { showAddGoal = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = morphism.primaryColor)
                }
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
            // Logical Allocation Header
            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "Total Detected Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = morphism.textMuted
                        )
                        val totalSavings = accounts.sumOf { it.balance }
                        Text(
                            "Ksh ${formatter.format(totalSavings)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = morphism.textPrimary
                        )
                        
                        if (accounts.isEmpty()) {
                            Text(
                                "No savings accounts detected. Import M-Shwari or Zidii messages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.warningColor,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Spacer(Modifier.height(spacing.m))
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                                accounts.forEach { acc ->
                                    NeuChip(
                                        text = "${acc.name}: Ksh ${formatter.format(acc.balance)}",
                                        selected = true,
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Your Goals") }

            if (goals.isEmpty()) {
                item {
                    Text(
                        "You haven't set any goals yet. Logical allocation helps you partition your savings without moving money.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = morphism.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }

            items(goals) { goal ->
                val progress = viewModel.calculateGoalProgress(goal)
                val percentage = (progress / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
                
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    goal.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = morphism.textPrimary
                                )
                                val accountName = accounts.find { it.id == goal.savingsAccountId }?.name ?: "Manual"
                                Text(
                                    "Linked to: $accountName (${(goal.allocationPercentage ?: 0.0) * 100}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = morphism.textMuted
                                )
                            }
                            Text(
                                "${(percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = morphism.primaryColor
                            )
                        }
                        
                        Spacer(Modifier.height(LocalSpacing.current.m))
                        
                        NeuProgressBar(
                            progress = percentage
                        )
                        
                        Spacer(Modifier.height(LocalSpacing.current.s))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Ksh ${formatter.format(progress)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textPrimary
                            )
                            Text(
                                "Target: Ksh ${formatter.format(goal.targetAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(
            accounts = accounts,
            onDismiss = { showAddGoal = false },
            onConfirm = { name, target, accountId, percentage ->
                viewModel.viewModelScope.launch {
                    viewModel.upsertGoal(
                        SavingsGoal(
                            name = name,
                            targetAmount = target,
                            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // Default 30 days
                            savingsAccountId = accountId,
                            allocationPercentage = percentage / 100.0
                        )
                    )
                    showAddGoal = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    accounts: List<SavingsAccount>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long?, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var percentage by remember { mutableStateOf(50f) }
    val morphism = LocalMorphismConfig.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val targetValue = target.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && targetValue > 0) {
                        onConfirm(name, targetValue, selectedAccountId, percentage.toDouble())
                    }
                }
            ) {
                Text("Create", color = morphism.primaryColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = morphism.textMuted)
            }
        },
        title = { Text("New Logical Goal", color = morphism.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NeuTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Goal Name (e.g., Wedding)",
                    modifier = Modifier.fillMaxWidth()
                )
                NeuTextField(
                    value = target,
                    onValueChange = { target = it },
                    placeholder = "Target Amount (Ksh)",
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (accounts.isNotEmpty()) {
                    Text("Select Savings Account", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { acc ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedAccountId == acc.id) morphism.primaryColor else morphism.neuShadow.surfaceColor)
                                    .clickable { selectedAccountId = acc.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    acc.name,
                                    color = if (selectedAccountId == acc.id) Color.White else morphism.textPrimary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    Text("Allocation: ${percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                    Slider(
                        value = percentage,
                        onValueChange = { percentage = it },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = morphism.primaryColor, activeTrackColor = morphism.primaryColor)
                    )
                } else {
                    Text("No savings accounts detected. Goal will use manual tracking.", color = morphism.dangerColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        containerColor = morphism.neuShadow.backgroundColor
    )
}
