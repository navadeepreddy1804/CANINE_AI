package com.canineai.android.presentation.splash.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.components.CanineLogo
import com.canineai.android.presentation.splash.state.NavigationTarget
import com.canineai.android.presentation.splash.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Animation states for Logo (Material Motion)
    val logoScale = remember { Animatable(0.8f) }
    val logoAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Parallel animations mimicking Material Motion
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
    }

    // React to navigation state trigger
    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { target ->
            when (target) {
                is NavigationTarget.Login -> onNavigateToLogin()
                is NavigationTarget.Dashboard -> onNavigateToDashboard()
            }
        }
    }

    // Medical Gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        // Modern glass card container
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large Logo Area (Animated)
                CanineLogo(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Name & Tagline
                Text(
                    text = "CanineAI",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "AI-Assisted CBCT Dental Analysis Platform",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Loading animations
                CanineCircularLoader()

                Spacer(modifier = Modifier.height(16.dp))

                // Loading progress message text
                Text(
                    text = state.loadingMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                // Optional linear progress indicator showing timeline state
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.width(140.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        }

        // Layout Footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "© 2026 CanineAI. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}
