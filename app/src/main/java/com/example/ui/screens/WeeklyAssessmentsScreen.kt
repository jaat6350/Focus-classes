package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeeklyAssessmentEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusNavy
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.AttendanceDateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AssessmentTab(val title: String) {
    ALL("All"),
    CURRENT("Current Week"),
    COMPLETED("Completed"),
    INCOMPLETE("Incomplete")
}

@Composable
fun WeeklyAssessmentsScreen(
    batchId: Long,
    viewModel: FocusViewModel
) {
    LaunchedEffect(batchId) {
        if (batchId > 0) {
            viewModel.setBatch(batchId)
            // Automatically ensure the Monday to Saturday assessment exists for current week!
            viewModel.ensureCurrentWeekAssessmentForBatch(batchId)
        }
    }

    val batch by viewModel.currentBatch.collectAsState()
    val assessments by viewModel.assessmentsForBatch.collectAsState()
    val students by viewModel.studentsInBatch.collectAsState()

    var selectedTab by remember { mutableStateOf(AssessmentTab.ALL) }
    var assessmentToDelete by remember { mutableStateOf<WeeklyAssessmentEntity?>(null) }

    val batchTitle = batch?.name ?: "Batch $batchId"
    val now = System.currentTimeMillis()

    // Filter assessments by tab
    val filteredAssessments = remember(assessments, selectedTab) {
        when (selectedTab) {
            AssessmentTab.ALL -> assessments
            AssessmentTab.CURRENT -> assessments.filter {
                now >= it.startDate && now <= it.endDate
            }.ifEmpty {
                assessments.take(1)
            }
            AssessmentTab.COMPLETED -> assessments.filter {
                // Completed if week has ended
                now > it.endDate
            }
            AssessmentTab.INCOMPLETE -> assessments.filter {
                now <= it.endDate
            }
        }
    }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "$batchTitle Assessments",
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.AddEditAssessment(0, batchId)) },
                        modifier = Modifier.testTag("add_assessment_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Assessment", tint = Color.White)
                    }
                }
            )
        },
        containerColor = FocusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("weekly_assessments_screen_root")
        ) {
            // Auto Assessment Banner
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F1FC))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = FocusPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Weekly assessments (Monday–Saturday) are created automatically for each batch. Attendance marks sync in real-time.",
                            fontSize = 12.sp,
                            color = FocusPrimaryBlue,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AssessmentTab.values()) { tab ->
                            val isSelected = selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FocusPrimaryBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = FocusTextDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("assessment_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Assessment List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredAssessments.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = FocusPrimaryBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (selectedTab == AssessmentTab.ALL) "No assessments created yet." else "No assessments under ${selectedTab.title}.",
                                    fontSize = 15.sp,
                                    color = FocusTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.ensureCurrentWeekAssessmentForBatch(batchId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Current Week")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredAssessments, key = { it.id }) { assessment ->
                        val isCurrentWeek = now >= assessment.startDate && now <= assessment.endDate

                        AssessmentCard(
                            assessment = assessment,
                            isCurrentWeek = isCurrentWeek,
                            totalStudents = students.size,
                            onEnterMarks = {
                                viewModel.setAssessment(assessment.id)
                                viewModel.navigateTo(Screen.MarksEntry(batchId, assessment.id))
                            },
                            onDailyAttendance = {
                                viewModel.setAssessment(assessment.id)
                                viewModel.setSelectedDate(assessment.startDate)
                                viewModel.navigateTo(Screen.Attendance(batchId, assessment.id))
                            },
                            onLeaderboard = {
                                viewModel.setAssessment(assessment.id)
                                viewModel.navigateTo(Screen.Leaderboard(batchId))
                            },
                            onEdit = {
                                viewModel.navigateTo(Screen.AddEditAssessment(assessment.id, batchId))
                            },
                            onDelete = {
                                assessmentToDelete = assessment
                            }
                        )
                    }
                }
            }
        }
    }

    assessmentToDelete?.let { assessment ->
        AlertDialog(
            onDismissRequest = { assessmentToDelete = null },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Delete Assessment", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${assessment.weekLabel}'? Associated marks for this week will also be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAssessment(assessment)
                        assessmentToDelete = null
                    }
                ) {
                    Text("Delete", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { assessmentToDelete = null }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }
}

@Composable
fun AssessmentCard(
    assessment: WeeklyAssessmentEntity,
    isCurrentWeek: Boolean,
    totalStudents: Int,
    onEnterMarks: () -> Unit,
    onDailyAttendance: () -> Unit,
    onLeaderboard: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val dateRangeText = remember(assessment.startDate, assessment.endDate) {
        "${dateFormat.format(Date(assessment.startDate))} – ${dateFormat.format(Date(assessment.endDate))}"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Week Label + Current Week Badge + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = assessment.weekLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusTextDark
                    )

                    if (isCurrentWeek) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Current Week",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = FocusTextSecondary)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Label & Max Marks", color = FocusTextDark) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = FocusPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Assessment", color = FocusErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = FocusErrorRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date Range
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = FocusTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateRangeText,
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Students: $totalStudents",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FocusTextDark
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Attendance: Mon–Sat (6 pts)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F1FC))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Test: ${assessment.testMaxMarks.toInt()} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FocusPrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Enter Marks, Attendance, Leaderboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEnterMarks,
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(38.dp)
                        .testTag("assessment_enter_marks_${assessment.id}")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enter Marks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDailyAttendance,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .testTag("assessment_attendance_${assessment.id}")
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = CategoryAttendance)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Attendance", fontSize = 12.sp, color = CategoryAttendance, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onLeaderboard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("assessment_leaderboard_${assessment.id}")
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp), tint = FocusGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rank", fontSize = 12.sp, color = FocusGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
