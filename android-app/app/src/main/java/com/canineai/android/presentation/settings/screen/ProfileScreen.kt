package com.canineai.android.presentation.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineDialog
import com.canineai.android.presentation.components.CanineTextField
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.settings.event.SettingsEvent
import com.canineai.android.presentation.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinician Profile") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CanineCard(modifier = Modifier.fillMaxWidth()) {
                Text("Medical Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Full Name
                CanineTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.onEvent(SettingsEvent.FullNameChanged(it)) },
                    label = "Full Name",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone
                CanineTextField(
                    value = state.phone,
                    onValueChange = { viewModel.onEvent(SettingsEvent.PhoneChanged(it)) },
                    label = "Phone Number",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Department
                CanineTextField(
                    value = state.department,
                    onValueChange = { viewModel.onEvent(SettingsEvent.DepartmentChanged(it)) },
                    label = "Clinic Department",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hospital static label
                CanineTextField(
                    value = state.hospital,
                    onValueChange = {},
                    label = "Hospital Link",
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Medical registration number static label
                CanineTextField(
                    value = state.medicalRegNo,
                    onValueChange = {},
                    label = "Medical Registration Number",
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isSaving) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CanineCircularLoader()
                    }
                } else {
                    CanineButton(
                        text = "Save Profile Updates",
                        onClick = { viewModel.onEvent(SettingsEvent.SaveProfileClicked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (state.showSaveSuccess) {
        CanineDialog(
            title = "Profile Saved",
            message = "Your medical profile credentials updates have been saved.",
            confirmButtonText = "Done",
            onConfirm = { viewModel.onEvent(SettingsEvent.DismissSuccessDialog) },
            onDismissRequest = {}
        )
    }
}
