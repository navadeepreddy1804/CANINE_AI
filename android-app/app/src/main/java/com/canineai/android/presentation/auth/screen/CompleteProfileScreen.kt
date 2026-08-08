package com.canineai.android.presentation.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canineai.android.presentation.auth.viewmodel.LoginViewModel
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineTextField
import com.canineai.android.presentation.components.CanineLogo
import kotlinx.coroutines.launch
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.data.network.UserDto

@Composable
fun CompleteProfileScreen(
    viewModel: com.canineai.android.presentation.auth.viewmodel.CompleteProfileViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    var phone by remember { mutableStateOf("") }
    var roleTitle by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var medicalRegistrationNumber by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Please provide additional clinical details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            CanineCard(modifier = Modifier.fillMaxWidth()) {
                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                CanineTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Mobile Number",
                    placeholder = "+91 98765 43210",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                CanineTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it },
                    label = "Specialization",
                    placeholder = "Orthodontist",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                CanineTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = "Hospital / Clinic Name",
                    placeholder = "Metro Dental Diagnostics",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                CanineTextField(
                    value = medicalRegistrationNumber,
                    onValueChange = { medicalRegistrationNumber = it },
                    label = "Medical Registration Number",
                    placeholder = "MDR-2026-4290",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                CanineButton(
                    text = if (isLoading) "Saving..." else "Complete Setup",
                    onClick = {
                        viewModel.completeProfile(
                            phone = phone,
                            roleTitle = roleTitle,
                            hospital = hospital,
                            medicalRegNo = medicalRegistrationNumber,
                            onSuccess = onNavigateToDashboard
                        )
                    },
                    enabled = !isLoading && phone.isNotBlank() && roleTitle.isNotBlank() && hospital.isNotBlank() && medicalRegistrationNumber.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
