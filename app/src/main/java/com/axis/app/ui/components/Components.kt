package com.axis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axis.app.ui.theme.LocalSpacing
import com.axis.app.ui.theme.MorphismThemeDefaults

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val config = MorphismThemeDefaults.config.neuShadow
    val surfaceColor = containerColor ?: config.surfaceColor

    Box(
        modifier = modifier
            .neuShadow(cornerRadius = cornerRadius)
            .clip(RoundedCornerShape(cornerRadius))
            .background(surfaceColor)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun NeuChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = MorphismThemeDefaults.config
    val spacing = LocalSpacing.current
    
    val shadowModifier = if (selected) {
        Modifier.neuInset(cornerRadius = 12.dp)
    } else {
        Modifier.neuShadow(cornerRadius = 12.dp)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(shadowModifier)
            .background(config.neuShadow.surfaceColor)
            .clickable { onClick() }
            .padding(horizontal = spacing.l, vertical = spacing.s),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) config.primaryColor else config.textSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun NeuCircularProgress(
    percentage: Float,
    size: Dp,
    strokeWidth: Dp,
    color: Color = MorphismThemeDefaults.config.primaryColor,
    showText: Boolean = true
) {
    val theme = MorphismThemeDefaults.config
    val density = LocalDensity.current

    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1000),
        label = "CircularProgressAnimation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val strokeWidthPx = with(density) { strokeWidth.toPx() }
            
            // Background Circle (Track)
            drawArc(
                color = theme.neuShadow.backgroundColor.copy(alpha = 0.5f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // Foreground Circle (Progress)
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
        
        if (showText) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.25f).sp
                ),
                color = theme.textPrimary
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(vertical = 8.dp),
        color = MorphismThemeDefaults.config.textMuted
    )
}

@Composable
fun NeuProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val theme = MorphismThemeDefaults.config
    val density = LocalDensity.current

    Box(modifier = modifier.height(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val strokeWidth = with(density) { 8.dp.toPx() }
            drawLine(
                color = theme.neuShadow.surfaceColor,
                start = Offset(x = 0f, y = center.y),
                end = Offset(x = size.width, y = center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = theme.primaryColor,
                start = Offset(x = 0f, y = center.y),
                end = Offset(x = size.width * progress, y = center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun NeuBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .neuShadow(cornerRadius = 0.dp)
            .background(MorphismThemeDefaults.config.neuShadow.backgroundColor)
            .padding(horizontal = spacing.s),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.NeuBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null
) {
    val theme = MorphismThemeDefaults.config
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier
            .weight(1f)
            .selectable(
                selected = selected,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(vertical = spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        
        if (label != null) {
            Spacer(Modifier.height(spacing.xs))
            Box {
                label()
            }
        }
    }
}

@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val config = MorphismThemeDefaults.config
    val spacing = LocalSpacing.current

    NeuCard(
        modifier = modifier,
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(horizontal = spacing.l, vertical = spacing.s)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(spacing.s))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = config.textMuted, style = MaterialTheme.typography.bodyMedium)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = config.textPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(spacing.s))
                trailingIcon()
            }
        }
    }
}

fun Modifier.neuShadow(
    cornerRadius: Dp = 16.dp
): Modifier = composed {
    val config = MorphismThemeDefaults.config.neuShadow
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val elevation = with(density) { config.elevation.toPx() }
    val blurRadius = with(density) { config.blurRadius.toPx() }

    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.color = Color.Transparent.toArgb()

            // 1. Draw the dark shadow (bottom-right)
            frameworkPaint.setShadowLayer(
                blurRadius,
                elevation,
                elevation,
                config.darkShadow.toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                cornerRadiusPx, cornerRadiusPx, paint
            )

            // 2. Draw the light shadow (top-left)
            frameworkPaint.setShadowLayer(
                blurRadius,
                -elevation,
                -elevation,
                config.lightShadow.toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                cornerRadiusPx, cornerRadiusPx, paint
            )
        }
    }
}

@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String
) {
    val config = MorphismThemeDefaults.config
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowModifier = if (isPressed) {
        Modifier.neuInset()
    } else {
        Modifier.neuShadow(cornerRadius = 28.dp)
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .padding(horizontal = LocalSpacing.current.xs)
            .clip(RoundedCornerShape(28.dp))
            .then(shadowModifier)
            .background(config.neuShadow.surfaceColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = config.primaryColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

fun Modifier.neuInset(
    cornerRadius: Dp = 28.dp
): Modifier = composed {
    val config = MorphismThemeDefaults.config.neuShadow
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val elevation = with(density) { 3.dp.toPx() }
    val blurRadius = with(density) { 12.dp.toPx() }

    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.color = Color.Transparent.toArgb()

            // Inset shadows are drawn inside the bounds

            // Top-left dark shadow (for inset)
            frameworkPaint.setShadowLayer(
                blurRadius,
                elevation,
                elevation,
                config.pressedDarkShadow.toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                cornerRadiusPx, cornerRadiusPx, paint
            )

            // Bottom-right light shadow (for inset)
            frameworkPaint.setShadowLayer(
                blurRadius,
                -elevation,
                -elevation,
                config.pressedLightShadow.toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                cornerRadiusPx, cornerRadiusPx, paint
            )
        }
    }
}
