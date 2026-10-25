package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScoringConfigEntity
import com.example.ui.components.FocusBottomNavBar
import com.example.ui.components.FocusTopAppBar
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusErrorRed
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun SettingsScreen(
    viewModel: FocusViewModel
) {
    val context = LocalContext.current
    val config by viewModel.scoringConfig.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()

    var showWeightsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Settings",
                onBackClick = { viewModel.navigateBack() }
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
                .testTag("settings_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SettingsCardItem(
                    title = "App Theme",
                    subtitle = "Light / Dark",
                    icon = Icons.Default.ColorLens,
                    onClick = {
                        viewModel.showMessage("Defaulting to Focus Classes Blue & White theme.")
                    }
                )
            }

            item {
                SettingsCardItem(
                    title = "Assessment Categories",
                    subtitle = "Add, edit, or remove categories beyond defaults & set max marks",
                    icon = Icons.Default.Tune,
                    onClick = { viewModel.navigateTo(Screen.CustomCategories) }
                )
            }

            item {
                SettingsCardItem(
                    title = "Category Weights & Scoring",
                    subtitle = "Attendance (${config.attendanceWeight.toInt()}%), Test (${config.testWeight.toInt()}%), Discipline (${config.disciplineWeight.toInt()}%), Project (${config.projectWeight.toInt()}%)",
                    icon = Icons.Default.Tune,
                    onClick = { showWeightsDialog = true }
                )
            }

            item {
                SettingsCardItem(
                    title = "Notification Settings",
                    subtitle = "Assessment reminders & alerts",
                    icon = Icons.Default.Notifications,
                    onClick = {
                        viewModel.showMessage("Weekly reminders are active.")
                    }
                )
            }

            item {
                SettingsCardItem(
                    title = "Backup & Restore",
                    subtitle = "Export data & restore records",
                    icon = Icons.Default.Backup,
                    onClick = {
                        viewModel.exportCurrentLeaderboard { csv ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csv)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Backup Leaderboard"))
                        }
                    }
                )
            }

            item {
                SettingsCardItem(
                    title = "About Focus Classes",
                    subtitle = "Learn • Grow • Excel",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsCardItem(
                    title = "Help & Support",
                    subtitle = "User guide and contact info",
                    icon = Icons.AutoMirrored.Filled.Help,
                    onClick = { showHelpDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusErrorRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_reset_button")
                ) {
                    Text("Clear All Data", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Category Weights Dialog
    if (showWeightsDialog) {
        var attWeight by remember { mutableStateOf(config.attendanceWeight.toInt().toString()) }
        var testWeight by remember { mutableStateOf(config.testWeight.toInt().toString()) }
        var discWeight by remember { mutableStateOf(config.disciplineWeight.toInt().toString()) }
        var projWeight by remember { mutableStateOf(config.projectWeight.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showWeightsDialog = false },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Scoring Weights (Total 100%)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = attWeight,
                        onValueChange = { attWeight = it },
                        label = { Text("Attendance Weight %") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = com.example.ui.components.focusTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = testWeight,
                        onValueChange = { testWeight = it },
                        label = { Text("Test Marks Weight %") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = com.example.ui.components.focusTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = discWeight,
                        onValueChange = { discWeight = it },
                        label = { Text("Discipline Weight %") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = com.example.ui.components.focusTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = projWeight,
                        onValueChange = { projWeight = it },
                        label = { Text("Project Weight %") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = FocusTextDark, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = com.example.ui.components.focusTextFieldColors(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val a = attWeight.toFloatOrNull() ?: 25f
                        val t = testWeight.toFloatOrNull() ?: 25f
                        val d = discWeight.toFloatOrNull() ?: 25f
                        val p = projWeight.toFloatOrNull() ?: 25f
                        viewModel.updateScoringConfig(
                            ScoringConfigEntity(
                                id = 1,
                                attendanceWeight = a,
                                testWeight = t,
                                disciplineWeight = d,
                                projectWeight = p
                            )
                        )
                        showWeightsDialog = false
                    }
                ) {
                    Text("Save Weights", fontWeight = FontWeight.Bold, color = FocusPrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightsDialog = false }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Focus Classes", fontWeight = FontWeight.Bold, color = FocusPrimaryBlue) },
            text = {
                Column {
                    Text("Learn • Grow • Excel", fontWeight = FontWeight.SemiBold, color = FocusGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Track Progress. Build Leaders.\n\nInternal student performance and batch management system for coaching institute administrators and teachers.", color = FocusTextDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Website: www.focusclasses.co.in", color = FocusPrimaryBlue)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.focusclasses.co.in/"))
                    context.startActivity(intent)
                    showAboutDialog = false
                }) {
                    Text("Visit Website", color = FocusPrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = FocusTextSecondary)
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Help & User Guide", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("1. Batches: Class 1–12 are pre-created. You can add more batches via the '+' button.\n\n2. Add Students: Upload their real photo and enter their name.\n\n3. Weekly Marks: Select a week, enter attendance (out of 6), test marks, discipline (out of 4), and project (out of 10).\n\n4. Leaderboard: Automatically computes normalized percentages and assigns class ranks with gold, silver, and bronze badges.", color = FocusTextDark)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It", fontWeight = FontWeight.Bold, color = FocusPrimaryBlue)
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color.White,
            titleContentColor = FocusTextDark,
            textContentColor = FocusTextDark,
            title = { Text("Clear All Data", fontWeight = FontWeight.Bold, color = FocusErrorRed) },
            text = { Text("This will permanently delete all student records and weekly marks from this device. Are you sure?", color = FocusTextDark) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.showMessage("To clear data, reinstall or clear app storage.")
                }) {
                    Text("Clear", color = FocusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = FocusTextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FocusPrimaryBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = FocusTextSecondary
                )
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
