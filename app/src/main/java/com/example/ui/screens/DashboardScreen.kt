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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FocusBottomNavBar
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusNavy
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun DashboardScreen(
    viewModel: FocusViewModel
) {
    val totalBatches by viewModel.totalBatches.collectAsState()
    val totalStudents by viewModel.totalStudents.collectAsState()
    val completedRecords by viewModel.completedRecords.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Focus Classes",
                showBrandLogo = true,
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("admin_profile_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(FocusPrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Admin Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("dashboard_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card (Matches UI Reference)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F1FC)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(FocusPrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Welcome Back!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Manage batches, students and track performance easily.",
                                fontSize = 13.sp,
                                color = FocusTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Live Database Statistics Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "Batches",
                        value = totalBatches.toString(),
                        subtitle = "Active Classes",
                        color = FocusPrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "Students",
                        value = totalStudents.toString(),
                        subtitle = if (totalStudents == 0) "None yet" else "Enrolled",
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "Records",
                        value = completedRecords.toString(),
                        subtitle = "Evaluated",
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Main Action Cards (Matches 4 main options in reference image)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardActionItem(
                        title = "Batches",
                        subtitle = "View & manage all batches",
                        icon = Icons.Default.Group,
                        iconBg = Color(0xFF1E88E5),
                        onClick = { viewModel.navigateTo(Screen.Batches) },
                        testTag = "dashboard_action_batches"
                    )

                    DashboardActionItem(
                        title = "Students",
                        subtitle = "Add, edit or remove students",
                        icon = Icons.Default.Person,
                        iconBg = Color(0xFF8E24AA),
                        onClick = {
                            val firstBatch = viewModel.allBatches.value.firstOrNull()?.id ?: 1L
                            viewModel.navigateTo(Screen.BatchStudents(firstBatch))
                        },
                        testTag = "dashboard_action_students"
                    )

                    DashboardActionItem(
                        title = "Daily Attendance",
                        subtitle = "Calendar marking, Mon–Sat (auto weekly sync)",
                        icon = Icons.Default.DateRange,
                        iconBg = Color(0xFF22C55E),
                        onClick = {
                            val firstBatch = viewModel.allBatches.value.firstOrNull()?.id ?: 1L
                            viewModel.navigateTo(Screen.Attendance(firstBatch))
                        },
                        testTag = "dashboard_action_attendance"
                    )

                    DashboardActionItem(
                        title = "Weekly Assessments",
                        subtitle = "Auto Monday–Saturday weeks & marks entry",
                        icon = Icons.Default.Assignment,
                        iconBg = Color(0xFF0284C7),
                        onClick = {
                            val firstBatch = viewModel.allBatches.value.firstOrNull()?.id ?: 1L
                            viewModel.navigateTo(Screen.WeeklyAssessments(firstBatch))
                        },
                        testTag = "dashboard_action_assessments"
                    )

                    DashboardActionItem(
                        title = "Assessment Categories",
                        subtitle = "Custom categories, weights & max marks",
                        icon = Icons.Default.Tune,
                        iconBg = Color(0xFF6366F1),
                        onClick = { viewModel.navigateTo(Screen.CustomCategories) },
                        testTag = "dashboard_action_categories"
                    )

                    DashboardActionItem(
                        title = "Leaderboard",
                        subtitle = "View class rankings",
                        icon = Icons.Default.EmojiEvents,
                        iconBg = Color(0xFFFB8C00),
                        onClick = { viewModel.navigateTo(Screen.Leaderboard()) },
                        testTag = "dashboard_action_leaderboard"
                    )

                    DashboardActionItem(
                        title = "Settings",
                        subtitle = "App settings & preferences",
                        icon = Icons.Default.Settings,
                        iconBg = Color(0xFF00ACC1),
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        testTag = "dashboard_action_settings"
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusTextDark
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = FocusTextSecondary
            )
        }
    }
}

@Composable
fun DashboardActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = FocusTextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = FocusTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
