package com.canineai.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavigationDrawerItemData(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanineDrawerLayout(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    clinicianName: String = "Dr. Clinician",
    clinicianRole: String = "Orthodontist",
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = clinicianName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = clinicianRole,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val menuItems = listOf(
                    NavigationDrawerItemData("Dashboard / Home", Icons.Default.Home, "dashboard", onNavigateToHome),
                    NavigationDrawerItemData("Patients", Icons.Default.Person, "patients", onNavigateToPatients),
                    NavigationDrawerItemData("Upload CBCT", Icons.Default.Share, "upload", onNavigateToUpload),
                    NavigationDrawerItemData("AI Analysis", Icons.Default.PlayArrow, "analysis", onNavigateToAnalysis),
                    NavigationDrawerItemData("Reports", Icons.Default.List, "reports", onNavigateToReports),
                    NavigationDrawerItemData("Diagnostic History", Icons.Default.Refresh, "history", onNavigateToHistory),
                    NavigationDrawerItemData("Settings", Icons.Default.Settings, "settings", onNavigateToSettings),
                    NavigationDrawerItemData("Profile", Icons.Default.AccountCircle, "settings/profile", onNavigateToProfile)
                )

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                        selected = currentRoute == item.route || currentRoute.startsWith(item.route),
                        onClick = item.onClick,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        content = content
    )
}
