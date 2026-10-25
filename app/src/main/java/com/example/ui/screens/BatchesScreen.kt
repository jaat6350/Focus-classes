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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatchEntity
import com.example.data.model.BatchWithStudentCount
import com.example.ui.components.FocusBottomNavBar
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

// Vibrant badge colors matching UI reference
private val BatchBadgeColors = listOf(
    Color(0xFF1E88E5), // Class 1 - Blue
    Color(0xFF43A047), // Class 2 - Green
    Color(0xFF00ACC1), // Class 3 - Teal
    Color(0xFFFB8C00), // Class 4 - Orange
    Color(0xFF2E7D32), // Class 5 - Dark Green
    Color(0xFFE53935), // Class 6 - Red
    Color(0xFF1976D2), // Class 7 - Blue
    Color(0xFFF4511E), // Class 8 - Deep Orange
    Color(0xFFC2185B), // Class 9 - Pink/Red
    Color(0xFF00897B), // Class 10 - Emerald
    Color(0xFF7B1FA2), // Class 11 - Purple
    Color(0xFF1565C0)  // Class 12 - Indigo
)

@Composable
fun BatchesScreen(
    viewModel: FocusViewModel
) {
    val batchesWithCount by viewModel.filteredBatches.collectAsState()
    val searchQuery by viewModel.batchSearchQuery.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()

    var batchToDelete by remember { mutableStateOf<BatchEntity?>(null) }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Batches",
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.AddEditBatch(batchId = 0)) },
                        modifier = Modifier.testTag("add_batch_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Batch",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            FocusBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = FocusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("batches_screen_root")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(batchesWithCount, key = { it.batch.id }) { item ->
                    BatchItemCard(
                        item = item,
                        onClick = {
                            viewModel.setBatch(item.batch.id)
                            viewModel.navigateTo(Screen.BatchStudents(item.batch.id))
                        },
                        onEdit = {
                            viewModel.navigateTo(Screen.AddEditBatch(item.batch.id))
                        },
                        onDelete = {
                            batchToDelete = item.batch
                        }
                    )
                }

                if (batchesWithCount.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No batches found.",
                                color = FocusTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting a batch
    batchToDelete?.let { batch ->
        AlertDialog(
            onDismissRequest = { batchToDelete = null },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Delete Batch", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${batch.name}'? Students in this batch will also be removed. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBatch(batch)
                        batchToDelete = null
                    }
                ) {
                    Text("Delete", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { batchToDelete = null }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }
}

@Composable
fun BatchItemCard(
    item: BatchWithStudentCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val colorIndex = ((item.batch.grade - 1).coerceAtLeast(0)) % BatchBadgeColors.size
    val badgeColor = BatchBadgeColors[colorIndex]

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("batch_item_${item.batch.grade}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numbered Circle Badge (e.g. 1, 2, 6, 10...)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.batch.grade.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.batch.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.studentCount} Students",
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
            }

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
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Batch") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Batch", color = FocusErrorRed) },
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
