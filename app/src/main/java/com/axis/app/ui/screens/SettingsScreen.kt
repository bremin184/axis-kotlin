package com.axis.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.navigation.Screen
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val userName by viewModel.userName.collectAsState()
    val balanceMode by viewModel.balanceMode.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSecurityEnabled by viewModel.isSecurityEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val currentCurrency by viewModel.currency.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    val encPin by viewModel.encPin.collectAsState()

    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.l),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = morphism.textPrimary,
                    fontWeight = FontWeight.Bold
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
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            // 1️⃣ PROFILE SECTION
            item {
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.ProfileDetails.route) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.l)
                    ) {
                        // Avatar Placeholder
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .neuShadow(cornerRadius = 30.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = morphism.primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (userName.isBlank()) "Set your name" else userName,
                                style = MaterialTheme.typography.titleMedium,
                                color = morphism.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    balanceMode.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = morphism.primaryColor
                                )
                                Text(
                                    "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = morphism.textMuted
                                )
                                Text(
                                    "Active Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = morphism.successColor
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${transactions.size} Transactions cached",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = morphism.textMuted
                        )
                    }
                }
            }

            // 2️⃣ CONNECTIVITY CARD
            item {
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.Connections.route) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.l)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .neuShadow(cornerRadius = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = morphism.primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Text(
                            "Connected Accounts",
                            style = MaterialTheme.typography.bodyLarge,
                            color = morphism.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = morphism.textMuted
                        )
                    }
                }
            }

            // 3️⃣ SECURITY SECTION
            item { SectionHeader(title = "Security") }

            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = morphism.primaryColor)
                            Text("Fast PIN Login", color = morphism.textPrimary)
                        }
                        Switch(
                            checked = isSecurityEnabled,
                            onCheckedChange = { 
                                if (it && encPin.isEmpty()) {
                                    showPinSetup = true
                                } else {
                                    viewModel.setSecurityEnabled(it)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = morphism.primaryColor,
                                checkedTrackColor = morphism.primaryColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = morphism.primaryColor)
                            Text("Biometric Unlock", color = morphism.textPrimary)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = morphism.primaryColor,
                                checkedTrackColor = morphism.primaryColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // More settings can go here (Preferences, Security, etc.)
            item { SectionHeader(title = "App Preferences") }

            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                             Icon(
                                if(isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = morphism.primaryColor
                            )
                            Text("Dark Mode", color = morphism.textPrimary)
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = morphism.primaryColor,
                                checkedTrackColor = morphism.primaryColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            item {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = morphism.primaryColor)
                            Text("Default Currency", color = morphism.textPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Ksh", "USD").forEach { symbol ->
                                NeuChip(
                                    text = symbol,
                                    selected = currentCurrency == symbol,
                                    onClick = { viewModel.updateCurrency(symbol) }
                                )
                            }
                        }
                    }
                }
            }

            // 4️⃣ DATA & REPORTS SECTION
            item { SectionHeader(title = "Data & Reports") }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.exportFinancialSummary(context) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.s)
                        ) {
                            Icon(Icons.Default.Description, "CSV", tint = morphism.primaryColor)
                            Text("Export CSV", style = MaterialTheme.typography.labelMedium, color = morphism.textPrimary)
                            Text("Summary Report", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                        }
                    }

                    NeuCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.exportProfessionalAnalysis(context) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.s)
                        ) {
                            Icon(Icons.Default.Analytics, "PDF", tint = morphism.primaryColor)
                            Text("Export PDF", style = MaterialTheme.typography.labelMedium, color = morphism.textPrimary)
                            Text("Pro Analysis", style = MaterialTheme.typography.labelSmall, color = morphism.textMuted)
                        }
                    }
                }
            }

            // 5️⃣ CLEAR DATA SECTION
            item { SectionHeader(title = "Data Management") }

            item {
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.l)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .neuShadow(cornerRadius = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = morphism.dangerColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Clear Imported Data",
                                style = MaterialTheme.typography.bodyLarge,
                                color = morphism.dangerColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Wipe transactions & rules",
                                style = MaterialTheme.typography.bodySmall,
                                color = morphism.textMuted
                            )
                        }
                    }
                }
            }
            
            item {
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearAllData() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = morphism.dangerColor
                        )
                        Text(
                            "Reset Application Data",
                            color = morphism.dangerColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        var deleteInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = morphism.neuShadow.backgroundColor,
            title = {
                Text("Confirm Deletion", color = morphism.textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This will permanently delete all imported SMS transactions and categorization rules. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = morphism.textSecondary
                    )
                    Text(
                        "Type DELETE to confirm:",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                    NeuTextField(
                        value = deleteInput,
                        onValueChange = { deleteInput = it },
                        placeholder = "DELETE",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                NeuButton(
                    onClick = {
                        if (deleteInput == "DELETE") {
                            viewModel.clearImportedData()
                            showDeleteDialog = false
                        }
                    },
                    text = "Confirm Wipe",
                    modifier = Modifier.padding(8.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = morphism.textMuted)
                }
            }
        )
    }

    if (showPinSetup) {
        SecuritySetupDialog(
            onConfirm = { pin ->
                viewModel.updateSharedPin(pin)
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false }
        )
    }
}
