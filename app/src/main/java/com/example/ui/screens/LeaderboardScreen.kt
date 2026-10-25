package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardCategory
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.RankedStudentItem
import com.example.ui.components.FocusBottomNavBar
import com.example.ui.components.FocusTopAppBar
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.theme.MedalBronze
import com.example.ui.theme.MedalGold
import com.example.ui.theme.MedalSilver
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun LeaderboardScreen(
    defaultBatchId: Long,
    viewModel: FocusViewModel
) {
    val context = LocalContext.current
    val allBatches by viewModel.allBatches.collectAsState()
    val selectedBatchId by viewModel.selectedBatchId.collectAsState()
    val selectedBatch by viewModel.currentBatch.collectAsState()
    val assessments by viewModel.assessmentsForBatch.collectAsState()
    val selectedAssessment by viewModel.currentAssessment.collectAsState()
    val rankings by viewModel.dynamicLeaderboardRankings.collectAsState()
    val currentFilter by viewModel.leaderboardFilter.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()

    var showBatchDropdown by remember { mutableStateOf(false) }
    var showWeekDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(defaultBatchId) {
        if (defaultBatchId > 0) {
            viewModel.setBatch(defaultBatchId)
        }
    }

    LaunchedEffect(assessments) {
        if (selectedAssessment == null && assessments.isNotEmpty()) {
            viewModel.setAssessment(assessments.first().id)
        }
    }

    // Dynamic Filter Chips List: Overall + System Categories + Custom Categories
    val filterOptions = remember(allCategories) {
        val list = mutableListOf<LeaderboardFilterCategory>()
        list.add(LeaderboardFilterCategory(id = "OVERALL", title = "Overall Performance"))
        allCategories.forEach { cat ->
            val filterId = cat.systemKey ?: "CUSTOM_${cat.id}"
            list.add(
                LeaderboardFilterCategory(
                    id = filterId,
                    title = cat.name,
                    customCategoryId = if (cat.systemKey == null) cat.id else null
                )
            )
        }
        list
    }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Leaderboard",
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.exportCurrentLeaderboard { csv ->
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Leaderboard")
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier.testTag("export_leaderboard_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Color.White)
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
                .testTag("leaderboard_screen_root")
        ) {
            // Filter Header (Batch & Week Dropdowns)
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Batch Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBatch?.name ?: "Select Batch",
                            onValueChange = {},
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBatchDropdown = true }
                                .testTag("leaderboard_batch_dropdown"),
                            shape = RoundedCornerShape(10.dp),
                            colors = com.example.ui.components.focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
                        )

                        DropdownMenu(
                            expanded = showBatchDropdown,
                            onDismissRequest = { showBatchDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            allBatches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name, color = FocusTextDark) },
                                    onClick = {
                                        viewModel.setBatch(b.id)
                                        showBatchDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Week Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedAssessment?.weekLabel ?: "Select Assessment Week",
                            onValueChange = {},
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWeekDropdown = true }
                                .testTag("leaderboard_week_dropdown"),
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
                                        viewModel.setAssessment(a.id)
                                        showWeekDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create Assessment Week", color = FocusPrimaryBlue, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showWeekDropdown = false
                                    viewModel.navigateTo(Screen.AddEditAssessment(0, selectedBatchId))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Category Chips (Overall, Attendance, Test, Discipline, Project, + Any Custom Categories!)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterOptions) { opt ->
                            FilterChip(
                                selected = currentFilter.id == opt.id,
                                onClick = { viewModel.setLeaderboardFilter(opt) },
                                label = { Text(opt.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FocusPrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Table Header Row: Rank, Name, Score
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rank",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusTextSecondary,
                    modifier = Modifier.width(50.dp)
                )
                Text(
                    text = "Name",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (currentFilter.id == "OVERALL") "Overall %" else "Score",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusTextSecondary
                )
            }

            // Ranked List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (assessments.isEmpty()) {
                    item {
                        EmptyLeaderboardCard(
                            message = "No assessment weeks exist for this batch yet.\nCreate an assessment week to view rankings.",
                            buttonText = "Create Week 1 Assessment",
                            onAction = {
                                viewModel.navigateTo(Screen.AddEditAssessment(0, selectedBatchId))
                            }
                        )
                    }
                } else if (rankings.isEmpty()) {
                    item {
                        EmptyLeaderboardCard(
                            message = "No students have been added to this batch yet.",
                            buttonText = "Add Student",
                            onAction = {
                                viewModel.navigateTo(Screen.AddEditStudent(0, selectedBatchId))
                            }
                        )
                    }
                } else {
                    items(rankings, key = { it.student.id }) { item ->
                        LeaderboardRankRow(
                            item = item,
                            onClick = {
                                viewModel.navigateTo(Screen.StudentProfile(item.student.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardRankRow(
    item: RankedStudentItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("leaderboard_row_${item.rank}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Display with Medals for Top 3
            Box(
                modifier = Modifier.width(46.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when (item.rank) {
                    1 -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MedalGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🥇", fontSize = 18.sp)
                        }
                    }
                    2 -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MedalSilver.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🥈", fontSize = 18.sp)
                        }
                    }
                    3 -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MedalBronze.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🥉", fontSize = 18.sp)
                        }
                    }
                    else -> {
                        Text(
                            text = item.rank.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusTextDark,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            StudentAvatar(
                imagePath = item.student.profileImagePath,
                fullName = item.student.fullName,
                size = 40
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.student.fullName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                if (item.student.rollNumber.isNotBlank()) {
                    Text(
                        text = "Roll No. ${item.student.rollNumber}",
                        fontSize = 11.sp,
                        color = FocusTextSecondary
                    )
                }
            }

            // Score / Total Marks
            Text(
                text = item.scoreDisplay,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isComplete) FocusPrimaryBlue else FocusTextSecondary
            )
        }
    }
}

@Composable
fun EmptyLeaderboardCard(
    message: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = FocusGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = FocusTextSecondary,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onAction,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FocusPrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(buttonText)
            }
        }
    }
}
