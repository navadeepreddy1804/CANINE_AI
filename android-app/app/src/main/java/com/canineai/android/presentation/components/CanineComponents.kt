package com.canineai.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.theme.ColorSuccess
import com.canineai.android.presentation.theme.ColorWarning
import com.canineai.android.presentation.theme.ColorError
import com.canineai.android.presentation.theme.ColorInfo

// ==========================================
// BRANDING LOGO (Abstract Dental Canine + CBCT Scanner Icon)
// ==========================================

@Composable
fun CanineLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw Outer CBCT scanner ring
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = w * 0.45f,
            style = Stroke(width = w * 0.08f)
        )
        // Draw Inner target tracking circle
        drawCircle(
            color = color,
            radius = w * 0.35f,
            style = Stroke(width = w * 0.04f)
        )
        // Draw central dental canine tooth shape
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            quadraticBezierTo(w * 0.65f, h * 0.45f, w * 0.6f, h * 0.75f)
            quadraticBezierTo(w * 0.5f, h * 0.65f, w * 0.4f, h * 0.75f)
            quadraticBezierTo(w * 0.35f, h * 0.45f, w * 0.5f, h * 0.25f)
            close()
        }
        drawPath(
            path = path,
            color = color
        )
    }
}

// ==========================================
// BUTTONS
// ==========================================

enum class CanineButtonType {
    FILLED, OUTLINED, TEXT
}

@Composable
fun CanineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: CanineButtonType = CanineButtonType.FILLED,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors? = null
) {
    val defaultColors = colors ?: when (type) {
        CanineButtonType.FILLED -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        CanineButtonType.OUTLINED -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        CanineButtonType.TEXT -> ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }

    val buttonModifier = modifier
        .height(48.dp)
        .clip(MaterialTheme.shapes.medium)

    when (type) {
        CanineButtonType.FILLED -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = defaultColors,
                shape = MaterialTheme.shapes.medium,
                modifier = buttonModifier
            ) {
                ButtonContent(text, icon)
            }
        }
        CanineButtonType.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                colors = defaultColors,
                shape = MaterialTheme.shapes.medium,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp
                ),
                modifier = buttonModifier
            ) {
                ButtonContent(text, icon)
            }
        }
        CanineButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                colors = defaultColors,
                shape = MaterialTheme.shapes.medium,
                modifier = buttonModifier
            ) {
                ButtonContent(text, icon)
            }
        }
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun CanineIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(48.dp) // Standard minimum touch target
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ==========================================
// TEXT FIELDS
// ==========================================

@Composable
fun CanineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    readOnly: Boolean = false
) {
    val isError = errorText != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp)) } },
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (errorText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// ==========================================
// CARDS & SURFACES
// ==========================================

@Composable
fun CanineCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .clip(MaterialTheme.shapes.large)
        .background(containerColor)
        .border(1.dp, borderColor, MaterialTheme.shapes.large)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(16.dp)

    Column(modifier = cardModifier) {
        content()
    }
}

// ==========================================
// STATUS CHIPS / BADGES
// ==========================================

enum class CanineStatus {
    SUCCESS, WARNING, ERROR, INFO
}

@Composable
fun CanineStatusChip(
    text: String,
    status: CanineStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        CanineStatus.SUCCESS -> ColorSuccess.copy(alpha = 0.15f) to ColorSuccess
        CanineStatus.WARNING -> ColorWarning.copy(alpha = 0.15f) to ColorWarning
        CanineStatus.ERROR -> ColorError.copy(alpha = 0.15f) to ColorError
        CanineStatus.INFO -> ColorInfo.copy(alpha = 0.15f) to ColorInfo
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// ==========================================
// LOADERS & SKELETON
// ==========================================

@Composable
fun CanineCircularLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(36.dp),
        color = color,
        strokeWidth = 3.dp
    )
}

@Composable
fun CanineSkeleton(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    borderRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(borderRadius))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

// ==========================================
// DIALOGS & MODALS
// ==========================================

@Composable
fun CanineDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissButtonText: String? = null,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDestructive) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            CanineButton(
                text = confirmButtonText,
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else null
            )
        },
        dismissButton = dismissButtonText?.let {
            {
                CanineButton(
                    text = it,
                    onClick = onDismissRequest,
                    type = CanineButtonType.TEXT
                )
            }
        },
        shape = MaterialTheme.shapes.large
    )
}

// ==========================================
// PROFESSIONAL UI STATES (EMPTY, LOADING, ERROR)
// ==========================================

@Composable
fun CanineEmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                CanineButton(
                    text = actionText,
                    onClick = onActionClick,
                    type = CanineButtonType.FILLED
                )
            }
        }
    }
}

@Composable
fun CanineLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CanineCircularLoader(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CanineErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(ColorError.copy(alpha = 0.05f), shape = MaterialTheme.shapes.large)
            .border(1.dp, ColorError.copy(alpha = 0.2f), MaterialTheme.shapes.large)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(ColorError.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ColorError,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Operation Failed",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ColorError
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                CanineButton(
                    text = "Retry Action",
                    onClick = onRetry,
                    type = CanineButtonType.OUTLINED,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError)
                )
            }
        }
    }
}
