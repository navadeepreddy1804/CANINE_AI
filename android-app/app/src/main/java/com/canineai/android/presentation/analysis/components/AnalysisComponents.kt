package com.canineai.android.presentation.analysis.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineStatusChip
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineButtonType
import com.canineai.android.presentation.analysis.state.AnalysisState
import com.canineai.android.presentation.analysis.state.PipelineStage
import com.canineai.android.presentation.components.CanineCircularLoader

@Composable
fun AnalysisProgressCard(
    state: AnalysisState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CanineCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.pipelineStage.description,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Timer: ${state.elapsedTime} (Remaining: ${state.estimatedRemaining})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (state.isRunning) {
                CanineCircularLoader()
            } else if (state.isComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )

        if (state.isRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CanineButton(
                    text = "Halt Analysis",
                    onClick = onCancel,
                    type = CanineButtonType.TEXT,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                )
            }
        } else if (!state.isComplete) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CanineButton(
                    text = "Start Analysis",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StudyCbctViewer(
    state: AnalysisState,
    onSliceIndexChanged: (Int) -> Unit,
    onToggleCanine: () -> Unit,
    onFocusCanine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSlices = if (state.totalSliceCount > 0) state.totalSliceCount else 12
    val safeSliceIndex = state.currentSliceIndex.coerceIn(0, totalSlices - 1)
    val displaySliceNumber = safeSliceIndex + 1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ToothSeg CBCT Viewer",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Slice $displaySliceNumber of $totalSlices",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(MaterialTheme.shapes.large)
                .background(Color(0xFF090D16))
                .border(1.dp, Color(0xFF1E293B), MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center
        ) {
            val baseUrl = com.canineai.android.data.network.ApiConfig.resolveBaseUrl().removeSuffix("/")
            val imageUrl = "$baseUrl/studies/${state.studyId}/previews/axial/$safeSliceIndex"
            
            AsyncImage(
                model = imageUrl,
                contentDescription = "CBCT Slice",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            
            // ToothSeg Prediction Overlay
            val hasCanineOnSlice = state.boundingBoxSliceIndex == safeSliceIndex
            if (state.isCanineHighlighted && hasCanineOnSlice && state.boundingBoxWidth > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 512f
                    val scaleY = size.height / 512f
                    
                    val rectWidth = state.boundingBoxWidth * scaleX
                    val rectHeight = state.boundingBoxHeight * scaleY
                    
                    val topLeft = Offset(
                        x = state.boundingBoxX * scaleX,
                        y = state.boundingBoxY * scaleY
                    )
                    
                    // Semi-transparent mask
                    drawRect(
                        color = Color(0xFFEF4444).copy(alpha = 0.3f),
                        topLeft = topLeft,
                        size = Size(rectWidth, rectHeight)
                    )
                    
                    // Solid bounding border
                    drawRect(
                        color = Color(0xFFEF4444),
                        topLeft = topLeft,
                        size = Size(rectWidth, rectHeight),
                        style = Stroke(width = 4f)
                    )
                }

                // Canine tag badge on top of overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xCCDC2626), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FDI ${state.canineFdi} • ${state.canineToothName}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (state.isCanineHighlighted && state.boundingBoxSliceIndex != null && state.boundingBoxSliceIndex != safeSliceIndex) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xFF334155).copy(alpha = 0.9f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Canine on Slice ${(state.boundingBoxSliceIndex ?: 0) + 1}",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Axial Slices",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val maxSliceRange = (if (totalSlices > 1) totalSlices - 1 else 1).toFloat()
                Slider(
                    value = safeSliceIndex.toFloat(),
                    onValueChange = { onSliceIndexChanged(it.toInt()) },
                    valueRange = 0f..maxSliceRange,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.boundingBoxSliceIndex != null) {
                CanineButton(
                    text = "Focus Canine (Slice ${(state.boundingBoxSliceIndex ?: 0) + 1})",
                    onClick = onFocusCanine,
                    type = CanineButtonType.OUTLINED,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                )
            }
        }
    }
}

@Composable
fun ToothSegFindingsCard(
    state: AnalysisState,
    modifier: Modifier = Modifier
) {
    val formattedCaseId = com.canineai.android.util.PredictionFormatter.formatCaseId(state.studyId)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Section 1: ToothSeg Findings",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            CanineStatusChip(
                text = "ToothSeg v2.1",
                status = CanineStatus.SUCCESS
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        ResultMetricRow(label = "Case ID", value = formattedCaseId)
        ResultMetricRow(label = "Canine Identified", value = state.canineToothName)
        ResultMetricRow(label = "FDI Tooth Label", value = "FDI ${state.canineFdi}")
        ResultMetricRow(label = "Quadrant / Sector", value = state.canineSector)
        ResultMetricRow(label = "Canine Volume", value = "${state.canineVolumeMm3} mm³")
        ResultMetricRow(label = "3D PCA Angulation", value = "${state.canineAngulation}°")
        ResultMetricRow(label = "3D Centroid (X,Y,Z)", value = state.canineCentroid)
        ResultMetricRow(
            label = "Segmented Teeth",
            value = "${state.totalTeethCount} Total (${state.detectedUpperTeethCount} Upper, ${state.detectedLowerTeethCount} Lower)"
        )
    }
}

@Composable
fun ClinicalDiagnosticCard(
    state: AnalysisState,
    modifier: Modifier = Modifier
) {
    val formattedPrediction = com.canineai.android.util.PredictionFormatter.formatPrediction(state.clinicalDiagnosis)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF0FDF4), shape = MaterialTheme.shapes.large)
            .border(1.dp, Color(0xFFBBF7D0), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Section 2: Diagnostic Assessment",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF15803D)
            )
            CanineStatusChip(
                text = "${(state.diagnosticConfidence * 100).toInt()}% CONF",
                status = CanineStatus.SUCCESS
            )
        }

        HorizontalDivider(color = Color(0xFFBBF7D0))

        ResultMetricRow(label = "Diagnostic Prediction", value = formattedPrediction)
        ResultMetricRow(label = "Eruption Trajectory", value = state.eruptionDirection)
        ResultMetricRow(label = "Root Resorption Risk", value = state.rootResorptionRisk)
        ResultMetricRow(label = "Surgical Difficulty", value = state.surgicalDifficulty)

        // Recommendation Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "Clinical Recommendation",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.clinicalRecommendation,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ResultMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ModelStatusCard(
    state: AnalysisState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Clinical AI Engine Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        ModelAttributeRow(label = "Segmentation Engine", value = "ToothSeg v2.1 (nnUNet)")
        ModelAttributeRow(label = "Diagnostic Engine", value = "ToothSeg Clinical AI (Real Morphometry)")
        ModelAttributeRow(label = "Clinical Standard", value = "FDI World Dental Federation")
        ModelAttributeRow(label = "Data Protection", value = "HIPAA Compliant Session")
    }
}

@Composable
private fun ModelAttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun PipelineProgress(
    currentStage: PipelineStage,
    modifier: Modifier = Modifier
) {
    val stages = PipelineStage.values().filter { it != PipelineStage.COMPLETE }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Pipeline Milestones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        stages.forEachIndexed { _, stage ->
            val isPassed = stage.ordinal < currentStage.ordinal
            val isActive = stage == currentStage
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isPassed -> Color(0xFF22C55E)
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stage.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
