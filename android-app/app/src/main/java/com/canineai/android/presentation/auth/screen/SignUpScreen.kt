package com.canineai.android.presentation.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.auth.event.SignUpEvent
import com.canineai.android.presentation.auth.event.SignUpUiAction
import com.canineai.android.presentation.auth.viewmodel.SignUpViewModel
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.components.CanineLogo
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineTextField

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = true) {
        viewModel.uiActions.collect { action ->
            when (action) {
                is SignUpUiAction.NavigateToDashboard -> {
                    keyboardController?.hide()
                    onNavigateToDashboard()
                }
                is SignUpUiAction.ShowToastError -> {
                    // Handled locally or via snacks
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
            CanineLogo(modifier = Modifier.size(72.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CanineAI Platform",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Create Clinician Account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CanineCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Join our medical imaging workspace",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                state.apiError?.let { err ->
                    CanineStatusChip(
                        text = err,
                        status = CanineStatus.ERROR,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                // Full Name
                CanineTextField(
                    value = state.fullNameValue,
                    onValueChange = { viewModel.onEvent(SignUpEvent.FullNameChanged(it)) },
                    label = "Full Name",
                    placeholder = "Dr. Darshan Shah",
                    errorText = state.fullNameError,
                    leadingIcon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                CanineTextField(
                    value = state.usernameValue,
                    onValueChange = { viewModel.onEvent(SignUpEvent.UsernameChanged(it)) },
                    label = "Username",
                    placeholder = "janesmith",
                    errorText = state.usernameError,
                    leadingIcon = Icons.Default.AccountCircle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hospital Email
                CanineTextField(
                    value = state.emailValue,
                    onValueChange = { viewModel.onEvent(SignUpEvent.EmailChanged(it)) },
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

                Spacer(modifier = Modifier.height(12.dp))


                // Contact Phone
                CanineTextField(
                    value = state.phoneValue,
                    onValueChange = { viewModel.onEvent(SignUpEvent.PhoneChanged(it)) },
                    label = "Phone Number",
                    placeholder = "+1 555-0199",
                    errorText = state.phoneError,
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                CanineTextField(
                    value = state.passwordValue,
                    onValueChange = { viewModel.onEvent(SignUpEvent.PasswordChanged(it)) },
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
                                .clickable { viewModel.onEvent(SignUpEvent.TogglePasswordVisibility) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Password
                CanineTextField(
                    value = state.confirmPasswordValue,
                    onValueChange = { viewModel.onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
                    label = "Confirm Password",
                    placeholder = "••••••••",
                    errorText = state.confirmPasswordError,
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CanineCircularLoader()
                    }
                } else {
                    CanineButton(
                        text = "Register Account",
                        onClick = {
                            keyboardController?.hide()
                            viewModel.onEvent(SignUpEvent.SubmitSignUp)
                        },
                        enabled = state.isFormValid,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CanineButton(
                        text = "Already have an account? Sign In",
                        onClick = onNavigateToSignIn,
                        type = CanineButtonType.TEXT,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
