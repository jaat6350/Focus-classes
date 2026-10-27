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
import com.example.util.AttendanceDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

    // Automatic Weekly Assessment Generation
    suspend fun ensureAssessmentForDate(batchId: Long, dateMillis: Long): WeeklyAssessmentEntity = withContext(Dispatchers.IO) {
        val weekLabel = AttendanceDateUtils.getWeekLabel(dateMillis)
        val mondayMillis = AttendanceDateUtils.getMondayMillis(dateMillis)
        val saturdayMillis = AttendanceDateUtils.getSaturdayMillis(dateMillis)

        val existing = assessmentDao.findAssessmentCoveringDate(batchId, dateMillis)
            ?: assessmentDao.findAssessmentByLabel(batchId, weekLabel)
        if (existing != null) {
            return@withContext existing
        }

        val newAssessment = WeeklyAssessmentEntity(
            batchId = batchId,
            weekLabel = weekLabel,
            startDate = mondayMillis,
            endDate = saturdayMillis,
            testMaxMarks = 20.0,
            createdAt = System.currentTimeMillis()
        )
        val newId = assessmentDao.insertAssessment(newAssessment)
        newAssessment.copy(id = newId)
    }

    suspend fun ensureCurrentWeekAssessment(batchId: Long): WeeklyAssessmentEntity {
        return ensureAssessmentForDate(batchId, System.currentTimeMillis())
    }

    suspend fun ensureWeeklyAssessmentsForAllBatches(dateMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val batches = batchDao.getAllBatchesDirect()
        batches.forEach { batch ->
            ensureAssessmentForDate(batch.id, dateMillis)
        }
    }

    // Daily Attendance Tracking by Calendar Date
    fun getAttendanceForBatchAndDate(batchId: Long, dateString: String): Flow<List<DailyAttendanceEntity>> =
        attendanceDao.getAttendanceForBatchAndDate(batchId, dateString).flowOn(Dispatchers.IO)

    suspend fun getAttendanceForBatchAndDateDirect(batchId: Long, dateString: String): List<DailyAttendanceEntity> =
        withContext(Dispatchers.IO) {
            attendanceDao.getAttendanceForBatchAndDateDirect(batchId, dateString)
        }

    fun getStudentAttendanceHistory(studentId: Long): Flow<List<DailyAttendanceEntity>> =
        attendanceDao.getAttendanceHistoryForStudent(studentId).flowOn(Dispatchers.IO)

    suspend fun getStudentAttendanceHistoryDirect(studentId: Long): List<DailyAttendanceEntity> =
        withContext(Dispatchers.IO) {
            attendanceDao.getAttendanceHistoryForStudentDirect(studentId)
        }

    suspend fun saveDailyAttendance(
        records: List<DailyAttendanceEntity>,
        batchId: Long,
        dateMillis: Long
    ) = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext

        // Ensure weekly assessment exists covering this date
        val assessment = ensureAssessmentForDate(batchId, dateMillis)
        val dateString = AttendanceDateUtils.toDateString(dateMillis)
        val dayOfWeek = AttendanceDateUtils.getDayOfWeek(dateMillis)

        val preparedRecords = records.map {
            it.copy(
                batchId = batchId,
                assessmentId = assessment.id,
                dateString = if (it.dateString.isBlank()) dateString else it.dateString,
                dateMillis = if (it.dateMillis == 0L) dateMillis else it.dateMillis,
                dayOfWeek = dayOfWeek
            )
        }

        attendanceDao.upsertAttendanceBatch(preparedRecords)

        // Recalculate attendance marks for all affected students for this weekly assessment
        val studentIds = records.map { it.studentId }.distinct()
        val mon = AttendanceDateUtils.getMondayMillis(dateMillis)
        val sat = AttendanceDateUtils.getSaturdayMillis(dateMillis)

        for (sId in studentIds) {
            val weekRecords = attendanceDao.getStudentAttendanceForAssessmentDirect(sId, assessment.id)
                .ifEmpty { attendanceDao.getStudentAttendanceBetween(sId, mon, sat) }
            val score = AttendanceDateUtils.calculateAttendanceScore(weekRecords)

            val existingResult = resultDao.getResult(sId, assessment.id)
            val updatedResult = if (existingResult != null) {
                existingResult.copy(
                    attendanceMarks = score,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                StudentWeeklyResultEntity(
                    studentId = sId,
                    assessmentId = assessment.id,
                    attendanceMarks = score
                )
            }
            resultDao.upsertResult(updatedResult)
        }
    }

    // Daily Attendance Tracking (Day of week support)
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
        val assessment = assessmentDao.getAssessmentById(assessmentId)
        val dateMillis = assessment?.startDate ?: System.currentTimeMillis()
        saveDailyAttendance(records, batchId, dateMillis)
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

    // Save profile image to local app storage with EXIF orientation normalization
    suspend fun copyImageToLocalStorage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "student_photos")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val filename = "student_${UUID.randomUUID()}.jpg"
        val destinationFile = File(imagesDir, filename)

        try {
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Check if photo contains EXIF orientation from phone camera
            val exif = ExifInterface(destinationFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.postRotate(90f)
                        matrix.postScale(-1f, 1f)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.postRotate(270f)
                        matrix.postScale(-1f, 1f)
                    }
                }
                val originalBitmap = BitmapFactory.decodeFile(destinationFile.absolutePath)
                if (originalBitmap != null) {
                    val rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                    )
                    FileOutputStream(destinationFile).use { out ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    if (rotatedBitmap != originalBitmap) {
                        rotatedBitmap.recycle()
                    }
                    originalBitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
