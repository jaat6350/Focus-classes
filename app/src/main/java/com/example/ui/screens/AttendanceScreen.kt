package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceEntity
import com.example.data.model.StudentEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.CategoryAttendanceBg
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.theme.FocusWarningAmber
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen
import java.util.Locale

private val DaysOfWeek = listOf(
    Pair(1, "Mon"),
    Pair(2, "Tue"),
    Pair(3, "Wed"),
    Pair(4, "Thu"),
    Pair(5, "Fri"),
    Pair(6, "Sat")
)

@Composable
fun AttendanceScreen(
    batchId: Long,
    assessmentId: Long,
    viewModel: FocusViewModel
) {
    LaunchedEffect(batchId) {
        if (batchId > 0) viewModel.setBatch(batchId)
    }

    val batch by viewModel.currentBatch.collectAsState()
    val assessments by viewModel.assessmentsForBatch.collectAsState()
    val currentAssessment by viewModel.currentAssessment.collectAsState()
    val students by viewModel.filteredStudents.collectAsState()
    val selectedDay by viewModel.selectedDayOfWeek.collectAsState()
    val dailyAttendance by viewModel.dailyAttendanceRecords.collectAsState()
    val batchStudentsWithResults by viewModel.batchStudentsWithResults.collectAsState()

    var showWeekDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(assessmentId, assessments) {
        if (assessmentId > 0) {
            viewModel.setAssessment(assessmentId)
        } else if (viewModel.selectedAssessmentId.value == null && assessments.isNotEmpty()) {
            viewModel.setAssessment(assessments.first().id)
        }
    }

    val attendanceMap = remember(dailyAttendance) {
        dailyAttendance.associateBy { it.studentId }
    }

    val weeklyAttendanceScores = remember(batchStudentsWithResults) {
        batchStudentsWithResults.associate { it.student.id to it.result?.attendanceMarks }
    }

    val batchTitle = batch?.name ?: "Batch $batchId"

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "$batchTitle Attendance",
                onBackClick = { viewModel.navigateBack() }
            )
        },
        containerColor = FocusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("attendance_screen_root")
        ) {
            // Assessment Week & Day Tabs Card
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Week Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentAssessment?.weekLabel ?: "Select Assessment Week",
                            onValueChange = {},
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWeekDropdown = true }
                                .testTag("attendance_week_dropdown"),
                            shape = RoundedCornerShape(10.dp),
                            colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                        )

                        DropdownMenu(
                            expanded = showWeekDropdown,
                            onDismissRequest = { showWeekDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            assessments.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a.weekLabel, color = FocusTextDark) },
                                    onClick = {
                                        viewModel.setAssessment(a.id)
                                        showWeekDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create Assessment Week", color = FocusPrimaryBlue, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showWeekDropdown = false
                                    viewModel.navigateTo(Screen.AddEditAssessment(0, batchId))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Day of Week selector (Mon..Sat)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Day:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusTextDark
                        )

                        DaysOfWeek.forEach { (dayNum, dayLabel) ->
                            val isSelected = selectedDay == dayNum
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) FocusPrimaryBlue else Color(0xFFF1F5F9))
                                    .clickable { viewModel.setDayOfWeek(dayNum) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else FocusTextDark
                                    )
                                    Text(
                                        text = "1 pt",
                                        fontSize = 9.sp,
                                        color = if (isSelected) FocusGold else FocusTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.markAllStudentsAttendance(AttendanceStatus.PRESENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = CategoryAttendance),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("mark_all_present_button")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Present (1.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.markAllStudentsAttendance(AttendanceStatus.ABSENT) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Clear / Absent", fontSize = 12.sp, color = FocusErrorRed)
                        }
                    }
                }
            }

            // Attendance Info Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F1FC))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Each day = 1 mark. Weekly score (out of 6.0) automatically updates student marks and overall rank.",
                    fontSize = 12.sp,
                    color = FocusPrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            // Student Attendance List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (students.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No students in this batch yet.",
                                    fontSize = 15.sp,
                                    color = FocusTextSecondary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.navigateTo(Screen.AddEditStudent(0, batchId)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue)
                                ) {
                                    Text("Add Student")
                                }
                            }
                        }
                    }
                } else {
                    items(students, key = { it.id }) { student ->
                        val record = attendanceMap[student.id]
                        val currentStatus = record?.status
                        val weeklyTotal = weeklyAttendanceScores[student.id]

                        StudentAttendanceRow(
                            student = student,
                            currentStatus = currentStatus,
                            weeklyTotal = weeklyTotal,
                            onStatusSelected = { status ->
                                viewModel.markStudentAttendance(student.id, status)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    student: StudentEntity,
    currentStatus: AttendanceStatus?,
    weeklyTotal: Double?,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(
                imagePath = student.profileImagePath,
                fullName = student.fullName,
                size = 40
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                val weeklyText = weeklyTotal?.let { String.format(Locale.US, "Week Total: %.1f / 6", it) } ?: "Week: Not evaluated"
                Text(
                    text = weeklyText,
                    fontSize = 11.sp,
                    color = if (weeklyTotal != null) CategoryAttendance else FocusTextSecondary,
                    fontWeight = if (weeklyTotal != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Quick Status Buttons: Present (P), Late (L), Absent (A)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Present (1.0 mark)
                AttendanceStatusButton(
                    label = "P",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    selectedBg = CategoryAttendance,
                    onClick = { onStatusSelected(AttendanceStatus.PRESENT) },
                    testTag = "att_present_${student.id}"
                )

                // Late (0.5 mark)
                AttendanceStatusButton(
                    label = "L",
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    selectedBg = FocusWarningAmber,
                    onClick = { onStatusSelected(AttendanceStatus.LATE) },
                    testTag = "att_late_${student.id}"
                )

                // Absent (0.0 mark)
                AttendanceStatusButton(
                    label = "A",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    selectedBg = FocusErrorRed,
                    onClick = { onStatusSelected(AttendanceStatus.ABSENT) },
                    testTag = "att_absent_${student.id}"
                )
            }
        }
    }
}

@Composable
fun AttendanceStatusButton(
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isSelected) selectedBg else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else FocusTextDark
        )
    }
}
