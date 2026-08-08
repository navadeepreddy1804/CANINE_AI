package com.canineai.android.presentation.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineDialog
import com.canineai.android.presentation.settings.components.AISettings
import com.canineai.android.presentation.settings.components.StorageSettings
import com.canineai.android.presentation.settings.components.ThemeSettings
import com.canineai.android.presentation.settings.event.SettingsEvent
import com.canineai.android.presentation.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Admin Settings") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
                },
                actions = {
                    CanineIconButton(icon = Icons.Default.Person, onClick = onNavigateToProfile, contentDescription = "Profile")
                    CanineIconButton(icon = Icons.Default.Info, onClick = onNavigateToAbout, contentDescription = "About")
                }
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
            // Theme settings config card
            ThemeSettings(
                state = state,
                onDarkModeToggled = { viewModel.onEvent(SettingsEvent.DarkModeToggled(it)) },
                onNotificationsToggled = { viewModel.onEvent(SettingsEvent.NotificationsToggled(it)) }
            )

            // AI Inference Configs card
            AISettings(state = state)

            // Storage quota progress card
            StorageSettings(
                state = state,
                onCleanClicked = { viewModel.onEvent(SettingsEvent.CleanTemporaryFiles) }
            )
        }
    }
}
