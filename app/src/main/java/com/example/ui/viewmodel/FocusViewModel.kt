package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.BatchEntity
import com.example.data.model.BatchWithStudentCount
import com.example.data.model.DailyAttendanceEntity
import com.example.data.model.LeaderboardCategory
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.RankedStudentItem
import com.example.data.model.ScoringConfigEntity
import com.example.data.model.StudentCategoryMarkEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.StudentWithResult
import com.example.data.model.WeeklyAssessmentEntity
import com.example.data.repository.FocusRepository
import com.example.util.AttendanceDateUtils
import com.example.util.AttendanceSummaryStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Splash : Screen()
    object Dashboard : Screen()
    object Batches : Screen()
    data class AddEditBatch(val batchId: Long = 0) : Screen()
    data class BatchStudents(val batchId: Long) : Screen()
    data class AddEditStudent(val studentId: Long = 0, val defaultBatchId: Long = 0) : Screen()
    data class StudentProfile(val studentId: Long) : Screen()
    data class WeeklyAssessments(val batchId: Long = 0) : Screen()
    data class AddEditAssessment(val assessmentId: Long = 0, val defaultBatchId: Long = 0) : Screen()
    data class MarksEntry(val batchId: Long, val assessmentId: Long, val currentStudentId: Long = 0) : Screen()
    data class Leaderboard(val defaultBatchId: Long = 0) : Screen()
    data class LeaderboardExport(val batchId: Long = 0, val assessmentId: Long = 0) : Screen()
    data class Attendance(val batchId: Long = 0, val assessmentId: Long = 0, val initialDateMillis: Long = 0L) : Screen()
    object CustomCategories : Screen()
    object Settings : Screen()
    object BackupRestore : Screen()
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    val repository = FocusRepository(application)

    init {
        viewModelScope.launch {
            try {
                repository.ensureWeeklyAssessmentsForAllBatches()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Navigation Backstack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Splash))
    val currentScreen: StateFlow<Screen> = _screenStack.flatMapLatest { stack ->
        flowOf(stack.lastOrNull() ?: Screen.Dashboard)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Splash)

    fun navigateTo(screen: Screen) {
        val current = _screenStack.value
        _screenStack.value = current + screen
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            return true
        }
        return false
    }

    fun replaceScreen(screen: Screen) {
        _screenStack.value = listOf(screen)
    }

    // Bottom bar tab selection
    val currentTab = MutableStateFlow(0) // 0: Home, 1: Batches, 2: Students, 3: Leaderboard
    fun selectTab(tabIndex: Int) {
        currentTab.value = tabIndex
        when (tabIndex) {
            0 -> _screenStack.value = listOf(Screen.Dashboard)
            1 -> _screenStack.value = listOf(Screen.Dashboard, Screen.Batches)
            2 -> {
                val firstBatch = allBatches.value.firstOrNull()?.id ?: 1L
                _screenStack.value = listOf(Screen.Dashboard, Screen.BatchStudents(firstBatch))
            }
            3 -> _screenStack.value = listOf(Screen.Dashboard, Screen.Leaderboard())
        }
    }

    // UI Feedback Message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    fun clearUserMessage() { _userMessage.value = null }
    fun showMessage(msg: String) { _userMessage.value = msg }

    // Dashboard Statistics
    val totalBatches = repository.totalBatchesCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalStudents = repository.totalStudentsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val completedRecords = repository.completedRecordsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Batches
    val batchesWithCount = repository.batchesWithCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allBatches = repository.batchesWithCount.flatMapLatest { list ->
        flowOf(list.map { it.batch })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search queries
    val batchSearchQuery = MutableStateFlow("")
    val studentSearchQuery = MutableStateFlow("")

    // Filtered Batches
    val filteredBatches = combine(batchesWithCount, batchSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.batch.name.contains(query, ignoreCase = true) ||
            "Class ${it.batch.grade}".contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Batch for Student List
    val selectedBatchId = MutableStateFlow(1L)
    fun setBatch(batchId: Long) { selectedBatchId.value = batchId }

    val currentBatch = selectedBatchId.flatMapLatest { id ->
        repository.getBatchFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Students in Selected Batch
    val studentsInBatch = selectedBatchId.flatMapLatest { id ->
        repository.getStudentsByBatch(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStudents = combine(studentsInBatch, studentSearchQuery) { students, query ->
        if (query.isBlank()) students
        else students.filter {
            it.fullName.contains(query, ignoreCase = true) ||
            it.rollNumber.contains(query, ignoreCase = true) ||
            it.studentCode.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-select student mode
    val isMultiSelectMode = MutableStateFlow(false)
    val selectedStudentIds = MutableStateFlow<Set<Long>>(emptySet())

    fun toggleStudentSelection(id: Long) {
        val current = selectedStudentIds.value
        selectedStudentIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAllStudents() {
        selectedStudentIds.value = filteredStudents.value.map { it.id }.toSet()
    }

    fun clearStudentSelection() {
        selectedStudentIds.value = emptySet()
        isMultiSelectMode.value = false
    }

    fun deleteSelectedStudents() {
        val ids = selectedStudentIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.deleteStudentsByIds(ids)
                showMessage("Deleted ${ids.size} student(s)")
                clearStudentSelection()
            }
        }
    }

    // Assessments
    val assessmentsForBatch = selectedBatchId.flatMapLatest { id ->
        repository.getAssessmentsByBatch(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAssessmentId = MutableStateFlow<Long?>(null)
    fun setAssessment(id: Long?) { selectedAssessmentId.value = id }

    val currentAssessment = selectedAssessmentId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getAssessmentFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Categories
    val allCategories = repository.allCategories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun saveCategory(category: AssessmentCategoryEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveCategory(category)
            showMessage("Category '${category.name}' saved")
            onComplete()
        }
    }

    fun deleteCategory(category: AssessmentCategoryEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showMessage("Category '${category.name}' removed")
            onComplete()
        }
    }

    // Batch Assessment Students
    val batchStudentsWithResults = combine(selectedBatchId, selectedAssessmentId) { bId, aId ->
        Pair(bId, aId)
    }.flatMapLatest { (bId, aId) ->
        if (aId == null) flowOf(emptyList())
        else repository.getBatchAssessmentStudents(bId, aId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Leaderboard state
    val leaderboardFilter = MutableStateFlow(
        LeaderboardFilterCategory(id = "OVERALL", title = "Overall Performance")
    )
    fun setLeaderboardFilter(filter: LeaderboardFilterCategory) {
        leaderboardFilter.value = filter
    }

    val dynamicLeaderboardRankings = combine(
        selectedBatchId,
        selectedAssessmentId,
        leaderboardFilter
    ) { bId, aId, filter ->
        Triple(bId, aId, filter)
    }.flatMapLatest { (bId, aId, filter) ->
        if (aId == null) flowOf(emptyList())
        else repository.getDynamicLeaderboard(bId, aId, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Calendar Attendance Tracking
    val selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateString = selectedDateMillis.flatMapLatest { millis ->
        flowOf(AttendanceDateUtils.toDateString(millis))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceDateUtils.toDateString(System.currentTimeMillis()))

    val selectedDayOfWeek = MutableStateFlow(AttendanceDateUtils.getDayOfWeek(System.currentTimeMillis()))

    fun setSelectedDate(millis: Long) {
        selectedDateMillis.value = millis
        selectedDayOfWeek.value = AttendanceDateUtils.getDayOfWeek(millis)
        viewModelScope.launch {
            val a = repository.ensureAssessmentForDate(selectedBatchId.value, millis)
            selectedAssessmentId.value = a.id
        }
    }

    fun previousDay() {
        setSelectedDate(AttendanceDateUtils.addDays(selectedDateMillis.value, -1))
    }

    fun nextDay() {
        setSelectedDate(AttendanceDateUtils.addDays(selectedDateMillis.value, 1))
    }

    fun jumpToToday() {
        setSelectedDate(System.currentTimeMillis())
    }

    fun setDayOfWeek(day: Int) {
        selectedDayOfWeek.value = day
        val mon = AttendanceDateUtils.getMondayMillis(selectedDateMillis.value)
        val newDate = AttendanceDateUtils.addDays(mon, day - 1)
        selectedDateMillis.value = newDate
        viewModelScope.launch {
            val a = repository.ensureAssessmentForDate(selectedBatchId.value, newDate)
            selectedAssessmentId.value = a.id
        }
    }

    val calendarAttendanceRecords: StateFlow<List<DailyAttendanceEntity>> = combine(
        selectedBatchId,
        selectedDateString
    ) { bId: Long, dateStr: String ->
        Pair(bId, dateStr)
    }.flatMapLatest { pair: Pair<Long, String> ->
        repository.getAttendanceForBatchAndDate(pair.first, pair.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyAttendanceRecords: StateFlow<List<DailyAttendanceEntity>> = combine(
        selectedBatchId,
        selectedAssessmentId,
        selectedDayOfWeek
    ) { bId: Long, aId: Long?, day: Int ->
        Triple(bId, aId, day)
    }.flatMapLatest { triple: Triple<Long, Long?, Int> ->
        val aId = triple.second
        if (aId == null) flowOf(emptyList())
        else repository.getDailyAttendance(triple.first, aId, triple.third)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markStudentAttendance(
        studentId: Long,
        status: AttendanceStatus
    ) {
        val bId = selectedBatchId.value
        val dateMillis = selectedDateMillis.value
        val dateStr = AttendanceDateUtils.toDateString(dateMillis)
        val day = AttendanceDateUtils.getDayOfWeek(dateMillis)

        viewModelScope.launch {
            val record = DailyAttendanceEntity(
                studentId = studentId,
                batchId = bId,
                dateString = dateStr,
                dayOfWeek = day,
                status = status,
                score = status.score,
                dateMillis = dateMillis
            )
            repository.saveDailyAttendance(listOf(record), bId, dateMillis)
        }
    }

    fun markAllStudentsAttendance(status: AttendanceStatus) {
        val bId = selectedBatchId.value
        val dateMillis = selectedDateMillis.value
        val dateStr = AttendanceDateUtils.toDateString(dateMillis)
        val day = AttendanceDateUtils.getDayOfWeek(dateMillis)
        val students = studentsInBatch.value

        viewModelScope.launch {
            val records = students.map { s ->
                DailyAttendanceEntity(
                    studentId = s.id,
                    batchId = bId,
                    dateString = dateStr,
                    dayOfWeek = day,
                    status = status,
                    score = status.score,
                    dateMillis = dateMillis
                )
            }
            repository.saveDailyAttendance(records, bId, dateMillis)
            showMessage("Marked all as ${status.label} for ${AttendanceDateUtils.formatDisplayDate(dateStr)}")
        }
    }

    fun ensureCurrentWeekAssessmentForBatch(batchId: Long, onReady: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val assessment = repository.ensureCurrentWeekAssessment(batchId)
            selectedAssessmentId.value = assessment.id
            onReady(assessment.id)
        }
    }

    // Scoring Configuration
    val scoringConfig = repository.scoringConfig.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ScoringConfigEntity()
    )

    fun updateScoringConfig(config: ScoringConfigEntity) {
        viewModelScope.launch {
            repository.updateScoringConfig(config)
            showMessage("Scoring weights updated successfully")
        }
    }

    // Batch operations
    fun saveBatch(batch: BatchEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveBatch(batch)
            showMessage("Batch '${batch.name}' saved")
            onComplete()
        }
    }

    fun deleteBatch(batch: BatchEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteBatch(batch)
            showMessage("Batch deleted")
            onComplete()
        }
    }

    // Student operations
    fun saveStudent(
        student: StudentEntity,
        imageUri: Uri? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            var imagePath = student.profileImagePath
            if (imageUri != null) {
                try {
                    imagePath = repository.copyImageToLocalStorage(imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.saveStudent(student.copy(profileImagePath = imagePath))
            showMessage("Student '${student.fullName}' saved")
            onComplete()
        }
    }

    fun deleteStudent(student: StudentEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            showMessage("Student '${student.fullName}' deleted")
            onComplete()
        }
    }

    // Assessment operations
    fun saveAssessment(assessment: WeeklyAssessmentEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveAssessment(assessment)
            selectedAssessmentId.value = id
            showMessage("Assessment '${assessment.weekLabel}' saved")
            onComplete(id)
        }
    }

    fun deleteAssessment(assessment: WeeklyAssessmentEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAssessment(assessment)
            showMessage("Assessment deleted")
            onComplete()
        }
    }

    // Marks saving
    fun saveStudentMarks(
        result: StudentWeeklyResultEntity,
        customMarks: List<StudentCategoryMarkEntity> = emptyList(),
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveResult(result)
            if (customMarks.isNotEmpty()) {
                repository.saveCategoryMarks(customMarks)
            }
            showMessage("Marks saved successfully")
            onComplete()
        }
    }

    // Export CSV
    fun exportCurrentLeaderboard(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val batchName = currentBatch.value?.name ?: "Batch"
            val weekLabel = currentAssessment.value?.weekLabel ?: "Weekly Assessment"
            val csv = repository.exportLeaderboardToCsv(batchName, weekLabel, dynamicLeaderboardRankings.value)
            onComplete(csv)
        }
    }
}
