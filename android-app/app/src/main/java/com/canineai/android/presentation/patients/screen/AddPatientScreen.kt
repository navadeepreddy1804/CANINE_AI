package com.canineai.android.presentation.patients.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineDialog
import com.canineai.android.presentation.components.CanineTextField
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.patients.event.PatientEvent
import com.canineai.android.presentation.patients.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    viewModel: PatientViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admit New Patient") },
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
                Text(
                    "EMR Record Information",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Name
                CanineTextField(
                    value = state.inputFullName,
                    onValueChange = { viewModel.onEvent(PatientEvent.FullNameChanged(it)) },
                    label = "Full Name",
                    placeholder = "John Doe",
                    errorText = state.inputFullNameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Age
                    CanineTextField(
                        value = state.inputAge,
                        onValueChange = { viewModel.onEvent(PatientEvent.AgeChanged(it)) },
                        label = "Age",
                        placeholder = "34",
                        errorText = state.inputAgeError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Gender (mock select)
                    CanineTextField(
                        value = state.inputGender,
                        onValueChange = { viewModel.onEvent(PatientEvent.GenderChanged(it)) },
                        label = "Gender",
                        placeholder = "Male / Female",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DOB with Calendar Picker
                val context = androidx.compose.ui.platform.LocalContext.current
                val calendar = java.util.Calendar.getInstance()
                val datePickerDialog = remember {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val formatted = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            viewModel.onEvent(PatientEvent.DobChanged(formatted))
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    ).apply {
                        datePicker.maxDate = System.currentTimeMillis()
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                    CanineTextField(
                        value = state.inputDob,
                        onValueChange = {},
                        label = "Date of Birth",
                        placeholder = "Select Date from Calendar",
                        readOnly = true,
                        trailingIcon = {
                            CanineIconButton(
                                icon = Icons.Default.DateRange,
                                onClick = { datePickerDialog.show() },
                                contentDescription = "Select Date of Birth"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Blood Group Dropdown Selector
                var isBloodGroupDropdownExpanded by remember { mutableStateOf(false) }
                val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

                Box(modifier = Modifier.fillMaxWidth()) {
                    CanineTextField(
                        value = state.inputBloodGroup,
                        onValueChange = {},
                        label = "Blood Group (Mandatory)",
                        placeholder = "Select Blood Group",
                        readOnly = true,
                        trailingIcon = {
                            CanineIconButton(icon = Icons.Default.ArrowDropDown, onClick = { isBloodGroupDropdownExpanded = true }, contentDescription = "Select Blood Group")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isBloodGroupDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = isBloodGroupDropdownExpanded,
                        onDismissRequest = { isBloodGroupDropdownExpanded = false }
                    ) {
                        bloodGroups.forEach { bg ->
                            DropdownMenuItem(
                                text = { Text(bg) },
                                onClick = {
                                    viewModel.onEvent(PatientEvent.BloodGroupChanged(bg))
                                    isBloodGroupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Phone
                CanineTextField(
                    value = state.inputPhone,
                    onValueChange = { viewModel.onEvent(PatientEvent.PhoneChanged(it)) },
                    label = "Phone Number",
                    placeholder = "+1 555-0100",
                    errorText = state.inputPhoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email
                CanineTextField(
                    value = state.inputEmail,
                    onValueChange = { viewModel.onEvent(PatientEvent.EmailChanged(it)) },
                    label = "Email Address",
                    placeholder = "patient@email.com",
                    errorText = state.inputEmailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Orthodontist
                CanineTextField(
                    value = state.inputOrthodontist,
                    onValueChange = { viewModel.onEvent(PatientEvent.OrthodontistChanged(it)) },
                    label = "Attending Orthodontist",
                    placeholder = "Dr. Allan Green",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                CanineTextField(
                    value = state.inputNotes,
                    onValueChange = { viewModel.onEvent(PatientEvent.NotesChanged(it)) },
                    label = "Clinical Medical Notes",
                    placeholder = "Write medical background comments...",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                state.apiError?.let { err ->
                    com.canineai.android.presentation.components.CanineStatusChip(
                        text = "Unable to admit patient: $err",
                        status = com.canineai.android.presentation.components.CanineStatus.ERROR,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                // Action buttons container
                if (state.isFormSaving) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CanineCircularLoader()
                            Text("Admitting patient...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        CanineButton(
                            text = "Admit Patient",
                            onClick = { viewModel.onEvent(PatientEvent.SavePatientSubmitted) },
                            enabled = state.isFormValid,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CanineButton(
                            text = "Cancel",
                            onClick = onNavigateBack,
                            type = CanineButtonType.TEXT,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Rich Success Confirmation Dialog
    if (state.showSuccessDialog) {
        val created = state.createdPatient
        val patientIdFormatted = created?.id?.let { id ->
            if (id.startsWith("PT-")) id else try { String.format("PT-%05d", id.toInt()) } catch (e: Exception) { "PT-$id" }
        } ?: "PT-00001"

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(PatientEvent.DismissSuccessDialog) },
            title = {
                Text(
                    text = "Patient Admitted Successfully",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The EMR record has been created and synced with the backend database.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Patient Name: ${created?.fullName ?: state.inputFullName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Patient ID: $patientIdFormatted", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("Demographics: ${created?.age ?: state.inputAge} YRS • ${created?.gender ?: state.inputGender}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = created?.id ?: "1"
                    viewModel.onEvent(PatientEvent.DismissSuccessDialog)
                    onNavigateBack()
                }) {
                    Text("Done")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.onEvent(PatientEvent.DismissSuccessDialog)
                    onNavigateBack()
                }) {
                    Text("View List")
                }
            }
        )
    }
}
