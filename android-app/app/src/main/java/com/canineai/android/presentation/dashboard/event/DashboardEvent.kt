package com.canineai.android.presentation.dashboard.event

sealed class DashboardEvent {
    object RefreshDashboard : DashboardEvent()
    data class QuickActionTriggered(val actionType: QuickActionType) : DashboardEvent()
    object LogoutRequested : DashboardEvent()
    object ClearAllNotifications : DashboardEvent()
}

enum class QuickActionType {
    NEW_PATIENT, UPLOAD_CBCT, GENERATE_REPORT, VIEW_HISTORY
}
