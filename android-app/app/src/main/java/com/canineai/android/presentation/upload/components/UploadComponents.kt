package com.canineai.android.presentation.upload.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
            Text(
                text = "Upload failed. The CBCT scan could not be processed by the server.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                CanineButton(
                    text = "Choose Another File",
                    onClick = onCancel,
                    type = CanineButtonType.TEXT
                )
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
fun CbctClinicalSliceViewer(
    studyId: String? = null,
    totalSlices: Int = 360,
    modifier: Modifier = Modifier
) {
    var currentSlice by remember { mutableIntStateOf(totalSlices / 2) }
    var currentPlane by remember { mutableStateOf("AXIAL") }
    val planes = listOf("AXIAL", "CORONAL", "SAGITTAL")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Plane selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CBCT PREVIEW SLICES",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                planes.forEach { plane ->
                    val isSelected = plane == currentPlane
                    FilterChip(
                        selected = isSelected,
                        onClick = { currentPlane = plane },
                        label = { Text(plane, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        val baseUrl = com.canineai.android.data.network.ApiConfig.resolveBaseUrl().removeSuffix("/")
        val sliceUrl = if (!studyId.isNullOrBlank()) {
            "$baseUrl/studies/$studyId/previews/${currentPlane.lowercase()}/$currentSlice"
        } else ""

        // Clinical Viewport Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF070B14)) // Deep clinical dark viewport
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            if (sliceUrl.isNotBlank()) {
                coil.compose.SubcomposeAsyncImage(
                    model = sliceUrl,
                    contentDescription = "CBCT Slice Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    error = {
                        CbctDentalAnatomicalCanvas(currentPlane = currentPlane, currentSlice = currentSlice)
                    },
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            com.canineai.android.presentation.components.CanineCircularLoader()
                        }
                    }
                )
            } else {
                CbctDentalAnatomicalCanvas(currentPlane = currentPlane, currentSlice = currentSlice)
            }

            // Orientation Annotations
            Text("A", color = Color.Cyan.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
            Text("P", color = Color.Cyan.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
            Text("R", color = Color.Cyan.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
            Text("L", color = Color.Cyan.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))

            // Window Level metadata overlay
            Text(
                text = "W: 2000 L: 400 | Slice #$currentSlice | 0.3mm",
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )

            Text(
                text = "CanineAI Engine • $currentPlane",
                color = Color.Cyan.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }

        // Slider & Step Controls Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slice $currentSlice / $totalSlices",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Position: ${(currentSlice * 0.3).let { String.format("%.1f mm", it) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Slider(
                value = currentSlice.toFloat(),
                onValueChange = { currentSlice = it.toInt().coerceIn(1, totalSlices) },
                valueRange = 1f..totalSlices.toFloat(),
                steps = 0,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CanineButton(
                    text = "← Previous Slice",
                    onClick = { if (currentSlice > 1) currentSlice-- },
                    type = CanineButtonType.OUTLINED,
                    enabled = currentSlice > 1,
                    modifier = Modifier.weight(1f)
                )
                CanineButton(
                    text = "Next Slice →",
                    onClick = { if (currentSlice < totalSlices) currentSlice++ },
                    type = CanineButtonType.OUTLINED,
                    enabled = currentSlice < totalSlices,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CbctDentalAnatomicalCanvas(
    currentPlane: String,
    currentSlice: Int,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Draw dark background
        drawRect(Color(0xFF090E1A))

        // Draw anatomical dental arch curve
        val arcPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.75f)
            cubicTo(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.25f, w * 0.75f, h * 0.75f)
        }

        // Outer cortical bone contour
        drawPath(
            path = arcPath,
            color = Color.LightGray.copy(alpha = 0.45f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 18f)
        )

        // Inner trabecular bone structure
        drawPath(
            path = arcPath,
            color = Color.White.copy(alpha = 0.75f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
        )

        // Tooth buds / dental arch elements
        val toothPositions = listOf(
            Offset(w * 0.28f, h * 0.68f),
            Offset(w * 0.35f, h * 0.48f),
            Offset(w * 0.43f, h * 0.35f), // Canine tooth impaction target
            Offset(w * 0.50f, h * 0.32f),
            Offset(w * 0.57f, h * 0.35f),
            Offset(w * 0.65f, h * 0.48f),
            Offset(w * 0.72f, h * 0.68f)
        )

        toothPositions.forEachIndexed { index, pos ->
            val isCanine = index == 2
            val color = if (isCanine) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f)
            val radius = if (isCanine) 12f else 9f
            drawCircle(color = color, radius = radius, center = pos)
            drawCircle(color = Color.White, radius = radius * 0.5f, center = pos)
        }

        // Crosshairs
        drawLine(
            color = Color.Cyan.copy(alpha = 0.35f),
            start = Offset(cx, 0f),
            end = Offset(cx, h),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.Cyan.copy(alpha = 0.35f),
            start = Offset(0f, cy),
            end = Offset(w, cy),
            strokeWidth = 1.5f
        )
    }
}
