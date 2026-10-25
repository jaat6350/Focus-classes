package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.BatchEntity
import com.example.data.model.DailyAttendanceEntity
import com.example.data.model.ScoringConfigEntity
import com.example.data.model.StudentCategoryMarkEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.WeeklyAssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE isArchived = 0 ORDER BY grade ASC, name ASC")
    fun getAllBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE id = :batchId LIMIT 1")
    suspend fun getBatchById(batchId: Long): BatchEntity?

    @Query("SELECT * FROM batches WHERE id = :batchId LIMIT 1")
    fun getBatchFlow(batchId: Long): Flow<BatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatches(batches: List<BatchEntity>): List<Long>

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)

    @Query("SELECT COUNT(*) FROM batches WHERE isArchived = 0")
    fun getBatchCount(): Flow<Int>
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE batchId = :batchId AND isArchived = 0 ORDER BY rollNumber ASC, fullName ASC")
    fun getStudentsByBatch(batchId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    fun getStudentFlow(studentId: Long): Flow<StudentEntity?>

    @Query("SELECT COUNT(*) FROM students WHERE batchId = :batchId AND isArchived = 0")
    fun getStudentCountByBatch(batchId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE isArchived = 0")
    fun getTotalStudentCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id IN (:studentIds)")
    suspend fun deleteStudentsByIds(studentIds: List<Long>)
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM weekly_assessments WHERE batchId = :batchId ORDER BY startDate DESC")
    fun getAssessmentsByBatch(batchId: Long): Flow<List<WeeklyAssessmentEntity>>

    @Query("SELECT * FROM weekly_assessments ORDER BY startDate DESC")
    fun getAllAssessments(): Flow<List<WeeklyAssessmentEntity>>

    @Query("SELECT * FROM weekly_assessments WHERE id = :assessmentId LIMIT 1")
    suspend fun getAssessmentById(assessmentId: Long): WeeklyAssessmentEntity?

    @Query("SELECT * FROM weekly_assessments WHERE id = :assessmentId LIMIT 1")
    fun getAssessmentFlow(assessmentId: Long): Flow<WeeklyAssessmentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: WeeklyAssessmentEntity): Long

    @Update
    suspend fun updateAssessment(assessment: WeeklyAssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: WeeklyAssessmentEntity)
}

@Dao
interface ResultDao {
    @Query("SELECT * FROM student_weekly_results WHERE assessmentId = :assessmentId")
    fun getResultsForAssessment(assessmentId: Long): Flow<List<StudentWeeklyResultEntity>>

    @Query("SELECT * FROM student_weekly_results WHERE studentId = :studentId")
    fun getResultsForStudent(studentId: Long): Flow<List<StudentWeeklyResultEntity>>

    @Query("SELECT * FROM student_weekly_results WHERE studentId = :studentId AND assessmentId = :assessmentId LIMIT 1")
    suspend fun getResult(studentId: Long, assessmentId: Long): StudentWeeklyResultEntity?

    @Query("SELECT * FROM student_weekly_results WHERE studentId = :studentId AND assessmentId = :assessmentId LIMIT 1")
    fun getResultFlow(studentId: Long, assessmentId: Long): Flow<StudentWeeklyResultEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResult(result: StudentWeeklyResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResults(results: List<StudentWeeklyResultEntity>): List<Long>

    @Delete
    suspend fun deleteResult(result: StudentWeeklyResultEntity)

    @Query("SELECT COUNT(*) FROM student_weekly_results WHERE attendanceMarks IS NOT NULL AND testMarks IS NOT NULL AND disciplineMarks IS NOT NULL AND projectMarks IS NOT NULL")
    fun getCompletedRecordsCount(): Flow<Int>
}

@Dao
interface ScoringConfigDao {
    @Query("SELECT * FROM scoring_configs WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<ScoringConfigEntity?>

    @Query("SELECT * FROM scoring_configs WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): ScoringConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: ScoringConfigEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM assessment_categories ORDER BY displayOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<AssessmentCategoryEntity>>

    @Query("SELECT * FROM assessment_categories ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllCategoriesDirect(): List<AssessmentCategoryEntity>

    @Query("SELECT * FROM assessment_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): AssessmentCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: AssessmentCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<AssessmentCategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: AssessmentCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: AssessmentCategoryEntity)

    @Query("DELETE FROM student_category_marks WHERE categoryId = :categoryId")
    suspend fun deleteMarksForCategory(categoryId: Long)
}

@Dao
interface CategoryMarkDao {
    @Query("SELECT * FROM student_category_marks WHERE assessmentId = :assessmentId")
    fun getMarksForAssessment(assessmentId: Long): Flow<List<StudentCategoryMarkEntity>>

    @Query("SELECT * FROM student_category_marks WHERE studentId = :studentId AND assessmentId = :assessmentId")
    fun getMarksForStudent(studentId: Long, assessmentId: Long): Flow<List<StudentCategoryMarkEntity>>

    @Query("SELECT * FROM student_category_marks WHERE studentId = :studentId AND assessmentId = :assessmentId")
    suspend fun getMarksForStudentDirect(studentId: Long, assessmentId: Long): List<StudentCategoryMarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMark(mark: StudentCategoryMarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMarks(marks: List<StudentCategoryMarkEntity>): List<Long>
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM daily_attendance WHERE batchId = :batchId AND assessmentId = :assessmentId AND dayOfWeek = :dayOfWeek")
    fun getAttendanceForDay(batchId: Long, assessmentId: Long, dayOfWeek: Int): Flow<List<DailyAttendanceEntity>>

    @Query("SELECT * FROM daily_attendance WHERE assessmentId = :assessmentId")
    fun getAttendanceForAssessment(assessmentId: Long): Flow<List<DailyAttendanceEntity>>

    @Query("SELECT * FROM daily_attendance WHERE studentId = :studentId AND assessmentId = :assessmentId")
    fun getStudentAttendanceForAssessment(studentId: Long, assessmentId: Long): Flow<List<DailyAttendanceEntity>>

    @Query("SELECT * FROM daily_attendance WHERE studentId = :studentId AND assessmentId = :assessmentId")
    suspend fun getStudentAttendanceForAssessmentDirect(studentId: Long, assessmentId: Long): List<DailyAttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(record: DailyAttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceBatch(records: List<DailyAttendanceEntity>): List<Long>

    @Query("SELECT SUM(score) FROM daily_attendance WHERE studentId = :studentId AND assessmentId = :assessmentId")
    suspend fun calculateWeeklyAttendanceScore(studentId: Long, assessmentId: Long): Double?
}
