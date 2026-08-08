package com.canineai.android.presentation.notifications.event

import com.canineai.android.presentation.notifications.state.NotificationType
import com.canineai.android.presentation.notifications.state.TimelineFilter

sealed class NotificationEvent {
    data class MarkAsRead(val id: String) : NotificationEvent()
    object MarkAllAsRead : NotificationEvent()
    data class ClearNotification(val id: String) : NotificationEvent()
    
    // Timeline Filter events
    data class ApplyTimelineFilter(val filter: TimelineFilter) : NotificationEvent()
    data class FilterByType(val type: NotificationType?) : NotificationEvent()
    data class SearchPatientTimeline(val query: String) : NotificationEvent()
}
