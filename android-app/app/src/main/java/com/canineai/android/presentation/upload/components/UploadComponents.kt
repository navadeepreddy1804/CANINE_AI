package com.canineai.android.presentation.upload.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.upload.state.DicomMetadata
import com.canineai.android.presentation.upload.state.DicomTagItem
import com.canineai.android.presentation.upload.state.UploadStatus

@Composable
fun UploadProgressCard(
    fileName: String,
    fileSize: String,
    status: UploadStatus,
    progress: Float,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    CanineCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = fileName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                Text(text = fileSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            CanineStatusChip(
                text = status.name,
                status = when (status) {
                    UploadStatus.COMPLETED -> CanineStatus.SUCCESS
                    UploadStatus.FAILED -> CanineStatus.ERROR
                    UploadStatus.UPLOADING -> CanineStatus.INFO
                    UploadStatus.VALIDATING -> CanineStatus.WARNING
                    else -> CanineStatus.INFO
                }
            )
        }

        if (status == UploadStatus.UPLOADING || status == UploadStatus.VALIDATING) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (status == UploadStatus.UPLOADING) "${(progress * 100).toInt()}% uploaded" else "Reading header metadata tags...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                    CanineButton(
                        text = "Cancel",
                        onClick = onCancel,
                        type = CanineButtonType.TEXT
                    )
            }
        }

        if (status == UploadStatus.FAILED || status == UploadStatus.CANCELLED) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                    CanineButton(
                        text = "Retry Upload",
                        onClick = onRetry,
                        type = CanineButtonType.OUTLINED
                    )
            }
        }
    }
}

@Composable
fun DicomMetadataCard(
    metadata: DicomMetadata,
    modifier: Modifier = Modifier
) {
    val tags = listOf(
        DicomTagItem("(0010,0020)", "Patient ID", "LO", metadata.patientId),
        DicomTagItem("(0010,0010)", "Patient Name", "PN", metadata.patientName),
        DicomTagItem("(0020,000D)", "Study Instance UID", "UI", metadata.studyUid.take(24) + "..."),
        DicomTagItem("(0020,000E)", "Series Instance UID", "UI", metadata.seriesUid.take(24) + "..."),
        DicomTagItem("(0008,0060)", "Modality", "CS", metadata.modality),
        DicomTagItem("(0008,0070)", "Manufacturer", "LO", metadata.manufacturer),
        DicomTagItem("(0028,0010)", "Rows x Columns", "US", metadata.dimensions),
        DicomTagItem("(0018,0050)", "Slice Thickness", "DS", metadata.sliceThickness),
        DicomTagItem("(0018,1100)", "Reconstruction Diameter", "DS", metadata.voxelSize),
        DicomTagItem("(0008,0020)", "Study Date", "DA", metadata.studyDate),
        DicomTagItem("(0008,1030)", "Study Description", "LO", metadata.studyDescription)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DICOM Header Tags", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("VR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }

        tags.forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tag.groupElement,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(90.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = tag.tagDescription, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = tag.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Text(
                    text = tag.vr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            if (tag != tags.last()) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun DicomThumbnailGrid(
    sliceCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Reconstructed 3D Slices (${sliceCount} total)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(9) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulating a DICOM slice segment frame
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Slice #${index * 40 + 1}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AXIAL",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
