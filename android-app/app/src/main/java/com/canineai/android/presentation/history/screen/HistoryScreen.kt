package com.canineai.android.presentation.history.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canineai.android.data.network.HistoryDto
import com.canineai.android.presentation.components.CanineBottomNavigationBar
import com.canineai.android.presentation.components.CanineCircularLoader
import com.canineai.android.presentation.components.CanineEmptyState
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.history.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToReport: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "history",
        onNavigateToHome = onNavigateToHome,
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToAnalysis = { onNavigateToUpload() },
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = { /* Already on History */ },
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = {},
        onLogout = {}
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Diagnostic History Log",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
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
                            onClick = { viewModel.loadHistory() },
                            contentDescription = "Refresh"
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                CanineBottomNavigationBar(
                    currentRoute = "history",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToPatients = onNavigateToPatients,
                    onNavigateToUpload = onNavigateToUpload,
                    onNavigateToAnalysis = { onNavigateToUpload() },
                    onNavigateToReports = onNavigateToReports
                )
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CanineCircularLoader()
                        }
                    }
                    state.error != null -> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            CanineStatusChip(
                                text = state.error ?: "Error loading history",
                                status = com.canineai.android.presentation.components.CanineStatus.ERROR
                            )
                        }
                    }
                    state.historyLogs.isEmpty() -> {
                        CanineEmptyState(
                            title = "No History Records",
                            message = "Chronological analysis logs will appear here once CBCT scans are analyzed.",
                            icon = Icons.Default.Refresh
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.historyLogs) { item ->
                                HistoryRowCard(item = item, onClick = {
                                    if (!item.studyId.isNullOrBlank()) {
                                        onNavigateToReport(item.studyId)
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRowCard(
    item: HistoryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedPatientId = com.canineai.android.util.PatientIdFormatter.format(item.patientId, item.patientDisplayId)
    val formattedCaseId = com.canineai.android.util.PredictionFormatter.formatCaseId(item.studyId, item.studyDisplayId)
    val formattedPrediction = com.canineai.android.util.PredictionFormatter.formatPrediction(item.prediction)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = formattedPrediction,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = item.patientName ?: "Patient",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = item.status ?: "Completed",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Patient ID: $formattedPatientId • Case: $formattedCaseId",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Confidence: ${item.confidence ?: "N/A"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = item.completedAt?.take(19)?.replace("T", " ") ?: "Date N/A",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
