package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.FocusDatabase
import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.BatchEntity
import com.example.data.model.BatchWithStudentCount
import com.example.data.model.DailyAttendanceEntity
import com.example.data.model.LeaderboardCategory
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.RankedStudentItem
import com.example.data.model.ScoringConfigEntity
import com.example.data.model.ScoringEngine
import com.example.data.model.StudentCategoryMarkEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.StudentWithResult
import com.example.data.model.WeeklyAssessmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FocusRepository(private val context: Context) {
    private val database = FocusDatabase.getDatabase(context)
    private val batchDao = database.batchDao()
    private val studentDao = database.studentDao()
    private val assessmentDao = database.assessmentDao()
    private val resultDao = database.resultDao()
    private val scoringConfigDao = database.scoringConfigDao()
    private val categoryDao = database.categoryDao()
    private val categoryMarkDao = database.categoryMarkDao()
    private val attendanceDao = database.attendanceDao()

    // Batches with Student Count
    val batchesWithCount: Flow<List<BatchWithStudentCount>> =
        combine(batchDao.getAllBatches(), studentDao.getAllStudents()) { batches, students ->
            val countMap = students.groupBy { it.batchId }
            batches.map { batch ->
                BatchWithStudentCount(
                    batch = batch,
                    studentCount = countMap[batch.id]?.size ?: 0
                )
            }
        }.flowOn(Dispatchers.IO)

    fun getBatchFlow(batchId: Long): Flow<BatchEntity?> = batchDao.getBatchFlow(batchId)

    suspend fun getBatchById(batchId: Long): BatchEntity? = batchDao.getBatchById(batchId)

    suspend fun saveBatch(batch: BatchEntity): Long = withContext(Dispatchers.IO) {
        if (batch.id == 0L) {
            batchDao.insertBatch(batch)
        } else {
            batchDao.updateBatch(batch)
            batch.id
        }
    }

    suspend fun deleteBatch(batch: BatchEntity) = withContext(Dispatchers.IO) {
        batchDao.deleteBatch(batch)
    }

    // Students
    fun getStudentsByBatch(batchId: Long): Flow<List<StudentEntity>> =
        studentDao.getStudentsByBatch(batchId).flowOn(Dispatchers.IO)

    fun getAllStudents(): Flow<List<StudentEntity>> =
        studentDao.getAllStudents().flowOn(Dispatchers.IO)

    fun getStudentFlow(studentId: Long): Flow<StudentEntity?> =
        studentDao.getStudentFlow(studentId).flowOn(Dispatchers.IO)

    suspend fun getStudentById(studentId: Long): StudentEntity? =
        studentDao.getStudentById(studentId)

    suspend fun saveStudent(student: StudentEntity): Long = withContext(Dispatchers.IO) {
        if (student.id == 0L) {
            val code = if (student.studentCode.isBlank()) {
                "FC-${1000 + (studentDao.getTotalStudentCount().first()) + 1}"
            } else student.studentCode
            studentDao.insertStudent(student.copy(studentCode = code))
        } else {
            studentDao.updateStudent(student)
            student.id
        }
    }

    suspend fun deleteStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        studentDao.deleteStudent(student)
    }

    suspend fun deleteStudentsByIds(studentIds: List<Long>) = withContext(Dispatchers.IO) {
        studentDao.deleteStudentsByIds(studentIds)
    }

    // Assessments
    fun getAssessmentsByBatch(batchId: Long): Flow<List<WeeklyAssessmentEntity>> =
        assessmentDao.getAssessmentsByBatch(batchId).flowOn(Dispatchers.IO)

    fun getAllAssessments(): Flow<List<WeeklyAssessmentEntity>> =
        assessmentDao.getAllAssessments().flowOn(Dispatchers.IO)

    fun getAssessmentFlow(assessmentId: Long): Flow<WeeklyAssessmentEntity?> =
        assessmentDao.getAssessmentFlow(assessmentId).flowOn(Dispatchers.IO)

    suspend fun getAssessmentById(assessmentId: Long): WeeklyAssessmentEntity? =
        assessmentDao.getAssessmentById(assessmentId)

    suspend fun saveAssessment(assessment: WeeklyAssessmentEntity): Long = withContext(Dispatchers.IO) {
        if (assessment.id == 0L) {
            assessmentDao.insertAssessment(assessment)
        } else {
            assessmentDao.updateAssessment(assessment)
            assessment.id
        }
    }

    suspend fun deleteAssessment(assessment: WeeklyAssessmentEntity) = withContext(Dispatchers.IO) {
        assessmentDao.deleteAssessment(assessment)
    }

    // Results & Scoring
    fun getResultsForAssessment(assessmentId: Long): Flow<List<StudentWeeklyResultEntity>> =
        resultDao.getResultsForAssessment(assessmentId).flowOn(Dispatchers.IO)

    fun getResultsForStudent(studentId: Long): Flow<List<StudentWeeklyResultEntity>> =
        resultDao.getResultsForStudent(studentId).flowOn(Dispatchers.IO)

    suspend fun getResult(studentId: Long, assessmentId: Long): StudentWeeklyResultEntity? =
        withContext(Dispatchers.IO) {
            resultDao.getResult(studentId, assessmentId)
        }

    fun getResultFlow(studentId: Long, assessmentId: Long): Flow<StudentWeeklyResultEntity?> =
        resultDao.getResultFlow(studentId, assessmentId).flowOn(Dispatchers.IO)

    suspend fun saveResult(result: StudentWeeklyResultEntity) = withContext(Dispatchers.IO) {
        resultDao.upsertResult(result.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveResults(results: List<StudentWeeklyResultEntity>) = withContext(Dispatchers.IO) {
        resultDao.upsertResults(results.map { it.copy(updatedAt = System.currentTimeMillis()) })
    }

    // Categories
    val allCategories: Flow<List<AssessmentCategoryEntity>> =
        categoryDao.getAllCategories().flowOn(Dispatchers.IO)

    suspend fun saveCategory(category: AssessmentCategoryEntity): Long = withContext(Dispatchers.IO) {
        if (category.id == 0L) {
            categoryDao.insertCategory(category)
        } else {
            categoryDao.updateCategory(category)
            category.id
        }
    }

    suspend fun deleteCategory(category: AssessmentCategoryEntity) = withContext(Dispatchers.IO) {
        if (!category.isSystemDefault) {
            categoryDao.deleteMarksForCategory(category.id)
            categoryDao.deleteCategory(category)
        }
    }

    // Custom Category Marks
    fun getCategoryMarksForAssessment(assessmentId: Long): Flow<List<StudentCategoryMarkEntity>> =
        categoryMarkDao.getMarksForAssessment(assessmentId).flowOn(Dispatchers.IO)

    fun getCustomMarksForStudentFlow(studentId: Long, assessmentId: Long): Flow<List<StudentCategoryMarkEntity>> =
        categoryMarkDao.getMarksForStudent(studentId, assessmentId).flowOn(Dispatchers.IO)

    suspend fun getCustomMarksForStudent(studentId: Long, assessmentId: Long): List<StudentCategoryMarkEntity> =
        withContext(Dispatchers.IO) {
            categoryMarkDao.getMarksForStudentDirect(studentId, assessmentId)
        }

    suspend fun saveCategoryMark(mark: StudentCategoryMarkEntity) = withContext(Dispatchers.IO) {
        categoryMarkDao.upsertMark(mark.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveCategoryMarks(marks: List<StudentCategoryMarkEntity>) = withContext(Dispatchers.IO) {
        categoryMarkDao.upsertMarks(marks.map { it.copy(updatedAt = System.currentTimeMillis()) })
    }

    // Daily Attendance Tracking
    fun getDailyAttendance(
        batchId: Long,
        assessmentId: Long,
        dayOfWeek: Int
    ): Flow<List<DailyAttendanceEntity>> =
        attendanceDao.getAttendanceForDay(batchId, assessmentId, dayOfWeek).flowOn(Dispatchers.IO)

    suspend fun recordAttendanceBatch(
        records: List<DailyAttendanceEntity>,
        batchId: Long,
        assessmentId: Long
    ) = withContext(Dispatchers.IO) {
        attendanceDao.upsertAttendanceBatch(records)

        // Automatically calculate each student's weekly attendance mark based on inserted daily data!
        val studentIds = records.map { it.studentId }.distinct()
        for (sId in studentIds) {
            val totalScore = attendanceDao.calculateWeeklyAttendanceScore(sId, assessmentId) ?: 0.0
            val cappedScore = totalScore.coerceIn(0.0, 6.0)

            // Update StudentWeeklyResultEntity
            val existingResult = resultDao.getResult(sId, assessmentId)
            val updatedResult = if (existingResult != null) {
                existingResult.copy(
                    attendanceMarks = cappedScore,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                StudentWeeklyResultEntity(
                    studentId = sId,
                    assessmentId = assessmentId,
                    attendanceMarks = cappedScore
                )
            }
            resultDao.upsertResult(updatedResult)
        }
    }

    // Scoring Config
    val scoringConfig: Flow<ScoringConfigEntity> =
        combine(scoringConfigDao.getConfig()) { configArray ->
            configArray.firstOrNull() ?: ScoringConfigEntity()
        }.flowOn(Dispatchers.IO)

    suspend fun updateScoringConfig(config: ScoringConfigEntity) = withContext(Dispatchers.IO) {
        scoringConfigDao.setConfig(config)
    }

    // Dashboard Statistics Flow
    val totalBatchesCount = batchDao.getBatchCount().flowOn(Dispatchers.IO)
    val totalStudentsCount = studentDao.getTotalStudentCount().flowOn(Dispatchers.IO)
    val completedRecordsCount = resultDao.getCompletedRecordsCount().flowOn(Dispatchers.IO)

    // Combined Batch + Assessment Student Performance Flow (incorporates custom category marks!)
    fun getBatchAssessmentStudents(
        batchId: Long,
        assessmentId: Long
    ): Flow<List<StudentWithResult>> = combine(
        studentDao.getStudentsByBatch(batchId),
        resultDao.getResultsForAssessment(assessmentId),
        categoryMarkDao.getMarksForAssessment(assessmentId),
        assessmentDao.getAssessmentFlow(assessmentId)
    ) { students, results, customMarks, assessment ->
        val resultMap = results.associateBy { it.studentId }
        val customMap = customMarks.groupBy { it.studentId }
            .mapValues { entry -> entry.value.associate { it.categoryId to it.marksObtained } }

        students.map { student ->
            StudentWithResult(
                student = student,
                result = resultMap[student.id],
                assessment = assessment,
                customCategoryMarks = customMap[student.id] ?: emptyMap()
            )
        }
    }.flowOn(Dispatchers.IO)

    // Dynamic Ranked Leaderboard Flow
    fun getDynamicLeaderboard(
        batchId: Long,
        assessmentId: Long,
        filter: LeaderboardFilterCategory
    ): Flow<List<RankedStudentItem>> = combine(
        getBatchAssessmentStudents(batchId, assessmentId),
        allCategories
    ) { studentsWithResults, categories ->
        ScoringEngine.rankStudentsDynamic(studentsWithResults, categories, filter)
    }.flowOn(Dispatchers.IO)

    // Save profile image to local app storage
    suspend fun copyImageToLocalStorage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "student_photos")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val filename = "student_${UUID.randomUUID()}.jpg"
        val destinationFile = File(imagesDir, filename)

        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        destinationFile.absolutePath
    }

    // Export student records & leaderboard to CSV
    suspend fun exportLeaderboardToCsv(
        batchName: String,
        weekLabel: String,
        items: List<RankedStudentItem>
    ): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("Batch: $batchName\n")
        sb.append("Assessment: $weekLabel\n\n")
        sb.append("Rank,Roll Number,Student Name,Attendance (out of 6),Test Marks,Discipline (out of 4),Project (out of 10),Overall Score\n")
        for (item in items) {
            val att = item.rawAttendance?.toString() ?: "N/A"
            val test = item.rawTest?.let { "$it/${item.testMaxMarks.toInt()}" } ?: "N/A"
            val disc = item.rawDiscipline?.toString() ?: "N/A"
            val proj = item.rawProject?.toString() ?: "N/A"
            sb.append("${item.rank},\"${item.student.rollNumber}\",\"${item.student.fullName}\",$att,$test,$disc,$proj,\"${item.scoreDisplay}\"\n")
        }
        sb.toString()
    }
}
