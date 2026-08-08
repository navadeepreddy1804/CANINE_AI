package com.canineai.android.presentation.upload.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineErrorState
import com.canineai.android.presentation.components.CanineEmptyState
import com.canineai.android.presentation.upload.components.DicomMetadataCard
import com.canineai.android.presentation.upload.components.DicomThumbnailGrid
import com.canineai.android.presentation.upload.event.UploadEvent
import com.canineai.android.presentation.upload.event.UploadUiAction
import com.canineai.android.presentation.upload.viewmodel.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWorkspace(
    viewModel: UploadViewModel,
    onNavigateToAnalysis: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val metadata = state.dicomMetadata

    // Listen to UI side effects
    LaunchedEffect(key1 = true) {
        viewModel.uiActions.collect { action ->
            when (action) {
                is UploadUiAction.NavigateToAIAnalysis -> onNavigateToAnalysis(action.patientId, action.studyId)
                is UploadUiAction.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DICOM Workspace") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (metadata == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CanineEmptyState(
                    title = "Workspace Empty",
                    message = "No parsed study records available in workspace. Please go back and upload a valid DICOM study.",
                    icon = Icons.Default.PlayArrow // Or another appropriate icon
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Patient Summary card
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Patient EMR Association",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metadata.patientName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Patient ID: ${metadata.patientId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Workspace Tabs switcher
                TabRow(selectedTabIndex = state.selectedTab) {
                    Tab(
                        selected = state.selectedTab == 0,
                        onClick = { viewModel.onEvent(UploadEvent.SwitchWorkspaceTab(0)) },
                        text = { Text("Reconstructed Preview") }
                    )
                    Tab(
                        selected = state.selectedTab == 1,
                        onClick = { viewModel.onEvent(UploadEvent.SwitchWorkspaceTab(1)) },
                        text = { Text("Metadata Tags") }
                    )
                }

                // Display selected tab contents
                when (state.selectedTab) {
                    0 -> {
                        DicomThumbnailGrid(sliceCount = metadata.sliceCount)
                    }
                    1 -> {
                        DicomMetadataCard(metadata = metadata)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Proceed to AI Analysis button trigger
                CanineButton(
                    text = "Proceed to AI Analysis",
                    onClick = { viewModel.onEvent(UploadEvent.ProceedToAIAnalysisRequested) },
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
