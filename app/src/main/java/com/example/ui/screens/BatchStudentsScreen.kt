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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentEntity
import com.example.ui.components.FocusBottomNavBar
import com.example.ui.components.FocusTopAppBar
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun BatchStudentsScreen(
    batchId: Long,
    viewModel: FocusViewModel
) {
    LaunchedEffect(batchId) {
        viewModel.setBatch(batchId)
    }

    val batch by viewModel.currentBatch.collectAsState()
    val students by viewModel.filteredStudents.collectAsState()
    val searchQuery by viewModel.studentSearchQuery.collectAsState()
    val isMultiSelect by viewModel.isMultiSelectMode.collectAsState()
    val selectedIds by viewModel.selectedStudentIds.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()

    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val batchTitle = batch?.name ?: "Class $batchId"

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = if (isMultiSelect) "${selectedIds.size} Selected" else batchTitle,
                onBackClick = {
                    if (isMultiSelect) {
                        viewModel.clearStudentSelection()
                    } else {
                        viewModel.navigateBack()
                    }
                },
                actions = {
                    if (isMultiSelect) {
                        IconButton(onClick = { viewModel.selectAllStudents() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.clearStudentSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.navigateTo(Screen.AddEditStudent(studentId = 0, defaultBatchId = batchId))
                            },
                            modifier = Modifier.testTag("add_student_top_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Student", tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMultiSelect) {
                // Multi-select management bottom actions (panel 10 in UI reference)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showBatchDeleteConfirm = true },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = FocusErrorRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_selected_students_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Selected", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.clearStudentSelection()
                            viewModel.navigateTo(Screen.WeeklyAssessments(batchId))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("manage_marks_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Weekly Marks", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                FocusBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        },
        containerColor = FocusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("batch_students_screen_root")
        ) {
            // Search Bar & Actions Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.studentSearchQuery.value = it },
                    placeholder = { Text("Search students...", color = FocusTextSecondary) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = FocusTextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.studentSearchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = FocusTextDark)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Shortcuts for Weekly Assessment & Leaderboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.Attendance(batchId)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Attendance", fontSize = 11.sp, color = CategoryAttendance, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.WeeklyAssessments(batchId)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Marks", fontSize = 11.sp, color = FocusPrimaryBlue, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.Leaderboard(batchId)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ranks", fontSize = 11.sp, color = FocusTextDark, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.isMultiSelectMode.value = !isMultiSelect },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isMultiSelect) "Done" else "Manage", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Student List
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No students match '$searchQuery'"
                                    else "No students have been added to this batch yet.",
                                    fontSize = 15.sp,
                                    color = FocusTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.navigateTo(Screen.AddEditStudent(studentId = 0, defaultBatchId = batchId))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("empty_state_add_student_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Student")
                                }
                            }
                        }
                    }
                } else {
                    items(students, key = { it.id }) { student ->
                        StudentItemRow(
                            student = student,
                            isMultiSelect = isMultiSelect,
                            isSelected = selectedIds.contains(student.id),
                            onToggleSelect = { viewModel.toggleStudentSelection(student.id) },
                            onClick = {
                                if (isMultiSelect) {
                                    viewModel.toggleStudentSelection(student.id)
                                } else {
                                    viewModel.navigateTo(Screen.StudentProfile(student.id))
                                }
                            },
                            onEdit = {
                                viewModel.navigateTo(Screen.AddEditStudent(student.id, defaultBatchId = batchId))
                            },
                            onDelete = { studentToDelete = student },
                            onEnterMarks = {
                                val currentAssessmentId = viewModel.selectedAssessmentId.value
                                if (currentAssessmentId != null) {
                                    viewModel.navigateTo(Screen.MarksEntry(batchId, currentAssessmentId, student.id))
                                } else {
                                    viewModel.navigateTo(Screen.WeeklyAssessments(batchId))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Single student delete confirm dialog
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Delete Student", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${student.fullName}? Their weekly marks will also be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStudent(student)
                        studentToDelete = null
                    }
                ) {
                    Text("Delete", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }

    // Batch delete selected confirm dialog
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Delete Selected Students", fontWeight = FontWeight.Bold) },
            text = { Text("Delete ${selectedIds.size} student(s) permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchDeleteConfirm = false
                        viewModel.deleteSelectedStudents()
                    }
                ) {
                    Text("Delete All", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }
}

@Composable
fun StudentItemRow(
    student: StudentEntity,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnterMarks: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_item_${student.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = FocusPrimaryBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            StudentAvatar(
                imagePath = student.profileImagePath,
                fullName = student.fullName,
                size = 44
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                val rollInfo = if (student.rollNumber.isNotBlank()) "Roll No. ${student.rollNumber}" else student.studentCode
                Text(
                    text = rollInfo,
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
            }

            if (!isMultiSelect) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = FocusTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Profile", color = FocusTextDark) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FocusPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Enter Weekly Marks", color = FocusTextDark) },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = FocusPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onEnterMarks()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Student", color = FocusTextDark) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = FocusPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Student", color = FocusErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = FocusErrorRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = FocusTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
