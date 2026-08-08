package com.canineai.android.presentation.dashboard.event

sealed class DashboardUiAction {
    object NavigateToPatients : DashboardUiAction()
    object NavigateToUpload : DashboardUiAction()
    object NavigateToHistory : DashboardUiAction()
    object NavigateToSettings : DashboardUiAction()
    object NavigateToReports : DashboardUiAction()
    object RouteBackToLogin : DashboardUiAction()
}
