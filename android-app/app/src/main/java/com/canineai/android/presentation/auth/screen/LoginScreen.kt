package com.canineai.android.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.auth.event.LoginEvent
import com.canineai.android.presentation.auth.event.LoginUiAction
import com.canineai.android.presentation.auth.viewmodel.LoginViewModel
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.components.CanineLogo
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineTextField

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AccountCircle

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val googleSignInClient = remember {
        val clientId = com.canineai.android.BuildConfig.GOOGLE_WEB_CLIENT_ID
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (clientId.isNotBlank()) {
            builder.requestIdToken(clientId)
        }
        GoogleSignIn.getClient(context, builder.build())
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.onEvent(LoginEvent.SubmitGoogleLogin(idToken))
            }
        } catch (e: ApiException) {
            // Handled via ViewModel
        }
    }

    // Local state to handle first-time interactive Onboarding Carousel
    var showOnboarding by remember { mutableStateOf(true) }
    var currentSlideIndex by remember { mutableStateOf(0) }

    val slides = listOf(
        OnboardingSlide(
            title = "Advanced CBCT Imaging",
            description = "Seamlessly import high-resolution 3D DICOM volumes or NIfTI (.nii, .nii.gz) scans directly into the secure cloud repository.",
            icon = Icons.Default.Share
        ),
        OnboardingSlide(
            title = "Precise Canine Localization",
            description = "Locate teeth boundaries and segment anatomical landmarks within seconds using clinical-grade AI network inference pipelines.",
            icon = Icons.Default.PlayArrow
        ),
        OnboardingSlide(
            title = "Clinical Report Templates",
            description = "Create consistent reports from stored clinical measurements, risk scores, and recommendations.",
            icon = Icons.Default.Info
        ),
        OnboardingSlide(
            title = "Hospital-Grade Integrity",
            description = "Complies fully with medical data privacy protocols. Role-based layouts ready for orthodontists, radiologists, and administrative clinics.",
            icon = Icons.Default.Check
        )
    )

    // Listen to UI side effects
    LaunchedEffect(key1 = true) {
        viewModel.uiActions.collect { action ->
            when (action) {
                is LoginUiAction.NavigateToDashboard -> {
                    keyboardController?.hide()
                    onNavigateToDashboard()
                }
                is LoginUiAction.NavigateToCompleteProfile -> {
                    keyboardController?.hide()
                    onNavigateToCompleteProfile()
                }
                is LoginUiAction.ShowToastError -> {
                    // Handled locally or via snackbars
                }
            }
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Organization Branding Logo Header
            CanineLogo(modifier = Modifier.size(72.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CanineAI Platform",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Clinical Medical Image Suite",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (showOnboarding) {
                // Render Onboarding Deck
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Platform Tour",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { showOnboarding = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Slide Icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slides[currentSlideIndex].icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slide Text content
                        Text(
                            text = slides[currentSlideIndex].title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = slides[currentSlideIndex].description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(80.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Indicators dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            slides.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == currentSlideIndex) 16.dp else 8.dp, 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == currentSlideIndex) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Controls
                        CanineButton(
                            text = if (currentSlideIndex == slides.lastIndex) "Get Started" else "Next Step",
                            onClick = {
                                if (currentSlideIndex < slides.lastIndex) {
                                    currentSlideIndex++
                                } else {
                                    showOnboarding = false
                                }
                            },
                            icon = Icons.Default.KeyboardArrowRight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Login input container
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Access dental CBCT analysis tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Network Offline Status Tag
                    if (state.isOffline) {
                        CanineStatusChip(
                            text = "Working Offline Mode",
                            status = CanineStatus.WARNING,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                    }

                    // Error response Tag
                    state.apiError?.let { err ->
                        CanineStatusChip(
                            text = err,
                            status = CanineStatus.ERROR,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                    }

                    // Email Field
                    CanineTextField(
                        value = state.emailValue,
                        onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                        label = "Email",
                        placeholder = "physician@hospital.org",
                        errorText = state.emailError,
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    CanineTextField(
                        value = state.passwordValue,
                        onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                        label = "Security Password",
                        placeholder = "••••••••",
                        errorText = state.passwordError,
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (state.isPasswordVisible) "Hide" else "Show",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.onEvent(LoginEvent.TogglePasswordVisibility) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remember Me Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.isRememberMeChecked,
                            onCheckedChange = { viewModel.onEvent(LoginEvent.ToggleRememberMe) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Remember Me",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CanineCircularLoader()
                        }
                    } else {
                        CanineButton(
                            text = "Sign In to Platform",
                            onClick = {
                                keyboardController?.hide()
                                viewModel.onEvent(LoginEvent.SubmitLogin)
                            },
                            enabled = state.isFormValid,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        CanineButton(
                            text = "Don't have an account? Sign Up",
                            onClick = onNavigateToSignUp,
                            type = CanineButtonType.TEXT,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (com.canineai.android.BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Divider(modifier = Modifier.weight(1f))
                                Text(
                                    text = "OR",
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Divider(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CanineButton(
                                text = "Continue with Google",
                                onClick = {
                                    val intent = googleSignInClient.signInIntent
                                    googleAuthLauncher.launch(intent)
                                },
                                icon = Icons.Default.AccountCircle, // Placeholder for Google Icon
                                type = CanineButtonType.OUTLINED,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CanineAI Suite v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "© 2026 CanineAI. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector
)
