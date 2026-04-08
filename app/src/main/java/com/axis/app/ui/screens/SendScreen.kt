package com.axis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.domain.utils.DeepLinkLauncher
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val budgetOverview by viewModel.budgetOverview.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var phone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "KE")) }

    // Recent contacts logic: extract from transactions where fundType is M-Pesa Send
    val recentContacts = remember(transactions) {
        transactions.filter { !it.isIncome && it.recipient != null }
            .map { it.recipient!! to (it.recipientPhone ?: "") }
            .distinctBy { it.first }
            .take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Money", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = morphism.neuShadow.backgroundColor,
                    titleContentColor = morphism.textPrimary
                )
            )
        },
        containerColor = morphism.neuShadow.backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.l)
        ) {
            item {
                Text(
                    "Recipient Information",
                    style = MaterialTheme.typography.titleSmall,
                    color = morphism.textMuted
                )
            }

            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                        NeuTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = "Enter Phone Number",
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = morphism.textMuted) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        NeuTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            placeholder = "Enter Amount (KES)",
                            leadingIcon = { Icon(Icons.Default.Payments, null, tint = morphism.textMuted) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Intelligence Layer: Budget Impact
            if (amount > 0 && budgetOverview != null) {
                val overview = budgetOverview!!
                val projectedTotal = overview.totalSpent + amount
                val isGoingOver = projectedTotal > overview.totalBudget && overview.totalBudget > 0

                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (isGoingOver) morphism.dangerColor.copy(alpha = 0.1f) else morphism.successColor.copy(alpha = 0.1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isGoingOver) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isGoingOver) morphism.dangerColor else morphism.successColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (isGoingOver) "Budget Alert" else "Within Budget",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isGoingOver) morphism.dangerColor else morphism.successColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isGoingOver) {
                                        "Sending Ksh ${formatter.format(amount)} will exceed your monthly budget by Ksh ${formatter.format(projectedTotal - overview.totalBudget)}."
                                    } else {
                                        "You'll have Ksh ${formatter.format(overview.totalBudget - projectedTotal)} remaining in your budget."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = morphism.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Recent Contacts
            if (recentContacts.isNotEmpty()) {
                item {
                    Text(
                        "Recent Recipients",
                        style = MaterialTheme.typography.titleSmall,
                        color = morphism.textMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(recentContacts) { (name, phoneNum) ->
                    NeuCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                phone = phoneNum.ifBlank { name }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(morphism.primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name.take(1).uppercase(),
                                    color = morphism.primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyMedium, color = morphism.textPrimary)
                                if (phoneNum.isNotBlank()) {
                                    Text(phoneNum, style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                NeuButton(
                    onClick = {
                        if (phone.isNotBlank() && amount > 0) {
                            DeepLinkLauncher.launchSend(context, phone, amount)
                        } else {
                            Toast.makeText(context, "Please enter valid recipient and amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    text = "Launch M-Pesa",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
