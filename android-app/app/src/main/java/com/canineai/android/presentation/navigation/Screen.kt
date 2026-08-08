package com.canineai.android.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object CompleteProfile : Screen("complete_profile")
    object Dashboard : Screen("dashboard")
    object Patients : Screen("patients")
    object PatientDetails : Screen("patients/{patientId}") {
        fun routeFor(patientId: String) = "patients/$patientId"
    }
    object AddPatient : Screen("patients/new")
    object EditPatient : Screen("patients/{patientId}/edit") {
        fun routeFor(patientId: String) = "patients/$patientId/edit"
    }
    object Upload : Screen("upload")
    object Analysis : Screen("analysis")
    object Reports : Screen("reports")
    object ReportDetails : Screen("reports/{reportId}") {
        fun routeFor(reportId: String) = "reports/$reportId"
    }
    object Settings : Screen("settings")
    object Profile : Screen("settings/profile")
    object About : Screen("settings/about")
}
