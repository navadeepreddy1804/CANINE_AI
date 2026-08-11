package com.canineai.android.presentation.analysis.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.analysis.event.AnalysisEvent
import com.canineai.android.presentation.analysis.event.AnalysisUiAction
import com.canineai.android.presentation.analysis.viewmodel.AnalysisViewModel
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    patientId: String = "",
    studyId: String = "",
    onNavigateToWorkspace: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToPatients: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(patientId, studyId) {
        if (patientId.isNotBlank() || studyId.isNotBlank()) {
            viewModel.setInitialStudyAndPatient(patientId, studyId)
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiActions.collect { action ->
            when (action) {
                is AnalysisUiAction.NavigateToReports -> onNavigateToWorkspace()
                is AnalysisUiAction.RouteBackToUpload -> onNavigateBack()
            }
        }
    }

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "analysis",
        onNavigateToHome = onNavigateToHome,
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToAnalysis = { /* Already on Analysis */ },
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = onNavigateToProfile,
        onLogout = onLogout
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Clinical Analysis Deck") },
                    navigationIcon = {
                        CanineIconButton(
                            icon = Icons.Default.Menu,
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            contentDescription = "Menu"
                        )
                    },
                    actions = {
                        CanineIconButton(
                            icon = Icons.Default.Refresh,
                            onClick = { viewModel.onEvent(AnalysisEvent.RestartAnalysis) },
                            contentDescription = "Restart"
                        )
                    }
                )
            },
            bottomBar = {
                com.canineai.android.presentation.components.CanineBottomNavigationBar(
                    currentRoute = "analysis",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToPatients = onNavigateToPatients,
                    onNavigateToUpload = onNavigateToUpload,
                    onNavigateToAnalysis = { /* Already on Analysis */ },
                    onNavigateToReports = onNavigateToReports
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                state.apiError?.let { error ->
                    CanineStatusChip(
                        text = error,
                        status = CanineStatus.ERROR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { viewModel.onEvent(AnalysisEvent.DismissError) }
                    )
                }
                AnalysisWorkspace(
                    state = state,
                    onEvent = { viewModel.onEvent(it) },
                    onNavigateToWorkspace = onNavigateToWorkspace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
