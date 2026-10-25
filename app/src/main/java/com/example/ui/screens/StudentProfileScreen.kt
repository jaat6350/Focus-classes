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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.WeeklyAssessmentEntity
import com.example.ui.components.CategoryBadge
import com.example.ui.components.FocusTopAppBar
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.CategoryActivity
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.CategoryTest
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen
import java.util.Locale

@Composable
fun StudentProfileScreen(
    studentId: Long,
    viewModel: FocusViewModel
) {
    var student by remember { mutableStateOf<StudentEntity?>(null) }
    var assessments by remember { mutableStateOf<List<WeeklyAssessmentEntity>>(emptyList()) }
    var selectedAssessment by remember { mutableStateOf<WeeklyAssessmentEntity?>(null) }
    var result by remember { mutableStateOf<StudentWeeklyResultEntity?>(null) }
    var showWeekDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val config by viewModel.scoringConfig.collectAsState()

    LaunchedEffect(studentId) {
        val s = viewModel.repository.getStudentById(studentId)
        student = s
        if (s != null) {
            viewModel.repository.getAssessmentsByBatch(s.batchId).collect { list ->
                assessments = list
                if (selectedAssessment == null && list.isNotEmpty()) {
                    selectedAssessment = list.first()
                }
            }
        }
    }

    LaunchedEffect(studentId, selectedAssessment?.id) {
        val aId = selectedAssessment?.id
        if (aId != null) {
            viewModel.repository.getResultFlow(studentId, aId).collect { r ->
                result = r
            }
        } else {
            result = null
        }
    }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Student Profile",
                onBackClick = { viewModel.navigateBack() }
            )
        },
        containerColor = FocusBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("student_profile_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card (Matches UI Reference)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StudentAvatar(
                            imagePath = student?.profileImagePath,
                            fullName = student?.fullName ?: "",
                            size = 80
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = student?.fullName ?: "Student",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusTextDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val rollText = if (!student?.rollNumber.isNullOrBlank()) "Roll No. ${student?.rollNumber}" else student?.studentCode ?: ""
                        Text(
                            text = "Batch ID: ${student?.batchId}  |  $rollText",
                            fontSize = 13.sp,
                            color = FocusTextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons Row: Edit Profile (Blue) & Delete (Red)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    student?.let {
                                        viewModel.navigateTo(Screen.AddEditStudent(it.id, it.batchId))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("profile_edit_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusErrorRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("profile_delete_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Weekly Performance Card (Matches UI Reference)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Weekly Performance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTextDark
                            )

                            // Quick edit / enter marks button
                            TextButton(
                                onClick = {
                                    val bId = student?.batchId ?: 1L
                                    val aId = selectedAssessment?.id ?: 0L
                                    if (aId > 0) {
                                        viewModel.navigateTo(Screen.MarksEntry(bId, aId, studentId))
                                    } else {
                                        viewModel.navigateTo(Screen.WeeklyAssessments(bId))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = FocusPrimaryBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Marks", color = FocusPrimaryBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Week Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedAssessment?.weekLabel ?: "No Assessment Period",
                                onValueChange = {},
                                readOnly = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = FocusTextDark)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWeekDropdown = true }
                                    .testTag("profile_week_dropdown"),
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
                                            selectedAssessment = a
                                            showWeekDropdown = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ Create New Assessment Week", color = FocusPrimaryBlue, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showWeekDropdown = false
                                        student?.let {
                                            viewModel.navigateTo(Screen.AddEditAssessment(0, it.batchId))
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Categories: Attendance, Test Marks, Classroom Activities
                        val att = result?.attendanceMarks
                        val testMarks = result?.testMarks
                        val testMax = selectedAssessment?.testMaxMarks ?: 20.0
                        val disc = result?.disciplineMarks
                        val proj = result?.projectMarks

                        // Category A: Attendance (out of 6)
                        PerformanceRowItem(
                            title = "Attendance",
                            obtained = att,
                            max = 6.0,
                            color = CategoryAttendance,
                            icon = Icons.Default.DateRange
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category B: Test Marks (out of testMax)
                        PerformanceRowItem(
                            title = "Test Marks",
                            obtained = testMarks,
                            max = testMax,
                            color = CategoryTest,
                            icon = Icons.Default.Description
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category C: Classroom Activities (Discipline & Project)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            CategoryBadge(
                                title = "Classroom Activities",
                                icon = Icons.Default.MenuBook,
                                color = CategoryActivity
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Discipline", fontSize = 13.sp, color = FocusTextDark)
                                Text(
                                    text = disc?.let { String.format(Locale.US, "%.1f / 4", it) } ?: "- / 4",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CategoryActivity
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Project", fontSize = 13.sp, color = FocusTextDark)
                                Text(
                                    text = proj?.let { String.format(Locale.US, "%.1f / 10", it) } ?: "- / 10",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CategoryActivity
                                )
                            }
                        }

                        // Custom Categories Rendering
                        val allCats by viewModel.allCategories.collectAsState()
                        val customCategories = allCats.filter { !it.isSystemDefault }
                        val customMarksList by viewModel.repository.getCustomMarksForStudentFlow(
                            studentId, selectedAssessment?.id ?: 0L
                        ).collectAsState(initial = emptyList())
                        val customMarksMap = remember(customMarksList) {
                            customMarksList.associate { it.categoryId to it.marksObtained }
                        }

                        if (customCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                CategoryBadge(
                                    title = "Custom Categories",
                                    icon = Icons.Default.Description,
                                    color = FocusPrimaryBlue
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                customCategories.forEach { cat ->
                                    val obtained = customMarksMap[cat.id]
                                    PerformanceRowItem(
                                        title = cat.name,
                                        obtained = obtained,
                                        max = cat.maxMarks,
                                        color = FocusPrimaryBlue,
                                        icon = Icons.Default.Description
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Overall Summary Card
                        val isComplete = att != null && testMarks != null && disc != null && proj != null
                        if (isComplete) {
                            val attPct = (att / 6.0) * 100.0
                            val testPct = (testMarks / testMax) * 100.0
                            val discPct = (disc / 4.0) * 100.0
                            val projPct = (proj / 10.0) * 100.0
                            val overall = (attPct * config.attendanceWeight +
                                    testPct * config.testWeight +
                                    discPct * config.disciplineWeight +
                                    projPct * config.projectWeight) / 100.0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Overall Score", fontSize = 13.sp, color = FocusTextSecondary)
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", overall),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FocusPrimaryBlue
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(FocusGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Complete",
                                        color = Color(0xFFB45309),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (result == null) "No marks recorded for this week yet."
                                    else "Assessment in progress (some categories pending).",
                                    color = Color(0xFF92400E),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Student", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this student?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        student?.let {
                            viewModel.deleteStudent(it) {
                                viewModel.navigateBack()
                            }
                        }
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PerformanceRowItem(
    title: String,
    obtained: Double?,
    max: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadge(title = title, icon = icon, color = color)
            Text(
                text = obtained?.let { String.format(Locale.US, "%.1f / %.0f", it, max) } ?: "- / ${max.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val progress = if (obtained != null && max > 0) (obtained / max).toFloat().coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}
