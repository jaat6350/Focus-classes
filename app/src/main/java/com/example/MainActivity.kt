package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddEditAssessmentScreen
import com.example.ui.screens.AddEditBatchScreen
import com.example.ui.screens.AddEditStudentScreen
import com.example.ui.screens.BatchStudentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.BatchesScreen
import com.example.ui.screens.EditMarksScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WeeklyAssessmentsScreen
import com.example.ui.theme.FocusClassesTheme
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusClassesTheme {
                val viewModel: FocusViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearUserMessage()
                    }
                }

                // Handle Hardware and Gesture Back Button
                BackHandler(enabled = currentScreen !is Screen.Dashboard && currentScreen !is Screen.Splash) {
                    viewModel.navigateBack()
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (val screen = currentScreen) {
                            is Screen.Splash -> {
                                SplashScreen(
                                    onEnterDashboard = {
                                        viewModel.replaceScreen(Screen.Dashboard)
                                    }
                                )
                            }
                            is Screen.Dashboard -> {
                                DashboardScreen(viewModel = viewModel)
                            }
                            is Screen.Batches -> {
                                BatchesScreen(viewModel = viewModel)
                            }
                            is Screen.AddEditBatch -> {
                                AddEditBatchScreen(
                                    batchId = screen.batchId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.BatchStudents -> {
                                BatchStudentsScreen(
                                    batchId = screen.batchId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.AddEditStudent -> {
                                AddEditStudentScreen(
                                    studentId = screen.studentId,
                                    defaultBatchId = screen.defaultBatchId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.StudentProfile -> {
                                com.example.ui.screens.StudentProfileScreen(
                                    studentId = screen.studentId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.WeeklyAssessments -> {
                                WeeklyAssessmentsScreen(
                                    batchId = screen.batchId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.AddEditAssessment -> {
                                AddEditAssessmentScreen(
                                    assessmentId = screen.assessmentId,
                                    defaultBatchId = screen.defaultBatchId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.MarksEntry -> {
                                EditMarksScreen(
                                    batchId = screen.batchId,
                                    assessmentId = screen.assessmentId,
                                    initialStudentId = screen.currentStudentId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.Leaderboard -> {
                                LeaderboardScreen(
                                    defaultBatchId = screen.defaultBatchId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.Attendance -> {
                                com.example.ui.screens.AttendanceScreen(
                                    batchId = screen.batchId,
                                    assessmentId = screen.assessmentId,
                                    viewModel = viewModel
                                )
                            }
                            is Screen.CustomCategories -> {
                                com.example.ui.screens.CustomCategoriesScreen(
                                    viewModel = viewModel
                                )
                            }
                            is Screen.Settings, is Screen.BackupRestore -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
