package com.axis.app.ui.theme

import androidx.compose.ui.graphics.Color

object CategoryColorRegistry {

    private val categoryColors = mutableMapOf<String, Color>()

    private val defaultPalette = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF009688), // Teal
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF3F51B5), // Indigo
        Color(0xFF795548), // Brown
        Color(0xFF607D8B)  // Blue Grey
    )

    fun getColor(category: String): Color {
        if (categoryColors.containsKey(category)) {
            return categoryColors[category]!!
        }

        val nextColor = defaultPalette[categoryColors.size % defaultPalette.size]
        categoryColors[category] = nextColor
        return nextColor
    }

    fun getAll(): Map<String, Color> {
        return categoryColors
    }
}
