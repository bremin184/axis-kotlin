package com.axis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.data.model.Transaction
import com.axis.app.domain.classifier.CategoryClassifier
import com.axis.app.ui.theme.LocalMorphismConfig
import com.axis.app.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecategorizationSheet(
    transaction: Transaction,
    onCategorize: (String, Boolean) -> Unit, // category, applyToAll
    onDismiss: () -> Unit
) {
    val morphism = LocalMorphismConfig.current
    val spacing = LocalSpacing.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    .clip(CircleShape)
                    .background(morphism.textMuted.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Recategorize",
                style = MaterialTheme.typography.headlineSmall,
                color = morphism.textPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Transaction with ${transaction.recipient ?: transaction.type}",
                style = MaterialTheme.typography.bodyMedium,
                color = morphism.textMuted
            )

            NeuCard(modifier = Modifier.fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CategoryClassifier.CATEGORY_META.toList()) { (key, meta) ->
                        val isSelected = transaction.category == key
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(meta.color))
                        } catch (_: Exception) { morphism.textMuted }

                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onCategorize(key, true) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .neuShadow(cornerRadius = 24.dp)
                                    .background(if (isSelected) catColor.copy(alpha = 0.2f) else morphism.neuShadow.surfaceColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = catColor)
                                } else {
                                    Icon(Icons.Default.Category, null, tint = catColor.copy(alpha = 0.6f))
                                }
                            }
                            Text(
                                meta.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) morphism.textPrimary else morphism.textMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            
            NeuButton(
                onClick = onDismiss,
                text = "Cancel",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
