package com.canineai.android.presentation.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canineai.android.data.repository.CanineRepository
import com.canineai.android.presentation.notifications.event.NotificationEvent
import com.canineai.android.presentation.notifications.state.NotificationItem
import com.canineai.android.presentation.notifications.state.NotificationType
import com.canineai.android.presentation.notifications.state.TimelineEventItem
import com.canineai.android.presentation.notifications.state.TimelineFilter
import com.canineai.android.presentation.notifications.state.NotificationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: CanineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationState())
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    private var masterTimeline: List<TimelineEventItem> = emptyList()

    init {
        loadPersistedReportHistory()
    }

    private fun loadPersistedReportHistory() = viewModelScope.launch {
        try {
            val reports = repository.getReports()
            masterTimeline = reports.mapNotNull { report ->
                val id = report.id ?: return@mapNotNull null
                TimelineEventItem(
                    id = id,
                    action = "Report generated",
                    description = report.prediction ?: "Persisted clinical report",
                    timestamp = report.studyDate.orEmpty(),
                    user = "",
                    patientName = report.patientName.orEmpty(),
                    category = "REPORT"
                )
            }
            _state.update { it.copy(timelineEvents = masterTimeline, notifications = emptyList()) }
        } catch (e: Exception) {
            _state.update { it.copy(timelineEvents = emptyList(), notifications = emptyList()) }
        }
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.MarkAsRead -> markRead(event.id)
            is NotificationEvent.MarkAllAsRead -> markAllRead()
            is NotificationEvent.ClearNotification -> clearNotification(event.id)
            is NotificationEvent.ApplyTimelineFilter -> applyTimelineFilter(event.filter)
            is NotificationEvent.FilterByType -> filterByType(event.type)
            is NotificationEvent.SearchPatientTimeline -> searchPatient(event.query)
        }
    }

    private fun markRead(id: String) {
        _state.update { s ->
            s.copy(
                notifications = s.notifications.map { n ->
                    if (n.id == id) n.copy(isRead = true) else n
                }
            )
        }
    }

    private fun markAllRead() {
        _state.update { s ->
            s.copy(
                notifications = s.notifications.map { n -> n.copy(isRead = true) }
            )
        }
    }

    private fun clearNotification(id: String) {
        _state.update { s ->
            s.copy(
                notifications = s.notifications.filter { n -> n.id != id }
            )
        }
    }

    private fun applyTimelineFilter(filter: TimelineFilter) {
        _state.update { it.copy(selectedFilter = filter) }
        filterMasterTimeline()
    }

    private fun filterByType(type: NotificationType?) {
        _state.update { it.copy(selectedType = type) }
        filterMasterTimeline()
    }

    private fun searchPatient(query: String) {
        _state.update { it.copy(patientSearchQuery = query) }
        filterMasterTimeline()
    }

    private fun filterMasterTimeline() {
        val filter = _state.value.selectedFilter
        val query = _state.value.patientSearchQuery

        val filtered = masterTimeline.filter { item ->
            val matchesSearch = query.isBlank() || item.patientName.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true)
            val matchesTime = when (filter) {
                TimelineFilter.TODAY -> item.timestamp.contains("Today", ignoreCase = true)
                TimelineFilter.THIS_WEEK -> !item.timestamp.contains("2026-07-09")
                else -> true
            }
            matchesSearch && matchesTime
        }

        _state.update { it.copy(timelineEvents = filtered) }
    }
}
