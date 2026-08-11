package com.canineai.android.presentation.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.settings.components.AISettings
import com.canineai.android.presentation.settings.components.ClinicalWorkspaceSummary
import com.canineai.android.presentation.settings.components.ThemeSettings
import com.canineai.android.presentation.settings.event.SettingsEvent
import com.canineai.android.presentation.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToPatients: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "settings",
        onNavigateToHome = onNavigateToHome,
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToAnalysis = { onNavigateToUpload() },
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = { /* Already on Settings */ },
        onNavigateToProfile = onNavigateToProfile,
        onLogout = {}
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings & Preferences") },
                    navigationIcon = {
                        CanineIconButton(
                            icon = Icons.Default.Menu,
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            contentDescription = "Menu"
                        )
                    },
                    actions = {
                        CanineIconButton(icon = Icons.Default.Person, onClick = onNavigateToProfile, contentDescription = "Profile")
                        CanineIconButton(icon = Icons.Default.Info, onClick = onNavigateToAbout, contentDescription = "About")
                    }
                )
            },
            bottomBar = {
                com.canineai.android.presentation.components.CanineBottomNavigationBar(
                    currentRoute = "settings",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToPatients = onNavigateToPatients,
                    onNavigateToUpload = onNavigateToUpload,
                    onNavigateToAnalysis = { onNavigateToUpload() },
                    onNavigateToReports = onNavigateToReports
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
                // Profile Summary Card
                CanineCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.fullName.ifBlank { "Dr. Clinician" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = state.email.ifBlank { "doctor@canineai.com" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Role: ${state.role.ifBlank { "ORTHODONTIST" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = onNavigateToProfile) {
                            Text("View Profile")
                        }
                    }
                }

                ThemeSettings(
                    state = state,
                    onDarkModeToggled = { viewModel.onEvent(SettingsEvent.DarkModeToggled(it)) },
                    onNotificationsToggled = { viewModel.onEvent(SettingsEvent.NotificationsToggled(it)) }
                )

                AISettings(state = state)

                ClinicalWorkspaceSummary(
                    activePatients = state.activePatientsCount ?: 0,
                    completedAnalyses = state.completedAnalysesCount ?: 0,
                    reportsGenerated = state.reportsGeneratedCount ?: 0
                )
            }
        }
    }
}
