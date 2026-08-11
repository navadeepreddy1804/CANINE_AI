package com.canineai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.canineai.android.presentation.navigation.Screen
import com.canineai.android.presentation.splash.screen.SplashScreen
import com.canineai.android.presentation.auth.screen.LoginScreen
import com.canineai.android.presentation.auth.screen.SignUpScreen
import com.canineai.android.presentation.auth.screen.CompleteProfileScreen
import com.canineai.android.presentation.dashboard.screen.DashboardScreen
import com.canineai.android.presentation.patients.screen.PatientListScreen
import com.canineai.android.presentation.patients.screen.PatientDetailsScreen
import com.canineai.android.presentation.patients.screen.AddPatientScreen
import com.canineai.android.presentation.patients.screen.EditPatientScreen
import com.canineai.android.presentation.upload.screen.UploadScreen
import com.canineai.android.presentation.upload.screen.CbctPreviewScreen
import com.canineai.android.presentation.analysis.screen.AnalysisScreen
import com.canineai.android.presentation.reports.screen.ReportsScreen
import com.canineai.android.presentation.reports.screen.ReportDetailsScreen
import com.canineai.android.presentation.settings.screen.SettingsScreen
import com.canineai.android.presentation.settings.screen.ProfileScreen
import com.canineai.android.presentation.settings.screen.AboutScreen
import com.canineai.android.presentation.splash.viewmodel.SplashViewModel
import com.canineai.android.presentation.auth.viewmodel.LoginViewModel
import com.canineai.android.presentation.auth.viewmodel.SignUpViewModel
import com.canineai.android.presentation.auth.viewmodel.CompleteProfileViewModel
import com.canineai.android.presentation.dashboard.viewmodel.DashboardViewModel
import com.canineai.android.presentation.patients.viewmodel.PatientViewModel
import com.canineai.android.presentation.upload.viewmodel.UploadViewModel
import com.canineai.android.presentation.analysis.viewmodel.AnalysisViewModel
import com.canineai.android.presentation.reports.viewmodel.ReportViewModel
import com.canineai.android.presentation.settings.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

