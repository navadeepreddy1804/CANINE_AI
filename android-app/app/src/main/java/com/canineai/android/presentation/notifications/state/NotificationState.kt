package com.canineai.android.presentation.notifications.state

data class NotificationState(
    val notifications: List<NotificationItem> = emptyList(),
    val timelineEvents: List<TimelineEventItem> = emptyList(),
    
    // Filters configurations
    val selectedFilter: TimelineFilter = TimelineFilter.ALL,
    val selectedType: NotificationType? = null,
    val patientSearchQuery: String = ""
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean,
    val type: NotificationType
)

enum class NotificationType {
    SYSTEM, AI, UPLOAD, REPORT
}

data class TimelineEventItem(
    val id: String,
    val action: String,
    val description: String,
    val timestamp: String,
    val user: String,
    val patientName: String,
    val category: String
)

enum class TimelineFilter {
    TODAY, THIS_WEEK, THIS_MONTH, ALL
}
