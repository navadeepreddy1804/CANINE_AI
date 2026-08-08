package com.canineai.android.presentation.analysis.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.analysis.components.AnalysisProgressCard
import com.canineai.android.presentation.analysis.components.ClinicalDiagnosticCard
import com.canineai.android.presentation.analysis.components.ModelStatusCard
import com.canineai.android.presentation.analysis.components.PipelineProgress
import com.canineai.android.presentation.analysis.components.StudyCbctViewer
import com.canineai.android.presentation.analysis.components.ToothSegFindingsCard
import com.canineai.android.presentation.analysis.event.AnalysisEvent
import com.canineai.android.presentation.analysis.state.AnalysisState

@Composable
fun AnalysisWorkspace(
    state: AnalysisState,
    onEvent: (AnalysisEvent) -> Unit,
    onNavigateToWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Pipeline status tracker
        AnalysisProgressCard(
            state = state,
            onStart = { onEvent(AnalysisEvent.StartAnalysis) },
            onCancel = { onEvent(AnalysisEvent.CancelAnalysis) }
        )

        // Interactive ToothSeg CBCT Viewer
        StudyCbctViewer(
            state = state,
            onSliceIndexChanged = { onEvent(AnalysisEvent.SlideSliceIndex(it)) },
            onToggleCanine = { onEvent(AnalysisEvent.ToggleCanineHighlight) },
            onFocusCanine = { onEvent(AnalysisEvent.FocusCanineSlice) }
        )

        // If analysis is in progress, show pipeline progress milestones
        if (!state.isComplete) {
            PipelineProgress(currentStage = state.pipelineStage)
        }

        // When complete: Section 1 (Real ToothSeg) and Section 2 (Clinical Diagnostics)
        if (state.isComplete) {
            ToothSegFindingsCard(state = state)
            ClinicalDiagnosticCard(state = state)
        }

        // Compute parameters card
        ModelStatusCard(state = state)

        Spacer(modifier = Modifier.height(8.dp))

        // Navigate forward to Report compiler
        if (state.isComplete) {
            CanineButton(
                text = "Proceed to Diagnostic Report",
                onClick = { onEvent(AnalysisEvent.GenerateReportRequested) },
                icon = Icons.Default.List,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
