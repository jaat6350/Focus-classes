package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "batches",
    indices = [Index(value = ["name"])]
)
data class BatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val grade: Int, // 1 to 12
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["rollNumber"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val batchId: Long,
    val rollNumber: String = "",
    val studentCode: String = "",
    val profileImagePath: String? = null,
    val enrollmentDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_assessments",
    indices = [
        Index(value = ["batchId", "weekLabel"])
    ]
)
data class WeeklyAssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,
    val weekLabel: String, // e.g. "Week 1 (Apr 21 – Apr 27)"
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L,
    val testMaxMarks: Double = 20.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "student_weekly_results",
    indices = [
        Index(value = ["studentId", "assessmentId"], unique = true),
        Index(value = ["assessmentId"])
    ]
)
data class StudentWeeklyResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val assessmentId: Long,
    val attendanceMarks: Double? = null, // 0..6
    val testMarks: Double? = null,       // 0..testMaxMarks
    val disciplineMarks: Double? = null, // 0..4
    val projectMarks: Double? = null,    // 0..10
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scoring_configs")
data class ScoringConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val attendanceWeight: Float = 25f,
    val testWeight: Float = 25f,
    val disciplineWeight: Float = 25f,
    val projectWeight: Float = 25f
)

@Entity(
    tableName = "assessment_categories",
    indices = [Index(value = ["name"])]
)
data class AssessmentCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val maxMarks: Double,
    val weight: Float = 20f,
    val isSystemDefault: Boolean = false,
    val systemKey: String? = null, // "ATTENDANCE", "TEST", "DISCIPLINE", "PROJECT", or null
    val displayOrder: Int = 0,
    val colorHex: String = "#0A4DA2"
)

@Entity(
    tableName = "student_category_marks",
    indices = [
        Index(value = ["studentId", "assessmentId", "categoryId"], unique = true),
        Index(value = ["assessmentId", "categoryId"])
    ]
)
data class StudentCategoryMarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val assessmentId: Long,
    val categoryId: Long,
    val marksObtained: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus(val label: String, val score: Double) {
    PRESENT("Present", 1.0),
    LATE("Late", 0.5),
    ABSENT("Absent", 0.0)
}

@Entity(
    tableName = "daily_attendance",
    indices = [
        Index(value = ["studentId", "assessmentId", "dayOfWeek"], unique = true),
        Index(value = ["batchId", "assessmentId", "dayOfWeek"])
    ]
)
data class DailyAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val batchId: Long,
    val assessmentId: Long,
    val dayOfWeek: Int, // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val score: Double = 1.0, // 1 mark per day
    val dateMillis: Long = System.currentTimeMillis()
)
