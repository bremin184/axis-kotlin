package com.axis.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.axis.app.AxisApplication
import com.axis.app.datastore.UserSettings
import com.axis.app.ui.components.*
import com.axis.app.ui.navigation.AxisBottomBar
import com.axis.app.ui.navigation.AxisNavHost
import com.axis.app.ui.theme.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import java.util.Calendar
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val application = LocalContext.current.applicationContext as AxisApplication
            val userSettings = UserSettings(application)
            val viewModel: MainViewModel = viewModel(
                factory = ViewModelFactory(application.repository, application, userSettings)
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            AxisTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val importState by viewModel.importProgress.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var showImportSheet by remember { mutableStateOf(false) }
                val morphism = LocalMorphismConfig.current

                // SMS permission handling
                var hasSmsPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasSmsPermission = permissions[Manifest.permission.READ_SMS] == true
                    if (hasSmsPermission) {
                        viewModel.importFromSmsInbox(0L)
                    }
                }

                // Import status snackbar
                LaunchedEffect(importState) {
                    when (val state = importState) {
                        is ImportState.Success -> {
                            showImportSheet = false
                            val message = if (state.isManual) {
                                "${state.added} transactions imported (${state.duplicates} duplicates skipped)"
                            } else {
                                if (state.added > 0) "${state.added} new transactions found" else ""
                            }
                            if (message.isNotBlank()) {
                                snackbarHostState.showSnackbar(message)
                            }
                            viewModel.resetImportState()
                        }
                        is ImportState.Error -> {
                            snackbarHostState.showSnackbar(state.message)
                            viewModel.resetImportState()
                        }
                        else -> {}
                    }
                }

                val isAppLocked by viewModel.isAppLocked.collectAsState()
                val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
                val encPin by viewModel.encPin.collectAsState()

                // Security Enforcement
                LaunchedEffect(isAppLocked) {
                    if (isAppLocked && isBiometricEnabled) {
                        showBiometricPrompt(
                            onSuccess = { viewModel.setAppLocked(false) },
                            onError = { /* Stay locked, let user use PIN */ }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            AxisBottomBar(
                                navController = navController
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { showImportSheet = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Import")
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        containerColor = morphism.neuShadow.backgroundColor
                    ) { innerPadding ->
                        AxisNavHost(
                            viewModel = viewModel,
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            onImportClick = { showImportSheet = true }
                        )
                    }

                    // Security Gate Overlay
                    if (isAppLocked) {
                        SecurityGate(
                            onPinEntered = { pin ->
                                if (viewModel.unlockApp(pin)) {
                                    // Successfully unlocked (handled via StateFlow update)
                                }
                            },
                            canUseBiometric = isBiometricEnabled,
                            onBiometricClick = {
                                showBiometricPrompt(
                                    onSuccess = { viewModel.setAppLocked(false) },
                                    onError = { /* Error handled by prompt */ }
                                )
                            }
                        )
                    }

                    // Import Bottom Sheet Overlay
                    if (showImportSheet) {
                        ImportBottomSheet(
                            viewModel = viewModel,
                            hasSmsPermission = hasSmsPermission,
                            onRequestPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            },
                            importState = importState,
                            onDismiss = { showImportSheet = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBottomSheet(
    viewModel: MainViewModel,
    hasSmsPermission: Boolean,
    onRequestPermission: () -> Unit,
    importState: ImportState,
    onDismiss: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pastedText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                viewModel.importFromSmsInbox(calendar.timeInMillis)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = morphism.neuShadow.backgroundColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = morphism.textMuted.copy(alpha = 0.3f)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Import Transactions",
                style = MaterialTheme.typography.headlineSmall,
                color = morphism.textPrimary
            )

            // Loading indicator
            if (importState is ImportState.Loading) {
                NeuCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            importState.message,
                            color = morphism.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Option 1: SMS Inbox
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.PhoneAndroid, "SMS", tint = Primary, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read SMS Inbox", style = MaterialTheme.typography.titleSmall, color = morphism.textPrimary)
                        Text("Auto-import M-Pesa messages", style = MaterialTheme.typography.bodySmall, color = morphism.textMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeuButton(
                        onClick = { showDatePicker = true },
                        text = "Select Date",
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        onClick = {
                            if (hasSmsPermission) viewModel.importFromSmsInbox(0L)
                            else onRequestPermission()
                        },
                        text = if (hasSmsPermission) "Import All" else "Grant Permission",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Option 2: Paste Text
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.ContentPaste, "Paste", tint = Success, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Paste SMS Text", style = MaterialTheme.typography.titleSmall, color = morphism.textPrimary)
                        Text("Paste multiple M-Pesa messages", style = MaterialTheme.typography.bodySmall, color = morphism.textMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                NeuTextField(
                    value = pastedText,
                    onValueChange = { pastedText = it },
                    placeholder = "Paste M-Pesa SMS messages here...",
                    leadingIcon = { Icon(Icons.Filled.ContentPaste, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                )
                Spacer(Modifier.height(12.dp))
                NeuButton(
                    onClick = {
                        if (pastedText.isNotBlank()) viewModel.importFromText(pastedText)
                    },
                    text = "Import Pasted Text",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Option 3: Demo Data
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, "Demo", tint = Warning, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Load Demo Data", style = MaterialTheme.typography.titleSmall, color = morphism.textPrimary)
                        Text("20 sample transactions to try the app", style = MaterialTheme.typography.bodySmall, color = morphism.textMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                NeuButton(
                    onClick = { viewModel.importDemoData() },
                    text = "Load Demo Data",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
fun ComponentActivity.showBiometricPrompt(
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    if (this !is FragmentActivity) return

    val executor = ContextCompat.getMainExecutor(this)
    val biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Security Check")
        .setSubtitle("Authenticate to access your financial data")
        .setNegativeButtonText("Use PIN")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun SecurityGate(
    onPinEntered: (String) -> Unit,
    canUseBiometric: Boolean,
    onBiometricClick: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    var pin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(morphism.neuShadow.backgroundColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                Icons.Filled.Lock, 
                contentDescription = null, 
                tint = morphism.primaryColor,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                "App Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = morphism.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Please enter your 4-digit PIN to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = morphism.textSecondary,
                textAlign = TextAlign.Center
            )

            NeuTextField(
                value = pin,
                onValueChange = { 
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        pin = it
                        if (it.length == 4) {
                            onPinEntered(it)
                            pin = "" // Clear for next try if failed
                        }
                    }
                },
                placeholder = "• • • •",
                modifier = Modifier.width(200.dp)
            )

            if (canUseBiometric) {
                NeuButton(
                    onClick = onBiometricClick,
                    text = "Use Biometric",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
