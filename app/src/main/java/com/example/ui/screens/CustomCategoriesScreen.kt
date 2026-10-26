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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssessmentCategoryEntity
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel

@Composable
fun CustomCategoriesScreen(
    viewModel: FocusViewModel
) {
    val categories by viewModel.allCategories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<AssessmentCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<AssessmentCategoryEntity?>(null) }

    val totalWeight = categories.sumOf { it.weight.toDouble() }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Assessment Categories",
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_custom_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = Color.White)
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
                .testTag("custom_categories_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Information Header Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1FC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = FocusPrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dynamic Assessment Categories",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTextDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add custom evaluation categories beyond default marks. Each category has its own maximum mark which contributes directly to the total marks and overall percentage calculation.",
                            fontSize = 12.sp,
                            color = FocusTextSecondary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val totalMaxMarks = categories.sumOf { it.maxMarks }
                        Text(
                            text = "Total Default Max Marks: ${totalMaxMarks.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusPrimaryBlue
                        )
                    }
                }
            }

            // Categories List
            items(categories, key = { it.id }) { cat ->
                CategoryRowCard(
                    category = cat,
                    onEdit = { categoryToEdit = cat },
                    onDelete = { categoryToDelete = cat }
                )
            }

            // Add New Category Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_category_footer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Custom Category", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || categoryToEdit != null) {
        val target = categoryToEdit
        var name by remember { mutableStateOf(target?.name ?: "") }
        var maxMarksText by remember { mutableStateOf(target?.maxMarks?.toInt()?.toString() ?: "10") }
        var weightText by remember { mutableStateOf(target?.weight?.toInt()?.toString() ?: "20") }
        var colorHex by remember { mutableStateOf(target?.colorHex ?: "#1459B3") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                categoryToEdit = null
            },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = {
                Text(
                    text = if (target != null) "Edit Category" else "New Assessment Category",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Oral Viva, Homework, Lab", color = FocusTextSecondary) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        enabled = target?.isSystemDefault != true,
                        singleLine = true,
                        colors = com.example.ui.components.focusTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                    )

                    OutlinedTextField(
                        value = maxMarksText,
                        onValueChange = { maxMarksText = it },
                        label = { Text("Maximum Marks") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = com.example.ui.components.focusTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("category_max_marks_input")
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Performance Weight %") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = com.example.ui.components.focusTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("category_weight_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val maxMarks = maxMarksText.toDoubleOrNull() ?: 10.0
                        val weight = weightText.toFloatOrNull() ?: 20f
                        if (name.isNotBlank()) {
                            val entity = if (target != null) {
                                target.copy(
                                    name = name.trim(),
                                    maxMarks = maxMarks,
                                    weight = weight,
                                    colorHex = colorHex
                                )
                            } else {
                                AssessmentCategoryEntity(
                                    name = name.trim(),
                                    maxMarks = maxMarks,
                                    weight = weight,
                                    isSystemDefault = false,
                                    colorHex = colorHex,
                                    displayOrder = categories.size + 1
                                )
                            }
                            viewModel.saveCategory(entity)
                            showAddDialog = false
                            categoryToEdit = null
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = FocusPrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    categoryToEdit = null
                }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Delete Category", fontWeight = FontWeight.Bold) },
            text = { Text("Delete '${cat.name}'? Existing marks in this category will also be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }
}

@Composable
fun CategoryRowCard(
    category: AssessmentCategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("category_card_${category.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A4DA2).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = FocusPrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusTextDark
                    )
                    if (category.isSystemDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Default", fontSize = 10.sp, color = FocusTextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Max Marks: ${category.maxMarks.toInt()}  •  Weight: ${category.weight.toInt()}%",
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = FocusTextSecondary, modifier = Modifier.size(18.dp))
            }

            if (!category.isSystemDefault) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FocusErrorRed, modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "System Default",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp).padding(end = 6.dp)
                )
            }
        }
    }
}
