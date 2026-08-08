package com.canineai.android.presentation.notifications.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.notifications.event.NotificationEvent
import com.canineai.android.presentation.notifications.state.NotificationItem
import com.canineai.android.presentation.notifications.state.NotificationType
import com.canineai.android.presentation.notifications.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Alert Center") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
                },
                actions = {
                    CanineIconButton(icon = Icons.Default.CheckCircle, onClick = { viewModel.onEvent(NotificationEvent.MarkAllAsRead) }, contentDescription = "Mark all as read")
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No notifications active.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.notifications) { item ->
                    NotificationRow(
                        item = item,
                        onClick = { viewModel.onEvent(NotificationEvent.MarkAsRead(item.id)) },
                        onDismiss = { viewModel.onEvent(NotificationEvent.ClearNotification(item.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    CanineCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CanineStatusChip(
                    text = item.type.name,
                    status = when (item.type) {
                        NotificationType.AI -> CanineStatus.SUCCESS
                        NotificationType.UPLOAD -> CanineStatus.INFO
                        NotificationType.REPORT -> CanineStatus.WARNING
                        NotificationType.SYSTEM -> CanineStatus.ERROR
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                CanineIconButton(icon = Icons.Default.Close, onClick = onDismiss, contentDescription = "Dismiss", modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = item.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = item.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
