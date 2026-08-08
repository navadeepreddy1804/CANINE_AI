package com.canineai.android.presentation.dashboard.state

data class DashboardState(
    val doctorName: String = "",
    val todayDate: String = "",
    val organizationName: String = "",
    val isLoading: Boolean = false,
    
    // Statistics Counters
    val totalPatients: Int? = null,
    val todayUploads: Int? = null,
    val completedAnalyses: Int? = null,
    val pendingAnalyses: Int? = null,
    val reportsGenerated: Int? = null,
    
    // System Status
    val isAiEngineReady: Boolean = true,
    val aiModelName: String = "ToothSeg v1.2",
    val isGpuAvailable: Boolean = true,
    val isBackendConnected: Boolean = true,
    
    val isDatabaseHealthy: Boolean = true,
    val isStorageHealthy: Boolean = true,
    
    // Notifications
    val unreadNotificationsCount: Int = 0,
    val latestNotifications: List<NotificationItem> = emptyList(),
    
    // Recent Activity Logs
    val recentActivities: List<ActivityItem> = emptyList(),
    val apiError: String? = null
)

data class NotificationItem(
    val id: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean
)

data class ActivityItem(
    val id: String,
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val timestamp: String
)

enum class ActivityType {
    UPLOAD, ANALYSIS, REPORT, PATIENT
}
