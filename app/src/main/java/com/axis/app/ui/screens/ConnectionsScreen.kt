package com.axis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axis.app.data.model.AccountEntity
import com.axis.app.data.model.AccountType
import com.axis.app.ui.BalanceMode
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val accounts by viewModel.allAccountEntities.collectAsState()
    val balanceMode by viewModel.balanceMode.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Required system accounts
    val systemAccounts = listOf("M-Pesa", "M-Shwari", "Zidii", "Till") + 
            if (balanceMode == BalanceMode.BUSINESS) listOf("Pochi") else emptyList()

    // Ensure system accounts exist in the list (if not detected yet)
    val displayAccounts = remember(accounts, systemAccounts) {
        val list = accounts.toMutableList()
        systemAccounts.forEach { sys ->
            if (list.none { it.name == sys }) {
                list.add(
                    AccountEntity(
                        name = sys,
                        type = when (sys) {
                            "M-Pesa" -> AccountType.WALLET
                            "M-Shwari", "Zidii" -> AccountType.SAVINGS
                            else -> AccountType.BUSINESS
                        },
                        isEnabled = true,
                        balance = 0.0,
                        lastSyncedAt = 0L
                    )
                )
            }
        }
        list.sortedBy { it.name == "M-Pesa" }.reversed() // M-Pesa first
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = morphism.textPrimary
                    )
                }
                Text(
                    "Connected Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = morphism.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = morphism.primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom")
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
                    "Manage where Axis tracks your money. Enable accounts to automatically sync balances from SMS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = morphism.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(displayAccounts) { account ->
                AccountItem(
                    account = account,
                    onToggle = { isEnabled ->
                        if (account.name != "M-Pesa") {
                            viewModel.updateAccount(account.copy(isEnabled = isEnabled))
                        }
                    },
                    morphism = morphism
                )
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type ->
                viewModel.addCustomAccount(name, type)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AccountItem(
    account: AccountEntity,
    onToggle: (Boolean) -> Unit,
    morphism: com.axis.app.ui.theme.MorphismConfig
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    NeuCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.l)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .neuShadow(cornerRadius = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (account.type) {
                        AccountType.SAVINGS -> Icons.Default.Savings
                        AccountType.BUSINESS -> Icons.Default.BusinessCenter
                        AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                        AccountType.CUSTOM -> Icons.Default.Wallet
                    },
                    contentDescription = null,
                    tint = if (account.isEnabled) morphism.primaryColor else morphism.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = morphism.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (account.isEnabled) "Ksh ${String.format("%.2f", account.balance)}" else "Tracking Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (account.isEnabled) morphism.textPrimary else morphism.textMuted
                )
                if (account.lastSyncedAt > 0) {
                    Text(
                        "Last synced: ${dateFormatter.format(Date(account.lastSyncedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                }
            }

            Switch(
                checked = account.isEnabled,
                onCheckedChange = onToggle,
                enabled = account.name != "M-Pesa", // M-Pesa cannot be disabled
                colors = SwitchDefaults.colors(
                    checkedThumbColor = morphism.primaryColor,
                    checkedTrackColor = morphism.primaryColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CUSTOM) }
    val morphism = LocalMorphismConfig.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedType) }
            ) {
                Text("Add", color = morphism.primaryColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = morphism.textMuted)
            }
        },
        title = { Text("Add Custom Account", color = morphism.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NeuTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Account Name (e.g. Cooperative)",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Account Type", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.values().filter { it != AccountType.WALLET }.forEach { type ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedType == type) morphism.primaryColor else morphism.neuShadow.surfaceColor)
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                type.name,
                                color = if (selectedType == type) Color.White else morphism.textPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        containerColor = morphism.neuShadow.backgroundColor
    )
}
