package com.canineai.android.presentation.patients.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineTextField
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.components.CanineEmptyState
import com.canineai.android.presentation.components.CanineLoadingState
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.patients.event.PatientEvent
import com.canineai.android.presentation.patients.state.PatientItem
import com.canineai.android.presentation.patients.viewmodel.PatientViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    viewModel: PatientViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    val onPatientClick = remember(viewModel, onNavigateToDetails) {
        { patientId: String ->
            viewModel.onEvent(PatientEvent.SelectPatient(patientId))
            onNavigateToDetails(patientId)
        }
    }

    val onLoadNextPage = remember(viewModel) {
        { viewModel.onEvent(PatientEvent.LoadNextPage) }
    }

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "patients",
        onNavigateToHome = onNavigateToHome,
        onNavigateToPatients = { /* Already on Patients */ },
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToAnalysis = { onNavigateToUpload() },
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = {},
        onLogout = {}
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Patient Registry") },
                    navigationIcon = {
                        CanineIconButton(
                            icon = Icons.Default.Menu,
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            contentDescription = "Menu"
                        )
                    },
                    actions = {
                        CanineIconButton(icon = Icons.Default.MoreVert, onClick = { showFilterSheet = true }, contentDescription = "Filter Options")
                    }
                )
            },
            bottomBar = {
                com.canineai.android.presentation.components.CanineBottomNavigationBar(
                    currentRoute = "patients",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToPatients = { /* Already on Patients */ },
                    onNavigateToUpload = onNavigateToUpload,
                    onNavigateToAnalysis = { onNavigateToUpload() },
                    onNavigateToReports = onNavigateToReports
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddPatient,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Patient")
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Search Bar
                CanineTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(PatientEvent.SearchQueryChanged(it)) },
                    label = "Search Patients",
                    placeholder = "Search by ID, name, phone...",
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error display
                state.apiError?.let { err ->
                    CanineStatusChip(
                        text = err,
                        status = CanineStatus.ERROR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { viewModel.onEvent(PatientEvent.DismissError) }
                    )
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CanineLoadingState(message = "Fetching patient registry...")
                    }
                } else if (state.filteredPatientsList.isEmpty()) {
                    CanineEmptyState(
                        title = "No Patients Registered",
                        message = if (state.searchQuery.isNotEmpty()) "No patients found matching your search criteria." else "Add patient profiles to start CBCT diagnostics.",
                        icon = Icons.Default.Person,
                        actionText = if (state.searchQuery.isEmpty()) "Admit Patient" else null,
                        onActionClick = if (state.searchQuery.isEmpty()) { { onNavigateToAddPatient() } } else null
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = state.filteredPatientsList,
                            key = { _, patient -> patient.id }
                        ) { index, patient ->
                            PatientRowCard(
                                patient = patient,
                                onClick = { onPatientClick(patient.id) }
                            )
                            
                            if (index == state.filteredPatientsList.lastIndex && !state.isLoadingNextPage && !state.isLastPage) {
                                LaunchedEffect(key1 = index) {
                                    onLoadNextPage()
                                }
                            }
                        }
                        
                        if (state.isLoadingNextPage) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CanineCircularLoader()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Advanced Filters",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Gender", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = state.filterGender == null,
                        onClick = { viewModel.onEvent(PatientEvent.GenderFilterChanged(null)) },
                        label = { Text("All") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = state.filterGender == "Male",
                        onClick = { viewModel.onEvent(PatientEvent.GenderFilterChanged("Male")) },
                        label = { Text("Male") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = state.filterGender == "Female",
                        onClick = { viewModel.onEvent(PatientEvent.GenderFilterChanged("Female")) },
                        label = { Text("Female") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Status", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = state.filterStatus == null,
                        onClick = { viewModel.onEvent(PatientEvent.StatusFilterChanged(null)) },
                        label = { Text("All") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = state.filterStatus == "Active",
                        onClick = { viewModel.onEvent(PatientEvent.StatusFilterChanged("Active")) },
                        label = { Text("Active") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = state.filterStatus == "Archived",
                        onClick = { viewModel.onEvent(PatientEvent.StatusFilterChanged("Archived")) },
                        label = { Text("Archived") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                CanineButton(
                    text = "Apply Filters",
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatPatientId(id: String): String {
    return try {
        val num = id.toInt()
        String.format("PT-%05d", num)
    } catch (e: Exception) {
        if (id.startsWith("PT-")) id else "PT-$id"
    }
}

@Composable
private fun PatientRowCard(
    patient: PatientItem,
    onClick: () -> Unit
) {
    CanineCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = patient.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${formatPatientId(patient.id)} • Age: ${patient.age} • ${patient.gender}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone: ${patient.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            CanineStatusChip(
                text = patient.status.uppercase(),
                status = if (patient.status == "Active") CanineStatus.SUCCESS else CanineStatus.WARNING
            )
        }
    }
}