import com.canineai.android.presentation.theme.CanineTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CanineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    val handleLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        composable(Screen.Splash.route) {
                            val splashViewModel: SplashViewModel = hiltViewModel()
                            SplashScreen(
                                viewModel = splashViewModel,
                                onNavigateToLogin = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                },
                                onNavigateToDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Login.route) {
                            val loginViewModel: LoginViewModel = hiltViewModel()
                            LoginScreen(
                                viewModel = loginViewModel,
                                onNavigateToDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToSignUp = {
                                    navController.navigate(Screen.SignUp.route)
                                },
                                onNavigateToCompleteProfile = {
                                    navController.navigate(Screen.CompleteProfile.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.CompleteProfile.route) {
                            val completeProfileViewModel: CompleteProfileViewModel = hiltViewModel()
                            CompleteProfileScreen(
                                viewModel = completeProfileViewModel,
                                onNavigateToDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.SignUp.route) {
                            val signUpViewModel: SignUpViewModel = hiltViewModel()
                            SignUpScreen(
                                viewModel = signUpViewModel,
                                onNavigateToDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.SignUp.route) { inclusive = true }
                                    }
                                },
                                onNavigateToSignIn = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.SignUp.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Dashboard.route) {
                            val dashboardViewModel: DashboardViewModel = hiltViewModel()
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onNavigateToLogin = handleLogout
                            )
                        }
                        composable(Screen.Patients.route) {
                            val patientViewModel: PatientViewModel = hiltViewModel()
                            PatientListScreen(
                                viewModel = patientViewModel,
                                onNavigateToDetails = { patientId ->
                                    navController.navigate(Screen.PatientDetails.routeFor(patientId))
                                },
                                onNavigateToAddPatient = { navController.navigate(Screen.AddPatient.route) },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.PatientDetails.route,
                            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                        ) { entry ->
                            val patientViewModel: PatientViewModel = hiltViewModel()
                            val patientId = requireNotNull(entry.arguments?.getString("patientId"))
                            PatientDetailsScreen(
                                patientId = patientId,
                                viewModel = patientViewModel,
                                onNavigateToEdit = { id -> navController.navigate(Screen.EditPatient.routeFor(id)) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.AddPatient.route) {
                            val patientViewModel: PatientViewModel = hiltViewModel()
                            AddPatientScreen(viewModel = patientViewModel, onNavigateBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.EditPatient.route,
                            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                        ) { entry ->
                            val patientViewModel: PatientViewModel = hiltViewModel()
                            EditPatientScreen(
                                patientId = requireNotNull(entry.arguments?.getString("patientId")),
                                viewModel = patientViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Upload.route) {
                            val uploadViewModel: UploadViewModel = hiltViewModel()
                            UploadScreen(
                                viewModel = uploadViewModel,
                                onNavigateToCbctPreview = { pId, sId ->
                                    navController.navigate(Screen.CbctPreview.routeFor(pId, sId))
                                },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onLogout = handleLogout,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.CbctPreview.route,
                            arguments = listOf(
                                navArgument("patientId") { type = NavType.StringType },
                                navArgument("studyId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val pId = entry.arguments?.getString("patientId").orEmpty()
                            val sId = entry.arguments?.getString("studyId").orEmpty()
                            CbctPreviewScreen(
                                patientId = pId,
                                studyId = sId,
                                patientName = "Selected Patient",
                                fileName = "CBCT Volume",
                                fileType = "CBCT Scan",
                                onStartAnalysis = { patientId, studyId ->
                                    navController.navigate(Screen.AnalysisWithArgs.routeFor(patientId, studyId))
                                },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onLogout = handleLogout,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Analysis.route) {
                            val analysisViewModel: AnalysisViewModel = hiltViewModel()
                            AnalysisScreen(
                                viewModel = analysisViewModel,
                                onNavigateToWorkspace = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onLogout = handleLogout,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.AnalysisWithArgs.route,
                            arguments = listOf(
                                navArgument("patientId") { type = NavType.StringType },
                                navArgument("studyId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val analysisViewModel: AnalysisViewModel = hiltViewModel()
                            val pId = entry.arguments?.getString("patientId").orEmpty()
                            val sId = entry.arguments?.getString("studyId").orEmpty()
                            AnalysisScreen(
                                viewModel = analysisViewModel,
                                patientId = pId,
                                studyId = sId,
                                onNavigateToWorkspace = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onLogout = handleLogout,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Reports.route) {
                            val reportViewModel: ReportViewModel = hiltViewModel()
                            ReportsScreen(
                                viewModel = reportViewModel,
                                onOpenReport = { reportId ->
                                    navController.navigate(Screen.ReportDetails.routeFor(reportId))
                                },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                            )
                        }
                        composable(
                            route = Screen.ReportDetails.route,
                            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                        ) { entry ->
                            val reportViewModel: ReportViewModel = hiltViewModel()
                            val reportId = requireNotNull(entry.arguments?.getString("reportId"))
                            ReportDetailsScreen(reportId = reportId, viewModel = reportViewModel)
                        }
                        composable(Screen.History.route) {
                            val historyViewModel: com.canineai.android.presentation.history.viewmodel.HistoryViewModel = hiltViewModel()
                            com.canineai.android.presentation.history.screen.HistoryScreen(
                                viewModel = historyViewModel,
                                onNavigateToReport = { reportId -> navController.navigate(Screen.ReportDetails.routeFor(reportId)) },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Settings.route) {
                            val settingsViewModel: SettingsViewModel = hiltViewModel()
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                                onNavigateToHome = { navController.navigate(Screen.Dashboard.route) },
                                onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Profile.route) {
                            val settingsViewModel: SettingsViewModel = hiltViewModel()
                            ProfileScreen(viewModel = settingsViewModel, onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Screen.About.route) {
                            AboutScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
