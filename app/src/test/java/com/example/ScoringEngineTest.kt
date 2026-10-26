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
            attendanceMarks = 6.0,   // 6/6
            testMarks = 18.0,        // 18/20
            disciplineMarks = 4.0,   // 4/4
            projectMarks = 8.0       // 8/10
        )
        // Custom category marks for Oral Viva (id=5): 9/10
        val customMarks = mapOf(5L to 9.0)

        val item = StudentWithResult(student, result, assessment, customMarks)

        assertTrue(item.isAllCategoriesEvaluated(categories))
        val overallPct = item.calculateDynamicOverallPct(categories)

        // Proportional overall:
        // Total obtained: 6 + 18 + 4 + 8 + 9 = 45.0
        // Total max: 6 + 20 + 4 + 10 + 10 = 50.0
        // Percentage: (45.0 / 50.0) * 100 = 90.0%
        assertEquals(90.0, overallPct!!, 0.001)
    }

    @Test
    fun testProportionalScoringPreventsArtificiallyHighPercentage() {
        // User scenario:
        // Test max marks = 80, Attendance max = 6, Discipline max = 4, Project max = 10.
        // Total max = 100 marks.
        val test100Assessment = WeeklyAssessmentEntity(
            id = 2,
            batchId = 6,
            weekLabel = "Week 2",
            testMaxMarks = 80.0
        )
        val test100Categories = listOf(
            AssessmentCategoryEntity(id = 1, name = "Attendance", maxMarks = 6.0, isSystemDefault = true, systemKey = "ATTENDANCE"),
            AssessmentCategoryEntity(id = 2, name = "Test Marks", maxMarks = 80.0, isSystemDefault = true, systemKey = "TEST"),
            AssessmentCategoryEntity(id = 3, name = "Discipline", maxMarks = 4.0, isSystemDefault = true, systemKey = "DISCIPLINE"),
            AssessmentCategoryEntity(id = 4, name = "Project", maxMarks = 10.0, isSystemDefault = true, systemKey = "PROJECT")
        )

        // Case 1: Student gets 36 marks out of 100 (6 attendance, 20 test, 4 discipline, 6 project)
        val s1 = StudentEntity(id = 10, fullName = "Student 36", batchId = 6)
        val r1 = StudentWeeklyResultEntity(
            studentId = 10,
            assessmentId = 2,
            attendanceMarks = 6.0,
            testMarks = 20.0,
            disciplineMarks = 4.0,
            projectMarks = 6.0
        )
        val item1 = StudentWithResult(s1, r1, test100Assessment)
        val pct1 = item1.calculateDynamicOverallPct(test100Categories)
        // Total obtained: 6 + 20 + 4 + 6 = 36. Total max: 6 + 80 + 4 + 10 = 100. Pct = 36.0%
        assertEquals(36.0, pct1!!, 0.001)

        // Case 2: Student gets full marks only in Discipline (4/4) and Attendance (6/6), but 0 in test and project
        val s2 = StudentEntity(id = 11, fullName = "Student Partial", batchId = 6)
        val r2 = StudentWeeklyResultEntity(
            studentId = 11,
            assessmentId = 2,
            attendanceMarks = 6.0,
            testMarks = 0.0,
            disciplineMarks = 4.0,
            projectMarks = 0.0
        )
        val item2 = StudentWithResult(s2, r2, test100Assessment)
        val pct2 = item2.calculateDynamicOverallPct(test100Categories)
        // Total obtained: 6 + 0 + 4 + 0 = 10. Total max: 100. Pct = 10.0%, NOT 50%!
        assertEquals(10.0, pct2!!, 0.001)
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
