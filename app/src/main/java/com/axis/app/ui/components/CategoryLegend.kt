package com.axis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.ui.theme.CategoryColorRegistry
import com.axis.app.ui.theme.LocalMorphismConfig

@Composable
fun CategoryLegend(categories: List<String>, modifier: Modifier = Modifier) {
    val morphism = LocalMorphismConfig.current
    
    Column(modifier = modifier) {
        categories.forEach { category ->
            val color = CategoryColorRegistry.getColor(category)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = morphism.textPrimary
                )
            }
        }
    }
}
