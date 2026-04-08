package com.axis.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val userName by viewModel.userName.collectAsState()
    var nameInput by remember { mutableStateOf(userName) }

    LaunchedEffect(userName) {
        nameInput = userName
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
                    "Profile Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = morphism.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        containerColor = morphism.neuShadow.backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            Text(
                "Your information helps Axis personalize your financial insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = morphism.textMuted
            )

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Display Name",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                    NeuTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "Enter your full name",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val userEmail by viewModel.userEmail.collectAsState()
            var emailInput by remember { mutableStateOf(userEmail) }
            LaunchedEffect(userEmail) { emailInput = userEmail }

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Email Address",
                        style = MaterialTheme.typography.labelSmall,
                        color = morphism.textMuted
                    )
                    NeuTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = "Enter your email",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            NeuButton(
                onClick = {
                    viewModel.updateUserName(nameInput)
                    viewModel.updateUserEmail(emailInput)
                    onBack()
                },
                text = "Save Profile",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
