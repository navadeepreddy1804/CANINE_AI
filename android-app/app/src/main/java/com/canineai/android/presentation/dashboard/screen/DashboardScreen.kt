package com.canineai.android.presentation.dashboard.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.theme.ColorSuccess
import com.canineai.android.presentation.theme.ColorInfo
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineLogo
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineEmptyState
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.dashboard.event.DashboardEvent
import com.canineai.android.presentation.dashboard.event.DashboardUiAction
import com.canineai.android.presentation.dashboard.event.QuickActionType
import com.canineai.android.presentation.dashboard.state.ActivityItem
import com.canineai.android.presentation.dashboard.state.ActivityType
import com.canineai.android.presentation.dashboard.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPatients: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Listen to UI side effects
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CanineLogo(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CanineAI",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                },
                actions = {
                    // Search Action Icon
                    CanineIconButton(icon = Icons.Default.Search, onClick = {}, contentDescription = "Search Scans")
                    // Notifications badge icon
                    BadgedBox(
                        badge = {
                            if (state.unreadNotificationsCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(text = state.unreadNotificationsCount.toString())
                                }
                            }
                        }
                    ) {
                        CanineIconButton(icon = Icons.Default.Notifications, onClick = {}, contentDescription = "Alerts")
                    }
                    // Avatar Layout Profile menu
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .clickable { /* Profile details */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.doctorName.take(1).ifBlank { "?" },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Sign out trigger
                    CanineIconButton(icon = Icons.Default.ExitToApp, onClick = { viewModel.onEvent(DashboardEvent.LogoutRequested) }, contentDescription = "Logout")
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Stay on Dashboard */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.NEW_PATIENT)) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Patients") },
                    label = { Text("Patients") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.UPLOAD_CBCT)) },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Upload") },
                    label = { Text("Upload") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.VIEW_HISTORY)) },
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.onEvent(DashboardEvent.QuickActionTriggered(QuickActionType.VIEW_HISTORY)) }, // Settings fallback
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
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
            // Welcome doctor Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Good Morning,",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = state.doctorName.ifBlank { "Clinician" },
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
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

            // Section Headline: Quick Actions shortcuts
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

            // Section Headline: Stat counters
            Text(
                text = "Operations Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Dynamic grid layouts (row-based lists of counters)
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

            // AI Pipeline Health Status Cards
            Text(
                text = "Clinical AI Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            CanineCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Medical Segmentation Engine",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Model Profile", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        state.aiModelName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Inference Engine", style = MaterialTheme.typography.bodyMedium)
                    CanineStatusChip(
                        text = "Analysis Ready",
                        status = CanineStatus.SUCCESS
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clinical Connection Security", style = MaterialTheme.typography.bodyMedium)
                    CanineStatusChip(
                        text = "Secure Connection",
                        status = CanineStatus.SUCCESS
                    )
                }
            }

            // Section Headline: Recent activities log tracker
            Text(
                text = "Recent Diagnostic Log",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.recentActivities.isEmpty()) {
                    CanineEmptyState(
                        title = "No Recent Activity",
                        message = "All recent system diagnostic operations will appear here once scans are processed.",
                        icon = Icons.Default.List
                    )
                } else {
                    state.recentActivities.forEach { item ->
                        ActivityRowItem(item = item)
                        if (item.id != state.recentActivities.last().id) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp))
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
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
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
                .size(36.dp)
                .background(color.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

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
