package com.axis.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shadow configuration for neumorphic elements.
 */
data class NeuShadowConfig(
    val lightShadow: Color,
    val darkShadow: Color,
    val elevation: Dp = 6.dp,
    val blurRadius: Dp = 20.dp,
    val cornerRadius: Dp = 16.dp,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val pressedLightShadow: Color,
    val pressedDarkShadow: Color
)

/**
 * Full theme configuration for the Axis Neumorphic Design System.
 */
data class MorphismConfig(
    val neuShadow: NeuShadowConfig,
    val isDark: Boolean,
    // Shared semantic colors
    val primaryColor: Color = Primary,
    val successColor: Color = Success,
    val dangerColor: Color = Danger,
    val warningColor: Color = Warning,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

// ========================
// Light Mode Config
// ========================

private val LightNeuShadow = NeuShadowConfig(
    lightShadow = LightShadow,
    darkShadow = DarkShadowLight,
    elevation = 6.dp,
    blurRadius = 20.dp,
    cornerRadius = 16.dp,
    backgroundColor = LightBackground,
    surfaceColor = CardBaseLight,
    pressedLightShadow = DarkShadowLight, // Inset shadow uses the dark shadow color
    pressedDarkShadow = LightShadow // Inset shadow uses the light shadow color
)

// ========================
// Dark Mode Config
// ========================

private val DarkNeuShadow = NeuShadowConfig(
    lightShadow = LightShadowDark,
    darkShadow = DarkShadowDark,
    elevation = 6.dp,
    blurRadius = 20.dp,
    cornerRadius = 16.dp,
    backgroundColor = DarkBackground,
    surfaceColor = CardSurfaceDark,
    pressedLightShadow = DarkShadowDark, // Inset shadow uses the dark shadow color
    pressedDarkShadow = LightShadowDark // Inset shadow uses the light shadow color
)

// ========================
// CompositionLocal
// ========================

val LocalMorphismConfig = staticCompositionLocalOf<MorphismConfig> {
    error("No MorphismConfig provided. Wrap with MorphismTheme composable.")
}

/**
 * Top-level theme wrapper for the Axis Neumorphic Design System.
 */
@Composable
fun AxisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val config = if (darkTheme) {
        MorphismConfig(
            neuShadow = DarkNeuShadow,
            isDark = true,
            textPrimary = TextPrimaryDark,
            textSecondary = TextMuted, // Using muted for secondary text in dark mode
            textMuted = TextMuted
        )
    } else {
        MorphismConfig(
            neuShadow = LightNeuShadow,
            isDark = false,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textMuted = TextMuted
        )
    }

    CompositionLocalProvider(
        LocalMorphismConfig provides config,
        LocalSpacing provides Spacing()
    ) {
        PesaAITheme(darkTheme = darkTheme) {
            content()
        }
    }
}

/**
 * Convenience accessor for the theme configuration.
 */
object MorphismThemeDefaults {
    val config: MorphismConfig
        @Composable
        @ReadOnlyComposable
        get() = LocalMorphismConfig.current
}
