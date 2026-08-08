package com.canineai.android.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.dashboard.event.DashboardEvent
import com.canineai.android.presentation.dashboard.event.DashboardUiAction
import com.canineai.android.presentation.dashboard.event.QuickActionType
import com.canineai.android.presentation.dashboard.state.ActivityItem
import com.canineai.android.presentation.dashboard.state.ActivityType
import com.canineai.android.presentation.dashboard.state.DashboardState
import com.canineai.android.presentation.dashboard.state.NotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CanineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _uiActions = Channel<DashboardUiAction>()
    val uiActions = _uiActions.receiveAsFlow()

    init {
        loadDashboardData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.RefreshDashboard -> loadDashboardData()
            is DashboardEvent.QuickActionTriggered -> handleQuickAction(event.actionType)
            is DashboardEvent.LogoutRequested -> handleLogout()
            is DashboardEvent.ClearAllNotifications -> clearNotifications()
        }
    }

    private fun loadDashboardData() {
        _state.update { it.copy(isLoading = true, apiError = null) }
        viewModelScope.launch {
            try {
                val user = repository.getCurrentUser()
                val stats = repository.getDashboardStats()
                
                val mappedActivities = stats.activities.map { act ->
                    val type = when (act.type.lowercase()) {
                        "upload" -> ActivityType.UPLOAD
                        "analysis" -> ActivityType.ANALYSIS
                        "report" -> ActivityType.REPORT
                        else -> ActivityType.PATIENT
                    }
                    ActivityItem(act.id, type, act.title, act.subtitle, act.time)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        doctorName = user.fullName.orEmpty(),
                        organizationName = user.hospital.orEmpty(),
                        totalPatients = stats.totalPatients,
                        todayUploads = stats.completedUploads.toInt(),
                        completedAnalyses = stats.totalReports,
                        pendingAnalyses = stats.retrainingQueue,
                        reportsGenerated = stats.totalReports.toInt(),
                        recentActivities = mappedActivities,
                        latestNotifications = emptyList(),
                        unreadNotificationsCount = 0
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, apiError = "Dashboard data is unavailable from the backend.") }
            }
        }
    }

    private fun handleQuickAction(type: QuickActionType) {
        viewModelScope.launch {
            when (type) {
                QuickActionType.NEW_PATIENT -> _uiActions.send(DashboardUiAction.NavigateToPatients)
                QuickActionType.UPLOAD_CBCT -> _uiActions.send(DashboardUiAction.NavigateToUpload)
                QuickActionType.GENERATE_REPORT -> _uiActions.send(DashboardUiAction.NavigateToReports)
                QuickActionType.VIEW_HISTORY -> _uiActions.send(DashboardUiAction.NavigateToHistory)
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            repository.logout()
            _uiActions.send(DashboardUiAction.RouteBackToLogin)
        }
    }

    private fun clearNotifications() {
        _state.update { it.copy(unreadNotificationsCount = 0, latestNotifications = emptyList()) }
    }
}
