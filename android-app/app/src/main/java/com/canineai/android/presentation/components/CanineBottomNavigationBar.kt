package com.canineai.android.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CanineBottomNavigationBar(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "dashboard" || currentRoute == "home",
            onClick = onNavigateToHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", maxLines = 1, overflow = TextOverflow.Clip, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = currentRoute.startsWith("patients"),
            onClick = onNavigateToPatients,
            icon = { Icon(Icons.Default.Person, contentDescription = "Patients") },
            label = { Text("Patients", maxLines = 1, overflow = TextOverflow.Clip, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = currentRoute == "upload",
            onClick = onNavigateToUpload,
            icon = { Icon(Icons.Default.Share, contentDescription = "Upload") },
            label = { Text("Upload", maxLines = 1, overflow = TextOverflow.Clip, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = currentRoute.startsWith("analysis"),
            onClick = onNavigateToAnalysis,
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "AI Analysis") },
            label = { Text("AI Analysis", maxLines = 1, overflow = TextOverflow.Clip, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = currentRoute.startsWith("reports"),
            onClick = onNavigateToReports,
            icon = { Icon(Icons.Default.List, contentDescription = "Reports") },
            label = { Text("Reports", maxLines = 1, overflow = TextOverflow.Clip, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
    }
}
