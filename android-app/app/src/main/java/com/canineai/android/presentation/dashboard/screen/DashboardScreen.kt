package com.canineai.android.presentation.dashboard.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.theme.ColorSuccess
import com.canineai.android.presentation.theme.ColorInfo
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineLogo
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineEmptyState
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.dashboard.event.DashboardEvent
import com.canineai.android.presentation.dashboard.event.DashboardUiAction
import com.canineai.android.presentation.dashboard.event.QuickActionType
import com.canineai.android.presentation.dashboard.state.ActivityItem
import com.canineai.android.presentation.dashboard.state.ActivityType
import com.canineai.android.presentation.dashboard.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPatients: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val isDarkMode by com.canineai.android.presentation.theme.ThemeManager.isDarkMode.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showNotificationsDialog by remember { mutableStateOf(false) }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Clinical Notifications") },
            text = { Text("No unread system alerts. All CBCT segmentation jobs and reports are up to date.") },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiActions.collect { action ->
            when (action) {
                is DashboardUiAction.NavigateToPatients -> onNavigateToPatients()
                is DashboardUiAction.NavigateToUpload -> onNavigateToUpload()
                is DashboardUiAction.NavigateToHistory -> onNavigateToHistory()
                is DashboardUiAction.NavigateToSettings -> onNavigateToSettings()
                is DashboardUiAction.NavigateToReports -> onNavigateToReports()
                is DashboardUiAction.RouteBackToLogin -> onNavigateToLogin()
            }
        }
    }

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "dashboard",
        onNavigateToHome = { /* Already on Home */ },
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToAnalysis = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.UPLOAD_CBCT)) },
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = onNavigateToProfile,
        onLogout = onNavigateToLogin,
        clinicianName = state.doctorName.ifBlank { "Dr. Clinician" },
        clinicianRole = "Orthodontist"
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CanineLogo(modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CanineAI",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    },
                    navigationIcon = {
                        CanineIconButton(
                            icon = Icons.Default.Menu,
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            contentDescription = "Menu"
                        )
                    },
                    actions = {
                        CanineIconButton(icon = Icons.Default.Search, onClick = onNavigateToPatients, contentDescription = "Search")
                        BadgedBox(
                            badge = {
                                if (state.unreadNotificationsCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(text = state.unreadNotificationsCount.toString())
                                    }
                                }
                            }
                        ) {
                            CanineIconButton(icon = Icons.Default.Notifications, onClick = { showNotificationsDialog = true }, contentDescription = "Alerts")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                com.canineai.android.presentation.components.CanineBottomNavigationBar(
                    currentRoute = "dashboard",
                    onNavigateToHome = { /* Already on Home */ },
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Good Morning,",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = state.doctorName.ifBlank { "Clinician" },
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.organizationName,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = state.todayDate,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionItem(
                        label = "New Patient",
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.NEW_PATIENT)) }
                    )
                    QuickActionItem(
                        label = "Upload CBCT",
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.UPLOAD_CBCT)) }
                    )
                    QuickActionItem(
                        label = "Export PDF",
                        icon = Icons.Default.MailOutline,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.GENERATE_REPORT)) }
                    )
                    QuickActionItem(
                        label = "View History",
                        icon = Icons.Default.List,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.VIEW_HISTORY)) }
                    )
                }

                Text(
                    text = "Operations Overview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(title = "Total Patients", count = state.totalPatients?.toString() ?: "—", modifier = Modifier.weight(1f))
                    StatCard(title = "Today's Uploads", count = state.todayUploads?.toString() ?: "—", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(title = "Completed", count = state.completedAnalyses?.toString() ?: "—", modifier = Modifier.weight(1f))
                    StatCard(title = "Pending Queue", count = state.pendingAnalyses?.toString() ?: "—", modifier = Modifier.weight(1f))
                }

                Text(
                    text = "Clinical AI Engine",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ToothSeg Maxillary Segmentation Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Model Profile", style = MaterialTheme.typography.bodySmall)
                        Text(
                            state.aiModelName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Pipeline Status", style = MaterialTheme.typography.bodySmall)
                        CanineStatusChip(
                            text = "Analysis Ready",
                            status = CanineStatus.SUCCESS
                        )
                    }
                }

                Text(
                    text = "Recent Diagnostic Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (state.recentActivities.isEmpty()) {
                    CanineEmptyState(
                        title = "No Recent Activity",
                        message = "System diagnostic operations will appear here once scans are processed.",
                        icon = Icons.Default.List
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.recentActivities.forEach { item ->
                            ActivityRowItem(item = item)
                            if (item.id != state.recentActivities.last().id) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActivityRowItem(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = when (item.type) {
            ActivityType.UPLOAD -> Icons.Default.Share to MaterialTheme.colorScheme.primary
            ActivityType.ANALYSIS -> Icons.Default.Star to MaterialTheme.colorScheme.secondary
            ActivityType.REPORT -> Icons.Default.MailOutline to ColorSuccess
            ActivityType.PATIENT -> Icons.Default.Person to ColorInfo
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = item.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
