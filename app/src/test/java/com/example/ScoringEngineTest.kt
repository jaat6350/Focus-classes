package com.example

import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceEntity
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.ScoringEngine
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.StudentWithResult
import com.example.data.model.WeeklyAssessmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {

    private val assessment = WeeklyAssessmentEntity(
        id = 1,
        batchId = 6,
        weekLabel = "Week 1",
        testMaxMarks = 20.0
    )

    private val categories = listOf(
        AssessmentCategoryEntity(id = 1, name = "Attendance", maxMarks = 6.0, weight = 20f, isSystemDefault = true, systemKey = "ATTENDANCE"),
        AssessmentCategoryEntity(id = 2, name = "Test Marks", maxMarks = 20.0, weight = 40f, isSystemDefault = true, systemKey = "TEST"),
        AssessmentCategoryEntity(id = 3, name = "Discipline", maxMarks = 4.0, weight = 10f, isSystemDefault = true, systemKey = "DISCIPLINE"),
        AssessmentCategoryEntity(id = 4, name = "Project", maxMarks = 10.0, weight = 10f, isSystemDefault = true, systemKey = "PROJECT"),
        AssessmentCategoryEntity(id = 5, name = "Oral Viva", maxMarks = 10.0, weight = 20f, isSystemDefault = false) // Custom Category!
    )

    @Test
    fun testCustomCategoryDynamicOverallCalculation() {
        val student = StudentEntity(id = 1, fullName = "Aarav Sharma", batchId = 6)
        val result = StudentWeeklyResultEntity(
            studentId = 1,
            assessmentId = 1,
            attendanceMarks = 6.0,   // 6/6 = 100% (weight 20%) -> 20.0
            testMarks = 18.0,         // 18/20 = 90% (weight 40%) -> 36.0
            disciplineMarks = 4.0,   // 4/4 = 100% (weight 10%) -> 10.0
            projectMarks = 8.0       // 8/10 = 80% (weight 10%) -> 8.0
        )
        // Custom category marks for Oral Viva (id=5): 9/10 = 90% (weight 20%) -> 18.0
        val customMarks = mapOf(5L to 9.0)

        val item = StudentWithResult(student, result, assessment, customMarks)

        assertTrue(item.isAllCategoriesEvaluated(categories))
        val overallPct = item.calculateDynamicOverallPct(categories)

        // Expected weighted overall:
        // (100 * 20 + 90 * 40 + 100 * 10 + 80 * 10 + 90 * 20) / 100 = (2000 + 3600 + 1000 + 800 + 1800) / 100 = 92.0%
        assertEquals(92.0, overallPct!!, 0.001)
    }

    @Test
    fun testMissingCustomCategoryIncompleteHandling() {
        val student = StudentEntity(id = 2, fullName = "Diya Patel", batchId = 6)
        val result = StudentWeeklyResultEntity(
            studentId = 2,
            assessmentId = 1,
            attendanceMarks = 6.0,
            testMarks = 18.0,
            disciplineMarks = 4.0,
            projectMarks = 8.0
        )
        // Oral viva is NOT evaluated yet!
        val customMarks = emptyMap<Long, Double?>()

        val item = StudentWithResult(student, result, assessment, customMarks)

        assertFalse(item.isAllCategoriesEvaluated(categories))
        assertNull(item.calculateDynamicOverallPct(categories))
    }

    @Test
    fun testDailyAttendanceScoringFormula() {
        // Attendance rule:
        // 6 days per week (Mon to Sat). Each day = 1.0 mark.
        // Present = 1.0 mark, Late = 0.5 mark, Absent = 0.0 mark.
        val days = listOf(
            DailyAttendanceEntity(studentId = 1, batchId = 6, assessmentId = 1, dayOfWeek = 1, status = AttendanceStatus.PRESENT, score = 1.0),
            DailyAttendanceEntity(studentId = 1, batchId = 6, assessmentId = 1, dayOfWeek = 2, status = AttendanceStatus.PRESENT, score = 1.0),
            DailyAttendanceEntity(studentId = 1, batchId = 6, assessmentId = 1, dayOfWeek = 3, status = AttendanceStatus.LATE, score = 0.5),
            DailyAttendanceEntity(studentId = 1, batchId = 6, assessmentId = 1, dayOfWeek = 4, status = AttendanceStatus.PRESENT, score = 1.0),
            DailyAttendanceEntity(studentId = 1, batchId = 6, assessmentId = 1, dayOfWeek = 5, status = AttendanceStatus.ABSENT, score = 0.0),
            DailyAttendanceEntity(studentId = 1, batchId = 6, assessmentId = 1, dayOfWeek = 6, status = AttendanceStatus.PRESENT, score = 1.0)
        )

        val totalWeeklyMarks = days.sumOf { it.score }
        assertEquals(4.5, totalWeeklyMarks, 0.001)

        // Percentage out of 6 marks = (4.5 / 6.0) * 100 = 75.0%
        val attendancePct = (totalWeeklyMarks / 6.0) * 100.0
        assertEquals(75.0, attendancePct, 0.001)
    }

    @Test
    fun testDynamicLeaderboardRankingByCustomCategory() {
        val s1 = StudentEntity(id = 1, fullName = "Student Low", batchId = 6)
        val s2 = StudentEntity(id = 2, fullName = "Student High", batchId = 6)

        val item1 = StudentWithResult(
            student = s1,
            result = null,
            assessment = assessment,
            customCategoryMarks = mapOf(5L to 4.0) // 4/10 in Oral Viva
        )

        val item2 = StudentWithResult(
            student = s2,
            result = null,
            assessment = assessment,
            customCategoryMarks = mapOf(5L to 9.5) // 9.5/10 in Oral Viva
        )

        val filter = LeaderboardFilterCategory(
            id = "CUSTOM_5",
            title = "Oral Viva",
            customCategoryId = 5L
        )

        val ranked = ScoringEngine.rankStudentsDynamic(listOf(item1, item2), categories, filter)

        assertEquals("Student High", ranked[0].student.fullName)
        assertEquals(1, ranked[0].rank)
        assertEquals("Student Low", ranked[1].student.fullName)
        assertEquals(2, ranked[1].rank)
    }
}
