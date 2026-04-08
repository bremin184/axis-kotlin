package com.axis.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axis.app.ui.theme.LocalMorphismConfig

@Composable
fun SecuritySetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = morphism.neuShadow.backgroundColor,
        title = {
            Text("Setup Fast PIN", color = morphism.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Enter a 4-digit numeric PIN to secure your financial data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = morphism.textSecondary
                )
                
                NeuTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    placeholder = "Enter 4-digit PIN",
                    modifier = Modifier.fillMaxWidth()
                )

                NeuTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    placeholder = "Confirm PIN",
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = morphism.dangerColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            NeuButton(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN must be 4 digits"
                        pin != confirmPin -> error = "PINs do not match"
                        else -> {
                            onConfirm(pin)
                        }
                    }
                },
                text = "Save PIN",
                modifier = Modifier.padding(8.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = morphism.textMuted)
            }
        }
    )
}
