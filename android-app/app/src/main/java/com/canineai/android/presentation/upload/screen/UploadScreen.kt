package com.canineai.android.presentation.upload.screen

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.upload.components.UploadProgressCard
import com.canineai.android.presentation.upload.event.UploadEvent
import com.canineai.android.presentation.upload.state.UploadStatus
import com.canineai.android.presentation.upload.viewmodel.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onNavigateToWorkspace: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        var displayName = uri.lastPathSegment ?: "CBCT study"
        var sizeBytes = 0L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { displayName = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { sizeBytes = cursor.getLong(it) }
            }
        }
        viewModel.onEvent(UploadEvent.FileSelected(displayName, formatFileSize(sizeBytes), uri))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prepare DICOM Study") },
                navigationIcon = {
                    CanineIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack, contentDescription = "Back")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Study selection surface. A completed study is validated by the backend.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                    .clickable {
                        filePicker.launch(arrayOf(
                            "application/dicom",
                            "application/zip",
                            "application/gzip",
                            "application/octet-stream"
                        ))
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Share, // Placement for Upload File
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select CBCT Image Study", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Choose a DICOM, NIfTI, MetaImage, NRRD, or ZIP study", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            // Supported format badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supported:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                listOf(".DCM", "DICOM Folders", ".NII", ".NII.GZ").forEach { format ->
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = format,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            state.apiError?.let { error ->
                CanineStatusChip(
                    text = error,
                    status = CanineStatus.ERROR,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onEvent(UploadEvent.DismissError) }
                )
            }

            // Display selection progress card
            if (state.fileName.isNotBlank()) {
                UploadProgressCard(
                    fileName = state.fileName,
                    fileSize = state.fileSize,
                    status = state.uploadState,
                    progress = state.progress,
                    onCancel = { viewModel.onEvent(UploadEvent.CancelCurrentUpload) },
                    onRetry = { viewModel.onEvent(UploadEvent.RetryCurrentUpload) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.uploadState == UploadStatus.IDLE && state.fileName.isNotBlank()) {
                    CanineButton(
                        text = "Trigger Upload Sequence",
                        onClick = { viewModel.onEvent(UploadEvent.TriggerUpload) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.uploadState == UploadStatus.COMPLETED) {
                    CanineButton(
                        text = "Open Workspace Platform",
                        onClick = onNavigateToWorkspace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L -> "Unknown size"
    bytes < 1024L * 1024L -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024f * 1024f))
}
