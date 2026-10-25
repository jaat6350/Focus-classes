package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.StudentEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import java.io.File

@Composable
fun AddEditStudentScreen(
    studentId: Long,
    defaultBatchId: Long,
    viewModel: FocusViewModel
) {
    var fullName by remember { mutableStateOf("") }
    var selectedBatchId by remember { mutableLongStateOf(defaultBatchId) }
    var rollNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var existingImagePath by remember { mutableStateOf<String?>(null) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isBatchDropdownExpanded by remember { mutableStateOf(false) }

    val allBatches by viewModel.allBatches.collectAsState()

    LaunchedEffect(studentId) {
        if (studentId > 0) {
            val student = viewModel.repository.getStudentById(studentId)
            if (student != null) {
                fullName = student.fullName
                selectedBatchId = student.batchId
                rollNumber = student.rollNumber
                notes = student.notes
                existingImagePath = student.profileImagePath
            }
        } else if (defaultBatchId > 0) {
            selectedBatchId = defaultBatchId
        } else if (allBatches.isNotEmpty()) {
            selectedBatchId = allBatches.first().id
        }
    }

    // Android Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            newImageUri = uri
        }
    }

    val isEditing = studentId > 0
    val title = if (isEditing) "Edit Student" else "Add Student"
    val selectedBatch = allBatches.find { it.id == selectedBatchId }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = title,
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = {
                            if (fullName.isNotBlank()) {
                                viewModel.saveStudent(
                                    student = StudentEntity(
                                        id = studentId,
                                        fullName = fullName.trim(),
                                        batchId = selectedBatchId,
                                        rollNumber = rollNumber.trim(),
                                        notes = notes.trim(),
                                        profileImagePath = existingImagePath
                                    ),
                                    imageUri = newImageUri
                                ) {
                                    viewModel.navigateBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_student_check_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_student_screen_root"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture Picker Section (Matches reference image)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F1FC))
                    .border(2.dp, FocusPrimaryBlue.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("upload_photo_avatar_box"),
                contentAlignment = Alignment.Center
            ) {
                if (newImageUri != null) {
                    AsyncImage(
                        model = newImageUri,
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else if (!existingImagePath.isNullOrBlank() && File(existingImagePath!!).exists()) {
                    AsyncImage(
                        model = File(existingImagePath!!),
                        contentDescription = "Existing photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = FocusPrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to upload",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusPrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (newImageUri != null || !existingImagePath.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        newImageUri = null
                        existingImagePath = null
                    }
                ) {
                    Text("Remove Photo", color = FocusErrorRed, fontSize = 12.sp)
                }
            } else {
                Text(
                    text = "Tap to upload photo",
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Student Name Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Student Name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("Enter student name", color = FocusTextSecondary) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = com.example.ui.components.focusTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Class / Batch Dropdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Class",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedBatch?.name ?: "Select Class",
                        onValueChange = {},
                        readOnly = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = FocusTextDark)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBatchDropdownExpanded = true }
                            .testTag("student_batch_dropdown"),
                        shape = RoundedCornerShape(10.dp),
                        colors = com.example.ui.components.focusTextFieldColors()
                    )

                    DropdownMenu(
                        expanded = isBatchDropdownExpanded,
                        onDismissRequest = { isBatchDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        allBatches.forEach { batch ->
                            DropdownMenuItem(
                                text = { Text(batch.name, color = FocusTextDark) },
                                onClick = {
                                    selectedBatchId = batch.id
                                    isBatchDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Roll Number Input (Optional)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Roll Number (Optional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { rollNumber = it },
                    placeholder = { Text("Enter roll number", color = FocusTextSecondary) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_roll_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = com.example.ui.components.focusTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Notes (Optional)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Notes (Optional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Special notes, strengths, remarks...", color = FocusTextSecondary) },
                    minLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_notes_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = com.example.ui.components.focusTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        viewModel.saveStudent(
                            student = StudentEntity(
                                id = studentId,
                                fullName = fullName.trim(),
                                batchId = selectedBatchId,
                                rollNumber = rollNumber.trim(),
                                notes = notes.trim(),
                                profileImagePath = existingImagePath
                            ),
                            imageUri = newImageUri
                        ) {
                            viewModel.navigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_student_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
