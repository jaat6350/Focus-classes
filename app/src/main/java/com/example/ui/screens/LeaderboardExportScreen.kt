package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardFilterCategory
import com.example.ui.components.FocusTopAppBar
import com.example.ui.components.focusTextFieldColors
import com.example.ui.export.ExportFormat
import com.example.ui.export.ExportStatsCalculator
import com.example.ui.export.LeaderboardExportManager
import com.example.ui.export.LeaderboardRenderer
import com.example.ui.theme.FocusBackground
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusNavy
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusTextDark
import com.example.ui.theme.FocusTextSecondary
import com.example.ui.viewmodel.FocusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LeaderboardExportScreen(
    initialBatchId: Long = 0,
    initialAssessmentId: Long = 0,
    viewModel: FocusViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allBatches by viewModel.allBatches.collectAsState()
    val selectedBatchId by viewModel.selectedBatchId.collectAsState()
    val selectedBatch by viewModel.currentBatch.collectAsState()
    val assessments by viewModel.assessmentsForBatch.collectAsState()
    val selectedAssessment by viewModel.currentAssessment.collectAsState()
    val rankings by viewModel.dynamicLeaderboardRankings.collectAsState()
    val currentFilter by viewModel.leaderboardFilter.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var showBatchDropdown by remember { mutableStateOf(false) }
    var showWeekDropdown by remember { mutableStateOf(false) }

    // Multi-page navigation for preview
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var previewKey by remember { mutableIntStateOf(0) }

    // Initialize Batch & Assessment
    LaunchedEffect(initialBatchId) {
        if (initialBatchId > 0 && selectedBatchId != initialBatchId) {
            viewModel.setBatch(initialBatchId)
        }
    }

    LaunchedEffect(initialAssessmentId, assessments) {
        if (initialAssessmentId > 0 && selectedAssessment?.id != initialAssessmentId) {
            viewModel.setAssessment(initialAssessmentId)
        } else if (selectedAssessment == null && assessments.isNotEmpty()) {
            viewModel.setAssessment(assessments.first().id)
        }
    }

    // Dynamic Filter Options
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

    val totalPages = remember(rankings.size) {
        LeaderboardRenderer.calculateTotalPages(rankings.size)
    }

    // Ensure current page is valid
    LaunchedEffect(totalPages) {
        if (currentPageIndex >= totalPages) {
            currentPageIndex = 0
        }
    }

    // Compute Statistics Dynamically using single source of truth
    val stats = remember(rankings) {
        ExportStatsCalculator.calculate(rankings)
    }

    // Render Preview Bitmap whenever selection or page changes
    LaunchedEffect(selectedBatch, selectedAssessment, currentFilter, rankings, currentPageIndex, previewKey) {
        if (selectedBatch != null && selectedAssessment != null) {
            isGeneratingPreview = true
            withContext(Dispatchers.Default) {
                try {
                    val bmp = LeaderboardRenderer.renderPageBitmap(
                        context = context,
                        batch = selectedBatch,
                        assessment = selectedAssessment,
                        filter = currentFilter,
                        rankedItems = rankings,
                        stats = stats,
                        pageIndex = currentPageIndex,
                        totalPages = totalPages
                    )
                    previewBitmap = bmp
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isGeneratingPreview = false
                }
            }
        } else {
            previewBitmap = null
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            FocusTopAppBar(
                title = "Export Leaderboard",
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { previewKey++ },
                        enabled = !isGeneratingPreview && !isExporting,
                        modifier = Modifier.testTag("refresh_preview_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Preview", tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FocusBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .testTag("leaderboard_export_screen_root")
        ) {
            // Configuration Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Poster Configuration",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusTextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Batch / Class Selector
                    Text(
                        text = "Select Class / Batch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBatch?.name ?: "Select Class / Batch",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBatchDropdown = true }
                                .testTag("export_batch_dropdown"),
                            shape = RoundedCornerShape(10.dp),
                            colors = focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
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
                                        currentPageIndex = 0
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Week / Assessment Selector
                    Text(
                        text = "Assessment Period",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (assessments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF4E5), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No assessment weeks created for this batch yet. Please create an assessment week first.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedAssessment?.weekLabel ?: "Select Assessment Week",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FocusTextDark) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWeekDropdown = true }
                                    .testTag("export_week_dropdown"),
                                shape = RoundedCornerShape(10.dp),
                                colors = focusTextFieldColors(containerColor = Color(0xFFF6F8FC))
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
                                            currentPageIndex = 0
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Leaderboard Type Selector (Chips)
                    Text(
                        text = "Leaderboard Type",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterOptions) { opt ->
                            FilterChip(
                                selected = currentFilter.id == opt.id,
                                onClick = {
                                    viewModel.setLeaderboardFilter(opt)
                                    currentPageIndex = 0
                                },
                                label = { Text(opt.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FocusPrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Export Format Selector (PNG, PDF, JPEG)
                    Text(
                        text = "Export Format",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FocusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportFormat.values().forEach { fmt ->
                            val isSelected = selectedFormat == fmt
                            val bg = if (isSelected) FocusNavy else Color(0xFFF1F5F9)
                            val textColor = if (isSelected) Color.White else FocusTextDark
                            val borderColor = if (isSelected) FocusGold else Color(0xFFCBD5E1)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clickable { selectedFormat = fmt }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = fmt.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (fmt == ExportFormat.PDF) "Print Ready" else "2400×3600",
                                        fontSize = 10.sp,
                                        color = if (isSelected) FocusGold else FocusTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Warning / Recommendation for Large Classes
                    if (rankings.size > 10 && selectedFormat != ExportFormat.PDF) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = FocusPrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This class has ${rankings.size} students. Multi-page PDF is recommended for full readability when printing.",
                                    fontSize = 12.sp,
                                    color = FocusPrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            // Live Preview Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Poster Preview",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusTextDark
                        )

                        if (totalPages > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                    enabled = currentPageIndex > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Page", tint = FocusNavy)
                                }
                                Text(
                                    text = "Page ${currentPageIndex + 1} / $totalPages",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTextDark
                                )
                                IconButton(
                                    onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                                    enabled = currentPageIndex < totalPages - 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", tint = FocusNavy)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Poster Canvas / Frame Container (2:3 aspect ratio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFDCE5F0), RoundedCornerShape(12.dp))
                            .background(Color(0xFFF6F8FC)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            selectedAssessment == null || assessments.isEmpty() -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = FocusTextSecondary, modifier = Modifier.size(44.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Please select an assessment week to view the leaderboard poster preview.",
                                        color = FocusTextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                            isGeneratingPreview -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = FocusPrimaryBlue, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Rendering high-res preview...", color = FocusTextSecondary, fontSize = 13.sp)
                                }
                            }
                            previewBitmap != null -> {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Live Leaderboard Poster Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Text("Preview unavailable", color = FocusTextSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Card (Export & Share)
            val canExport = selectedBatch != null && selectedAssessment != null && !isExporting

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Export Button
                    Button(
                        onClick = {
                            val b = selectedBatch ?: return@Button
                            val a = selectedAssessment ?: return@Button
                            isExporting = true
                            scope.launch {
                                try {
                                    val genResult = LeaderboardExportManager.generateExportFile(
                                        context = context,
                                        batch = b,
                                        assessment = a,
                                        filter = currentFilter,
                                        rankedItems = rankings,
                                        stats = stats,
                                        format = selectedFormat
                                    )
                                    if (genResult.isSuccess) {
                                        val (file, _) = genResult.getOrThrow()
                                        val saveResult = LeaderboardExportManager.saveToDeviceStorage(context, file, selectedFormat)
                                        if (saveResult.isSuccess) {
                                            snackbarHostState.showSnackbar(
                                                "Successfully exported ${selectedFormat.name} to ${if (selectedFormat == ExportFormat.PDF) "Downloads" else "Pictures"} folder!"
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar("File created in app storage: ${file.name}")
                                        }
                                    } else {
                                        val err = genResult.exceptionOrNull()?.message ?: "Unknown error"
                                        snackbarHostState.showSnackbar("Export failed: $err")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = canExport,
                        colors = ButtonDefaults.buttonColors(containerColor = FocusNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("export_save_button")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating ${selectedFormat.name}...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                if (selectedFormat == ExportFormat.PDF) Icons.Default.PictureAsPdf else Icons.Default.Download,
                                contentDescription = null,
                                tint = FocusGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Export & Save ${selectedFormat.displayName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Share Button (Direct Android Sharesheet)
                    OutlinedButton(
                        onClick = {
                            val b = selectedBatch ?: return@OutlinedButton
                            val a = selectedAssessment ?: return@OutlinedButton
                            isExporting = true
                            scope.launch {
                                try {
                                    val genResult = LeaderboardExportManager.generateExportFile(
                                        context = context,
                                        batch = b,
                                        assessment = a,
                                        filter = currentFilter,
                                        rankedItems = rankings,
                                        stats = stats,
                                        format = selectedFormat
                                    )
                                    if (genResult.isSuccess) {
                                        val (_, uri) = genResult.getOrThrow()
                                        LeaderboardExportManager.shareExportedFile(
                                            context = context,
                                            fileUri = uri,
                                            format = selectedFormat,
                                            title = "Share ${b.name} - ${a.weekLabel} Leaderboard"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to prepare file for sharing")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Share failed: ${e.message}")
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = canExport,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FocusPrimaryBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FocusPrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = FocusPrimaryBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Share File Directly (WhatsApp, Email...)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusPrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
