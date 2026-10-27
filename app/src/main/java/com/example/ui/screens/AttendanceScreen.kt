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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.theme.FocusWarningAmber
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.AttendanceDateUtils
import java.util.Locale

private val Weekdays = listOf(
    Pair(1, "Mon"),
    Pair(2, "Tue"),
    Pair(3, "Wed"),
    Pair(4, "Thu"),
    Pair(5, "Fri"),
    Pair(6, "Sat")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    batchId: Long = 0,
    assessmentId: Long = 0,
    viewModel: FocusViewModel
) {
    LaunchedEffect(batchId) {
        if (batchId > 0) viewModel.setBatch(batchId)
    }

    val allBatches by viewModel.allBatches.collectAsState()
    val batch by viewModel.currentBatch.collectAsState()
    val currentBatchId by viewModel.selectedBatchId.collectAsState()
    val students by viewModel.studentsInBatch.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsState()
    val calendarAttendance by viewModel.calendarAttendanceRecords.collectAsState()
    val batchStudentsWithResults by viewModel.batchStudentsWithResults.collectAsState()

    var showBatchDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    // Map of studentId -> attendance record for selected date
    val attendanceMap = remember(calendarAttendance) {
        calendarAttendance.associateBy { it.studentId }
    }

    // Map of studentId -> weekly attendance score for current assessment
    val weeklyAttendanceScores = remember(batchStudentsWithResults) {
        batchStudentsWithResults.associate { it.student.id to it.result?.attendanceMarks }
    }

    // Filtered students by search
    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.rollNumber.contains(searchQuery, ignoreCase = true) ||
            it.studentCode.contains(searchQuery, ignoreCase = true)
        }
    }

    // Counters for the active day
    val totalCount = students.size
    val presentCount = students.count { attendanceMap[it.id]?.status == AttendanceStatus.PRESENT }
    val absentCount = students.count { attendanceMap[it.id]?.status == AttendanceStatus.ABSENT }
    val leaveCount = students.count { attendanceMap[it.id]?.status == AttendanceStatus.LEAVE }
    val unmarkedCount = totalCount - (presentCount + absentCount + leaveCount)

    val currentWeekLabel = AttendanceDateUtils.getWeekLabel(selectedDateMillis)
    val formattedDisplayDate = AttendanceDateUtils.formatDisplayDate(selectedDateMillis)

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Daily Attendance",
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
            // Top Controls Card: Batch Selector + Calendar Date Navigation
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Batch Selector Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Batch:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusTextDark
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F1FC))
                                    .clickable { showBatchDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("attendance_batch_selector"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = batch?.name ?: "Select Batch",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FocusPrimaryBlue
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = FocusPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showBatchDropdown,
                                onDismissRequest = { showBatchDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                allBatches.forEach { b ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = b.name,
                                                color = if (b.id == currentBatchId) FocusPrimaryBlue else FocusTextDark,
                                                fontWeight = if (b.id == currentBatchId) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setBatch(b.id)
                                            showBatchDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date Navigator Bar (Prev Day < Date + Picker > Next Day, Today)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF6F8FC))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousDay() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("attendance_prev_day")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Previous Day",
                                tint = FocusPrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Center Date Button (Opens DatePicker Dialog)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("attendance_open_datepicker"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick Date",
                                tint = FocusPrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedDisplayDate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTextDark
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { viewModel.jumpToToday() },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Today", fontSize = 12.sp, color = FocusPrimaryBlue, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.nextDay() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("attendance_next_day")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Next Day",
                                    tint = FocusPrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Monday to Saturday Day Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Weekdays.forEach { (dayNum, dayLabel) ->
                            val isSelected = selectedDayOfWeek == dayNum
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) FocusPrimaryBlue else Color(0xFFF1F5F9))
                                    .clickable { viewModel.setDayOfWeek(dayNum) }
                                    .testTag("day_pill_$dayNum"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else FocusTextDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Stats Strip: Total, Present, Absent, Leave, Unmarked
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatBadge(
                            label = "Total",
                            count = totalCount,
                            color = FocusTextDark,
                            bgColor = Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceStatBadge(
                            label = "Present",
                            count = presentCount,
                            color = Color(0xFF16A34A),
                            bgColor = Color(0xFFDCFCE7),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceStatBadge(
                            label = "Absent",
                            count = absentCount,
                            color = FocusErrorRed,
                            bgColor = Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceStatBadge(
                            label = "Leave",
                            count = leaveCount,
                            color = FocusWarningAmber,
                            bgColor = Color(0xFFFEF3C7),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceStatBadge(
                            label = "Unmarked",
                            count = unmarkedCount,
                            color = FocusTextSecondary,
                            bgColor = Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Batch Quick Actions Row: Mark All Present, Clear, Search Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.markAllStudentsAttendance(AttendanceStatus.PRESENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                                .testTag("mark_all_present_button")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Present", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.markAllStudentsAttendance(AttendanceStatus.ABSENT) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("clear_attendance_button")
                        ) {
                            Text("All Absent", fontSize = 12.sp, color = FocusErrorRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student by name or roll...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("attendance_search_field"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF6F8FC),
                            unfocusedContainerColor = Color(0xFFF6F8FC),
                            focusedBorderColor = FocusPrimaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Sync Information Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F1FC))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Auto-synced to $currentWeekLabel (Monday–Saturday). Score out of 6.0 = (Present / (Present + Absent)) × 6.0.",
                    fontSize = 11.sp,
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
                if (filteredStudents.isEmpty()) {
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
                                    text = if (students.isEmpty()) "No students in this batch yet." else "No students match your search.",
                                    fontSize = 14.sp,
                                    color = FocusTextSecondary
                                )
                                if (students.isEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.AddEditStudent(0, currentBatchId)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue)
                                    ) {
                                        Text("Add Student")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredStudents, key = { it.id }) { student ->
                        val record = attendanceMap[student.id]
                        val currentStatus = record?.status
                        val weeklyScore = weeklyAttendanceScores[student.id]

                        DailyStudentAttendanceCard(
                            student = student,
                            currentStatus = currentStatus,
                            weeklyScore = weeklyScore,
                            onStatusSelected = { status ->
                                viewModel.markStudentAttendance(student.id, status)
                            }
                        )
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setSelectedDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = FocusPrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AttendanceStatBadge(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun DailyStudentAttendanceCard(
    student: StudentEntity,
    currentStatus: AttendanceStatus?,
    weeklyScore: Double?,
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
                size = 42
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

                val subInfo = buildString {
                    if (student.rollNumber.isNotBlank()) append("Roll: ${student.rollNumber} • ")
                    if (weeklyScore != null) {
                        append(String.format(Locale.US, "Week: %.1f/6", weeklyScore))
                    } else {
                        append("Week: Pending")
                    }
                }
                Text(
                    text = subInfo,
                    fontSize = 11.sp,
                    color = if (weeklyScore != null) CategoryAttendance else FocusTextSecondary,
                    fontWeight = if (weeklyScore != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Quick Status Buttons: Present (P), Absent (A), Leave (L) with 48dp touch targets
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Present (1.0 mark)
                DailyStatusButton(
                    label = "P",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    selectedBg = Color(0xFF16A34A),
                    onClick = { onStatusSelected(AttendanceStatus.PRESENT) },
                    testTag = "att_present_${student.id}"
                )

                // Absent (0.0 mark)
                DailyStatusButton(
                    label = "A",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    selectedBg = FocusErrorRed,
                    onClick = { onStatusSelected(AttendanceStatus.ABSENT) },
                    testTag = "att_absent_${student.id}"
                )

                // Leave (Approved leave, excluded from denominator)
                DailyStatusButton(
                    label = "L",
                    isSelected = currentStatus == AttendanceStatus.LEAVE,
                    selectedBg = FocusWarningAmber,
                    onClick = { onStatusSelected(AttendanceStatus.LEAVE) },
                    testTag = "att_leave_${student.id}"
                )
            }
        }
    }
}

@Composable
fun DailyStatusButton(
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
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
