package com.canineai.android.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.theme.ColorSuccess
import com.canineai.android.presentation.settings.state.SettingsState

@Composable
fun ThemeSettings(
    state: SettingsState,
    onDarkModeToggled: (Boolean) -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Application Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Interface Dark Mode", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.isDarkMode, onCheckedChange = onDarkModeToggled)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Push Notifications Alerts", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.isNotificationsEnabled, onCheckedChange = onNotificationsToggled)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Default Locale Language", style = MaterialTheme.typography.bodyMedium)
            Text(state.language, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun AISettings(
    state: SettingsState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Clinical AI Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI Auto-localization Mode", style = MaterialTheme.typography.bodyMedium)
            Text("Maxillary Segment (Active)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Image Quality Checks", style = MaterialTheme.typography.bodyMedium)
            Text("Standard Validation", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun StorageSettings(
    state: SettingsState,
    onCleanClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUsed = listOfNotNull(state.studiesSizeGb, state.reportsSizeGb, state.logsSizeGb).takeIf { it.size == 3 }?.sum()
    val usedFraction = if (totalUsed != null && state.maxStorageGb != null && state.maxStorageGb > 0f) totalUsed / state.maxStorageGb else null

    CanineCard(modifier = modifier.fillMaxWidth()) {
        Text("Local Storage Status", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(totalUsed?.let { "${it.toInt()} GB used" } ?: "Usage unavailable", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(state.maxStorageGb?.let { "Limit: ${it.toInt()} GB" } ?: "No storage API", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { usedFraction ?: 0f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (totalUsed != null) CanineButton(text = "Clean Temporary Logs", onClick = onCleanClicked, type = CanineButtonType.OUTLINED, modifier = Modifier.fillMaxWidth())
    }
}
