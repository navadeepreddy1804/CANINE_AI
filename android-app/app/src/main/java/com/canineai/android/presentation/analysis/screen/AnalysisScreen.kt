package com.canineai.android.presentation.analysis.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.analysis.event.AnalysisEvent
import com.canineai.android.presentation.analysis.event.AnalysisUiAction
import com.canineai.android.presentation.analysis.viewmodel.AnalysisViewModel
import androidx.compose.runtime.LaunchedEffect
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onNavigateToWorkspace: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.uiActions.collect { action ->
            when (action) {
                is AnalysisUiAction.NavigateToReports -> onNavigateToWorkspace()
                is AnalysisUiAction.RouteBackToUpload -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Analysis Deck") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
                },
                actions = {
                    CanineIconButton(icon = Icons.Default.Refresh, onClick = { viewModel.onEvent(AnalysisEvent.RestartAnalysis) }, contentDescription = "Restart")
                }
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
