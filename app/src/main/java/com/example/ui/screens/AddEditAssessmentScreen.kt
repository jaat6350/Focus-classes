package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeeklyAssessmentEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel

@Composable
fun AddEditAssessmentScreen(
    assessmentId: Long,
    defaultBatchId: Long,
    viewModel: FocusViewModel
) {
    var weekLabel by remember { mutableStateOf("Week 1 (Apr 21 – Apr 27)") }
    var selectedBatchId by remember { mutableLongStateOf(defaultBatchId) }
    var testMaxMarksText by remember { mutableStateOf("20") }
    var isBatchDropdownExpanded by remember { mutableStateOf(false) }

    val allBatches by viewModel.allBatches.collectAsState()

    LaunchedEffect(assessmentId) {
        if (assessmentId > 0) {
            val a = viewModel.repository.getAssessmentById(assessmentId)
            if (a != null) {
                weekLabel = a.weekLabel
                selectedBatchId = a.batchId
                testMaxMarksText = a.testMaxMarks.toInt().toString()
            }
        } else if (defaultBatchId > 0) {
            selectedBatchId = defaultBatchId
        } else if (allBatches.isNotEmpty()) {
            selectedBatchId = allBatches.first().id
        }
    }

    val isEditing = assessmentId > 0
    val title = if (isEditing) "Edit Assessment" else "New Weekly Assessment"
    val selectedBatch = allBatches.find { it.id == selectedBatchId }

    fun submit() {
        val maxMarks = testMaxMarksText.toDoubleOrNull() ?: 20.0
        if (weekLabel.isNotBlank() && selectedBatchId > 0) {
            viewModel.saveAssessment(
                WeeklyAssessmentEntity(
                    id = assessmentId,
                    batchId = selectedBatchId,
                    weekLabel = weekLabel.trim(),
                    testMaxMarks = maxMarks
                )
            ) {
                viewModel.navigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = title,
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { submit() },
                        modifier = Modifier.testTag("save_assessment_button")
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
                .testTag("add_edit_assessment_root")
        ) {
            // Batch Selection
            Text(
                text = "Class / Batch",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedBatch?.name ?: "Select Batch",
                    onValueChange = {},
                    readOnly = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isBatchDropdownExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = com.example.ui.components.focusTextFieldColors()
                )

                DropdownMenu(
                    expanded = isBatchDropdownExpanded,
                    onDismissRequest = { isBatchDropdownExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    allBatches.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b.name, color = FocusTextDark) },
                            onClick = {
                                selectedBatchId = b.id
                                isBatchDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Week Label
            Text(
                text = "Week Label",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = weekLabel,
                onValueChange = { weekLabel = it },
                placeholder = { Text("e.g. Week 1 (Apr 21 – Apr 27)", color = FocusTextSecondary) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assessment_label_input"),
                shape = RoundedCornerShape(10.dp),
                colors = com.example.ui.components.focusTextFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Configurable Test Maximum Marks (e.g. 20, 25, 50, 100)
            Text(
                text = "Test Maximum Marks",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure maximum marks for this week's test (e.g. 20, 25, 50, 100).",
                fontSize = 12.sp,
                color = FocusTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                listOf(20, 25, 50, 100).forEach { preset ->
                    FilterChip(
                        selected = testMaxMarksText == preset.toString(),
                        onClick = { testMaxMarksText = preset.toString() },
                        label = { Text("$preset Marks") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FocusPrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = testMaxMarksText,
                onValueChange = { testMaxMarksText = it },
                placeholder = { Text("Enter custom max marks", color = FocusTextSecondary) },
                textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assessment_max_marks_input"),
                shape = RoundedCornerShape(10.dp),
                colors = com.example.ui.components.focusTextFieldColors()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_assessment_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Create Assessment Week",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
