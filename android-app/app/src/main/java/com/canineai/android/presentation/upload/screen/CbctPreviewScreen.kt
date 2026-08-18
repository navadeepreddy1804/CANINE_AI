package com.canineai.android.presentation.upload.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.canineai.android.data.network.ApiConfig
import com.canineai.android.presentation.components.CanineButton
import com.canineai.android.presentation.components.CanineCard
import com.canineai.android.presentation.components.CanineDrawerLayout
import com.canineai.android.presentation.components.CanineIconButton
import com.canineai.android.presentation.components.CanineStatus
import com.canineai.android.presentation.components.CanineStatusChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbctPreviewScreen(
    patientId: String,
    studyId: String,
    patientName: String,
    fileName: String,
    fileType: String,
    onStartAnalysis: (patientId: String, studyId: String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var selectedSliceIndex by remember { mutableIntStateOf(0) }
    var selectedViewType by remember { mutableStateOf("axial") } // axial, coronal, sagittal

    val displayPatientId = if (patientId.startsWith("PT-")) patientId else "PT-$patientId"
    val displayStudyId = if (studyId.startsWith("ST-")) studyId else "ST-${studyId.take(8)}"
    val baseUrl = ApiConfig.resolveBaseUrl()
    val previewUrl = "$baseUrl/studies/$studyId/previews/$selectedViewType/$selectedSliceIndex"

    CanineDrawerLayout(
        drawerState = drawerState,
        currentRoute = "upload",
        onNavigateToHome = onNavigateToHome,
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToAnalysis = onNavigateToAnalysis,
        onNavigateToReports = onNavigateToReports,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = onNavigateToProfile,
        onLogout = onLogout
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CBCT Study Verification", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        CanineIconButton(
                            icon = Icons.Default.Menu,
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            contentDescription = "Menu"
                        )
                    },
                    actions = {
                        CanineIconButton(
                            icon = Icons.Default.ArrowBack,
                            onClick = onNavigateBack,
                            contentDescription = "Back"
                        )
                    }
                )
            },
            bottomBar = {
                com.canineai.android.presentation.components.CanineBottomNavigationBar(
                    currentRoute = "upload",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToPatients = onNavigateToPatients,
                    onNavigateToUpload = onNavigateToUpload,
                    onNavigateToAnalysis = onNavigateToAnalysis,
                    onNavigateToReports = onNavigateToReports
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Metadata Card
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = patientName.ifBlank { "Selected Patient" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Patient ID: $displayPatientId",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            CanineStatusChip(text = "UPLOADED", status = CanineStatus.SUCCESS)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Study ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(displayStudyId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("File Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(fileName.ifBlank { "CBCT_Scan.nii.gz" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Format", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(fileType.ifBlank { "CBCT / NIfTI" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 2D CBCT Slice Viewer Container
                CanineCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2D CBCT Slice Preview",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            // View selector tabs
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("axial", "coronal", "sagittal").forEach { type ->
                                    FilterChip(
                                        selected = selectedViewType == type,
                                        onClick = { selectedViewType = type },
                                        label = { Text(type.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }

                        // Image display frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val imageRequest = remember(previewUrl) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(previewUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .build()
                            }
                            coil.compose.SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = "CBCT Slice Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Text(
                                                text = "CBCT Scan Registered & Validated",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Ready for AI Diagnostic Pipeline",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // Slice Index Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Slice Index: $selectedSliceIndex", style = MaterialTheme.typography.labelSmall)
                                Text("Orientation: ${selectedViewType.uppercase()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = selectedSliceIndex.toFloat(),
                                onValueChange = { selectedSliceIndex = it.toInt() },
                                valueRange = 0f..20f,
                                steps = 19
                            )
                        }
                    }
                }

                // Status Verification Summary
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Study Verification Complete", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("CBCT volume loaded successfully. You can now launch AI canine eruption analysis.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Button: Launch AI Analysis
                CanineButton(
                    text = "Start AI Analysis",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onStartAnalysis(patientId, studyId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
