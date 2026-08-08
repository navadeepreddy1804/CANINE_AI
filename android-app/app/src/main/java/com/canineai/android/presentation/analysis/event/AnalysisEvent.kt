package com.canineai.android.presentation.analysis.event

sealed class AnalysisEvent {
    object StartAnalysis : AnalysisEvent()
    object CancelAnalysis : AnalysisEvent()
    object RestartAnalysis : AnalysisEvent()
    data class SlideSliceIndex(val index: Int) : AnalysisEvent()
    object ToggleCanineHighlight : AnalysisEvent()
    object FocusCanineSlice : AnalysisEvent()
    data class SwitchWorkspaceTab(val index: Int) : AnalysisEvent()
    object GenerateReportRequested : AnalysisEvent()
    object DismissError : AnalysisEvent()
}
