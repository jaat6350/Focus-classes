package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.StudentCategoryMarkEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.WeeklyAssessmentEntity
import com.example.ui.components.CategoryBadge
import com.example.ui.components.FocusTopAppBar
import com.example.ui.components.NumberSelectPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.CategoryActivity
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.CategoryTest
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun EditMarksScreen(
    batchId: Long,
    assessmentId: Long,
    initialStudentId: Long,
    viewModel: FocusViewModel
) {
    val studentsInBatch by viewModel.studentsInBatch.collectAsState()
    val assessments by viewModel.assessmentsForBatch.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var currentStudentIndex by remember { mutableIntStateOf(0) }
    var currentAssessmentId by remember { mutableStateOf(assessmentId) }

    LaunchedEffect(studentsInBatch, initialStudentId) {
        val index = studentsInBatch.indexOfFirst { it.id == initialStudentId }
        if (index >= 0) {
            currentStudentIndex = index
        }
    }

    val currentStudent = studentsInBatch.getOrNull(currentStudentIndex)
    var currentAssessment by remember { mutableStateOf<WeeklyAssessmentEntity?>(null) }

    var attendanceMarks by remember { mutableStateOf<Double?>(null) }
    var testObtainedText by remember { mutableStateOf("") }
    var testMaxText by remember { mutableStateOf("20") }
    var disciplineText by remember { mutableStateOf("") }
    var projectText by remember { mutableStateOf("") }

    // Map for custom category marks inputs (categoryId -> text)
    val customMarksInputs = remember { mutableStateMapOf<Long, String>() }

    var isWeekDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Load Assessment
    LaunchedEffect(currentAssessmentId) {
        if (currentAssessmentId > 0) {
            val a = viewModel.repository.getAssessmentById(currentAssessmentId)
            currentAssessment = a
            if (a != null) {
                testMaxText = a.testMaxMarks.toInt().toString()
            }
        } else if (assessments.isNotEmpty()) {
            val a = assessments.first()
            currentAssessmentId = a.id
            currentAssessment = a
            testMaxText = a.testMaxMarks.toInt().toString()
        }
    }

    // Load Existing Result & Custom Marks for current student
    LaunchedEffect(currentStudent?.id, currentAssessmentId) {
        val sId = currentStudent?.id
        if (sId != null && currentAssessmentId > 0) {
            val existing = viewModel.repository.getResult(sId, currentAssessmentId)
            if (existing != null) {
                attendanceMarks = existing.attendanceMarks
                testObtainedText = existing.testMarks?.toString() ?: ""
                disciplineText = existing.disciplineMarks?.toString() ?: ""
                projectText = existing.projectMarks?.toString() ?: ""
            } else {
                attendanceMarks = null
                testObtainedText = ""
                disciplineText = ""
                projectText = ""
            }

            // Load custom category marks
            customMarksInputs.clear()
            val customMarks = viewModel.repository.getCustomMarksForStudent(sId, currentAssessmentId)
            for (cm in customMarks) {
                cm.marksObtained?.let {
                    customMarksInputs[cm.categoryId] = it.toString()
                }
            }
            validationError = null
        }
    }

    val customCategories = allCategories.filter { !it.isSystemDefault }

    fun saveCurrentMarks(advanceNext: Boolean = false) {
        val sId = currentStudent?.id ?: return
        if (currentAssessmentId <= 0) return

        val testObtained = testObtainedText.toDoubleOrNull()
        val testMax = testMaxText.toDoubleOrNull() ?: 20.0
        val discipline = disciplineText.toDoubleOrNull()
        val project = projectText.toDoubleOrNull()

        // Validations
        if (attendanceMarks != null && (attendanceMarks!! < 0 || attendanceMarks!! > 6)) {
            validationError = "Attendance must be between 0 and 6"
            return
        }
        if (testObtained != null && (testObtained < 0 || testObtained > testMax)) {
            validationError = "Test marks must be between 0 and $testMax"
            return
        }
        if (discipline != null && (discipline < 0 || discipline > 4)) {
            validationError = "Discipline must be between 0 and 4"
            return
        }
        if (project != null && (project < 0 || project > 10)) {
            validationError = "Project must be between 0 and 10"
            return
        }

        // Validate custom categories
        val customEntities = mutableListOf<StudentCategoryMarkEntity>()
        for (cat in customCategories) {
            val text = customMarksInputs[cat.id]
            if (!text.isNullOrBlank()) {
                val value = text.toDoubleOrNull()
                if (value == null || value < 0 || value > cat.maxMarks) {
                    validationError = "${cat.name} marks must be between 0 and ${cat.maxMarks.toInt()}"
                    return
                }
                customEntities.add(
                    StudentCategoryMarkEntity(
                        studentId = sId,
                        assessmentId = currentAssessmentId,
                        categoryId = cat.id,
                        marksObtained = value
                    )
                )
            }
        }

        viewModel.saveStudentMarks(
            result = StudentWeeklyResultEntity(
                studentId = sId,
                assessmentId = currentAssessmentId,
                attendanceMarks = attendanceMarks,
                testMarks = testObtained,
                disciplineMarks = discipline,
                projectMarks = project
            ),
            customMarks = customEntities
        ) {
            validationError = null
            if (advanceNext && currentStudentIndex < studentsInBatch.size - 1) {
                currentStudentIndex++
            }
        }
    }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Edit Marks",
                onBackClick = { viewModel.navigateBack() }
            )
        },
        containerColor = FocusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .testTag("edit_marks_screen_root")
        ) {
            // Student Switcher Header Card
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudentAvatar(
                            imagePath = currentStudent?.profileImagePath,
                            fullName = currentStudent?.fullName ?: "",
                            size = 50
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentStudent?.fullName ?: "Select Student",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val roll = if (!currentStudent?.rollNumber.isNullOrBlank()) "Roll No. ${currentStudent?.rollNumber}" else currentStudent?.studentCode ?: ""
                            Text(
                                text = "Class $batchId  |  $roll",
                                fontSize = 13.sp,
                                color = FocusTextSecondary
                            )
                        }

                        // Fast previous / next student switchers
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (currentStudentIndex > 0) {
                                        saveCurrentMarks(false)
                                        currentStudentIndex--
                                    }
                                },
                                enabled = currentStudentIndex > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Student")
                            }

                            Text(
                                text = "${currentStudentIndex + 1}/${studentsInBatch.size.coerceAtLeast(1)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FocusTextSecondary
                            )

                            IconButton(
                                onClick = {
                                    if (currentStudentIndex < studentsInBatch.size - 1) {
                                        saveCurrentMarks(true)
                                    }
                                },
                                enabled = currentStudentIndex < studentsInBatch.size - 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Student")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Week Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Week",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FocusTextDark,
                            modifier = Modifier.width(50.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = currentAssessment?.weekLabel ?: "Select Assessment Week",
                                onValueChange = {},
                                readOnly = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 14.sp),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isWeekDropdownExpanded = true }
                                    .testTag("assessment_week_dropdown"),
                                shape = RoundedCornerShape(8.dp),
                                colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                            )

                            DropdownMenu(
                                expanded = isWeekDropdownExpanded,
                                onDismissRequest = { isWeekDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                assessments.forEach { a ->
                                    DropdownMenuItem(
                                        text = { Text(a.weekLabel, color = FocusTextDark) },
                                        onClick = {
                                            currentAssessmentId = a.id
                                            currentAssessment = a
                                            isWeekDropdownExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ Create New Week", color = FocusPrimaryBlue, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        isWeekDropdownExpanded = false
                                        viewModel.navigateTo(Screen.AddEditAssessment(0, batchId))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Marks Entry Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Attendance (out of 6) with selector pills + Daily Attendance Sheet Button
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryBadge(
                                title = "Attendance (out of 6)",
                                icon = Icons.Default.DateRange,
                                color = CategoryAttendance
                            )

                            // Quick link to Daily Attendance Sheet
                            TextButton(
                                onClick = {
                                    viewModel.navigateTo(Screen.Attendance(batchId, currentAssessmentId))
                                }
                            ) {
                                Text("Daily Sheet (Mon-Sat)", fontSize = 12.sp, color = FocusPrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selector Pills [1] [2] [3] [4] [5] [6]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..6).forEach { num ->
                                NumberSelectPill(
                                    number = num,
                                    isSelected = attendanceMarks?.toInt() == num,
                                    onSelect = { attendanceMarks = it.toDouble() },
                                    modifier = Modifier.testTag("pill_attendance_$num")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = attendanceMarks?.let { "Score: $it / 6.0" } ?: "Not marked",
                                fontSize = 12.sp,
                                color = FocusTextSecondary
                            )
                            Row {
                                TextButton(onClick = { attendanceMarks = 0.0 }) {
                                    Text("0 (Absent)", fontSize = 12.sp, color = CategoryAttendance)
                                }
                                TextButton(onClick = { attendanceMarks = null }) {
                                    Text("Clear", fontSize = 12.sp, color = FocusTextSecondary)
                                }
                            }
                        }
                    }
                }

                // Section 2: Test Marks (Marks Obtained, Total Marks)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CategoryBadge(
                            title = "Test Marks (out of $testMaxText)",
                            icon = Icons.Default.Description,
                            color = CategoryTest
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Marks Obtained",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FocusTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = testObtainedText,
                                    onValueChange = { testObtainedText = it },
                                    placeholder = { Text("e.g. 18", color = FocusTextSecondary) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_marks_obtained_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Marks",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FocusTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = testMaxText,
                                    onValueChange = { testMaxText = it },
                                    placeholder = { Text("20", color = FocusTextSecondary) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_max_marks_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                                )
                            }
                        }
                    }
                }

                // Section 3: Classroom Activities (Discipline & Project)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CategoryBadge(
                            title = "Classroom Activities",
                            icon = Icons.Default.MenuBook,
                            color = CategoryActivity
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Discipline (out of 4)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FocusTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = disciplineText,
                                    onValueChange = { disciplineText = it },
                                    placeholder = { Text("0 - 4", color = FocusTextSecondary) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("discipline_marks_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Project (out of 10)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FocusTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = projectText,
                                    onValueChange = { projectText = it },
                                    placeholder = { Text("0 - 10", color = FocusTextSecondary) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("project_marks_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                                )
                            }
                        }
                    }
                }

                // Section 4: DYNAMIC CUSTOM CATEGORIES (User Added)
                if (customCategories.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CategoryBadge(
                                title = "Custom Assessment Categories",
                                icon = Icons.Default.Assignment,
                                color = FocusPrimaryBlue
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            customCategories.forEach { cat ->
                                val currentVal = customMarksInputs[cat.id] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FocusTextDark
                                        )
                                        Text(
                                            text = "Max Marks: ${cat.maxMarks.toInt()}  •  Weight: ${cat.weight.toInt()}%",
                                            fontSize = 11.sp,
                                            color = FocusTextSecondary
                                        )
                                    }

                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { customMarksInputs[cat.id] = it },
                                        placeholder = { Text("0 - ${cat.maxMarks.toInt()}", color = FocusTextSecondary) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .width(130.dp)
                                            .testTag("custom_cat_input_${cat.id}"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                                    )
                                }
                            }
                        }
                    }
                }

                // Validation Error display
                validationError?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save Changes Button
                Button(
                    onClick = { saveCurrentMarks(false) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_marks_changes_button")
                ) {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (currentStudentIndex < studentsInBatch.size - 1) {
                    OutlinedButton(
                        onClick = { saveCurrentMarks(true) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Save & Next Student", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FocusPrimaryBlue)
                    }
                }
            }
        }
    }
}
