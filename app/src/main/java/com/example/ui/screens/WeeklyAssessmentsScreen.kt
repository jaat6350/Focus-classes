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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.WeeklyAssessmentEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.CategoryAttendance
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun WeeklyAssessmentsScreen(
    batchId: Long,
    viewModel: FocusViewModel
) {
    LaunchedEffect(batchId) {
        if (batchId > 0) {
            viewModel.setBatch(batchId)
        }
    }

    val batch by viewModel.currentBatch.collectAsState()
    val assessments by viewModel.assessmentsForBatch.collectAsState()

    var assessmentToDelete by remember { mutableStateOf<WeeklyAssessmentEntity?>(null) }

    val batchTitle = batch?.name ?: "Batch $batchId"

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("weekly_assessments_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (assessments.isEmpty()) {
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
                                text = "No weekly assessments created for $batchTitle yet.",
                                fontSize = 15.sp,
                                color = FocusTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.navigateTo(Screen.AddEditAssessment(0, batchId)) },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Assessment Week")
                            }
                        }
                    }
                }
            } else {
                items(assessments, key = { it.id }) { assessment ->
                    AssessmentItemCard(
                        assessment = assessment,
                        onEnterMarks = {
                            viewModel.setAssessment(assessment.id)
                            viewModel.navigateTo(Screen.MarksEntry(batchId, assessment.id))
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
fun AssessmentItemCard(
    assessment: WeeklyAssessmentEntity,
    onEnterMarks: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEnterMarks)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = FocusPrimaryBlue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assessment.weekLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Test Maximum: ${assessment.testMaxMarks.toInt()} marks",
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = FocusTextSecondary)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Enter / Edit Marks", color = FocusTextDark) },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = FocusPrimaryBlue) },
                        onClick = {
                            showMenu = false
                            onEnterMarks()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Settings", color = FocusTextDark) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = FocusPrimaryBlue) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = FocusErrorRed) },
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
