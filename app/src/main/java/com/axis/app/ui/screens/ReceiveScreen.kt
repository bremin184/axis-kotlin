package com.axis.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.NeuButton
import com.axis.app.ui.components.NeuCard
import com.axis.app.ui.components.NeuTextField
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // In a real app, this would come from a user profile or settings
    var userPhone by remember { mutableStateOf("0712345678") } 
    var amountRequest by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive Money", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            // QR Code Placeholder
            NeuCard(
                modifier = Modifier
                    .size(240.dp)
                    .aspectRatio(1f),
                containerColor = morphism.neuShadow.surfaceColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = morphism.textPrimary
                        )
                        Text(
                            "Scan to Pay",
                            style = MaterialTheme.typography.labelMedium,
                            color = morphism.textMuted
                        )
                    }
                }
            }

            // Phone Number Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your M-Pesa Number",
                    style = MaterialTheme.typography.bodySmall,
                    color = morphism.textMuted
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        userPhone,
                        style = MaterialTheme.typography.headlineMedium,
                        color = morphism.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(userPhone))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = morphism.primaryColor)
                    }
                }
            }

            // Payment Request Intelligence
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    Text(
                        "Quick Payment Request",
                        style = MaterialTheme.typography.titleSmall,
                        color = morphism.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    NeuTextField(
                        value = amountRequest,
                        onValueChange = { amountRequest = it },
                        placeholder = "Amount (Optional)",
                        leadingIcon = { Icon(Icons.Default.Payments, null, tint = morphism.textMuted) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    NeuButton(
                        onClick = {
                            val message = if (amountRequest.isBlank()) {
                                "Please send money via M-Pesa to $userPhone"
                            } else {
                                "Please send Ksh $amountRequest via M-Pesa to $userPhone"
                            }
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, message)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        text = "Share Request Link",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Text(
                "Turn your phone into a mini invoicing system by sharing request links with context.",
                style = MaterialTheme.typography.bodySmall,
                color = morphism.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
