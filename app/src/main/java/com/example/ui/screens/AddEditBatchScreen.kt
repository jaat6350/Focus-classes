package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatchEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel

@Composable
fun AddEditBatchScreen(
    batchId: Long,
    viewModel: FocusViewModel
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableIntStateOf(6) }
    var description by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(batchId) {
        if (batchId > 0) {
            val existing = viewModel.repository.getBatchById(batchId)
            if (existing != null) {
                name = existing.name
                grade = existing.grade
                description = existing.description
            }
        } else {
            name = "Class 6"
        }
    }

    val isEditing = batchId > 0
    val title = if (isEditing) "Edit Batch" else "Add Batch"

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = title,
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.saveBatch(
                                    BatchEntity(
                                        id = batchId,
                                        name = name.trim(),
                                        grade = grade,
                                        description = description.trim()
                                    )
                                ) {
                                    viewModel.navigateBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_batch_check_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Batch",
                            tint = Color.White
                        )
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
                .testTag("add_batch_screen_root")
        ) {
            Text(
                text = "Batch Name",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Class 6 Morning", color = FocusTextSecondary) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_name_input"),
                colors = com.example.ui.components.focusTextFieldColors(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Class",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = grade.toString(),
                    onValueChange = {},
                    readOnly = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Class",
                            tint = FocusTextDark
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = true }
                        .testTag("batch_grade_dropdown"),
                    colors = com.example.ui.components.focusTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    (1..12).forEach { g ->
                        DropdownMenuItem(
                            text = { Text("Class $g", color = FocusTextDark) },
                            onClick = {
                                grade = g
                                if (!isEditing) {
                                    name = "Class $g"
                                }
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Description (Optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Enter batch description...", color = FocusTextSecondary) },
                textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_description_input"),
                colors = com.example.ui.components.focusTextFieldColors(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveBatch(
                            BatchEntity(
                                id = batchId,
                                name = name.trim(),
                                grade = grade,
                                description = description.trim()
                            )
                        ) {
                            viewModel.navigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_batch_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Add Batch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
