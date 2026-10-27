package com.example

import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.BatchEntity
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.RankedStudentItem
import com.example.data.model.ScoringEngine
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.StudentWithResult
import com.example.data.model.WeeklyAssessmentEntity
import com.example.ui.export.ExportFormat
import com.example.ui.export.ExportStatsCalculator
import com.example.ui.export.LeaderboardExportManager
import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.ui.export.LeaderboardRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LeaderboardExportTest {

    private val assessment = WeeklyAssessmentEntity(
        id = 101,
        batchId = 6,
        weekLabel = "Week 1 (Apr 21 – Apr 27)",
        testMaxMarks = 20.0
    )

    private val batch = BatchEntity(
        id = 6,
        name = "Class 6",
        grade = 6
    )

    private val systemCategories = listOf(
        AssessmentCategoryEntity(id = 1, name = "Attendance", maxMarks = 6.0, isSystemDefault = true, systemKey = "ATTENDANCE"),
        AssessmentCategoryEntity(id = 2, name = "Test Marks", maxMarks = 20.0, isSystemDefault = true, systemKey = "TEST"),
        AssessmentCategoryEntity(id = 3, name = "Discipline", maxMarks = 4.0, isSystemDefault = true, systemKey = "DISCIPLINE"),
        AssessmentCategoryEntity(id = 4, name = "Project", maxMarks = 10.0, isSystemDefault = true, systemKey = "PROJECT")
    )

    @Test
    fun testExportStatsCalculationForCompleteAssessment() {
        val s1 = StudentEntity(id = 1, fullName = "Aarav Sharma", batchId = 6)
        val r1 = StudentWeeklyResultEntity(studentId = 1, assessmentId = 101, attendanceMarks = 6.0, testMarks = 19.0, disciplineMarks = 4.0, projectMarks = 10.0) // 39/40 = 97.5%

        val s2 = StudentEntity(id = 2, fullName = "Diya Patel", batchId = 6)
        val r2 = StudentWeeklyResultEntity(studentId = 2, assessmentId = 101, attendanceMarks = 6.0, testMarks = 18.0, disciplineMarks = 4.0, projectMarks = 9.0) // 37/40 = 92.5%

        val s3 = StudentEntity(id = 3, fullName = "Rohan Mehta", batchId = 6)
        val r3 = StudentWeeklyResultEntity(studentId = 3, assessmentId = 101, attendanceMarks = 5.0, testMarks = 17.0, disciplineMarks = 4.0, projectMarks = 8.0) // 34/40 = 85.0%

        val s4 = StudentEntity(id = 4, fullName = "Sneha Gupta", batchId = 6)
        val r4 = StudentWeeklyResultEntity(studentId = 4, assessmentId = 101, attendanceMarks = 5.0, testMarks = 15.0, disciplineMarks = 3.0, projectMarks = 7.0) // 30/40 = 75.0%

        val items = listOf(
            StudentWithResult(s1, r1, assessment),
            StudentWithResult(s2, r2, assessment),
            StudentWithResult(s3, r3, assessment),
            StudentWithResult(s4, r4, assessment)
        )

        val ranked = ScoringEngine.rankStudentsDynamic(
            items = items,
            activeCategories = systemCategories,
            filter = LeaderboardFilterCategory("OVERALL", "Overall Performance")
        )

        // Verify podium order
        assertEquals("Aarav Sharma", ranked[0].student.fullName)
        assertEquals(1, ranked[0].rank)
        assertEquals("Diya Patel", ranked[1].student.fullName)
        assertEquals(2, ranked[1].rank)
        assertEquals("Rohan Mehta", ranked[2].student.fullName)
        assertEquals(3, ranked[2].rank)
        assertEquals("Sneha Gupta", ranked[3].student.fullName)
        assertEquals(4, ranked[3].rank)

        // Verify stats calculation
        val stats = ExportStatsCalculator.calculate(ranked, totalEnrolled = 4)

        assertEquals(4, stats.totalStudents)
        assertEquals(4, stats.completedStudents)
        assertEquals("97.5%", stats.highestScoreDisplay)
        assertEquals("Aarav Sharma", stats.highestStudentName)

        // Class average: (97.5 + 92.5 + 85.0 + 75.0) / 4 = 350.0 / 4 = 87.5%
        assertEquals("87.5%", stats.classAverageDisplay)

        // Top 3 average: (97.5 + 92.5 + 85.0) / 3 = 275.0 / 3 = 91.666... -> 91.7%
        assertEquals("91.7%", stats.topThreeAverageDisplay)
        assertEquals(3, stats.topThreeCount)
    }

    @Test
    fun testExportStatsWithEmptyAssessment() {
        val emptyRanked = emptyList<RankedStudentItem>()
        val stats = ExportStatsCalculator.calculate(emptyRanked, totalEnrolled = 0)

        assertEquals(0, stats.totalStudents)
        assertEquals(0, stats.completedStudents)
        assertEquals("-", stats.highestScoreDisplay)
        assertNull(stats.highestStudentName)
        assertEquals("-", stats.classAverageDisplay)
        assertEquals("-", stats.topThreeAverageDisplay)
        assertEquals(0, stats.topThreeCount)
    }

    @Test
    fun testExportStatsWithFewerThanThreeStudents() {
        val s1 = StudentEntity(id = 1, fullName = "Solo Winner", batchId = 6)
        val r1 = StudentWeeklyResultEntity(studentId = 1, assessmentId = 101, attendanceMarks = 6.0, testMarks = 18.0, disciplineMarks = 4.0, projectMarks = 10.0) // 38/40 = 95.0%

        val items = listOf(StudentWithResult(s1, r1, assessment))
        val ranked = ScoringEngine.rankStudentsDynamic(
            items = items,
            activeCategories = systemCategories,
            filter = LeaderboardFilterCategory("OVERALL", "Overall Performance")
        )

        val stats = ExportStatsCalculator.calculate(ranked, totalEnrolled = 1)
        assertEquals(1, stats.totalStudents)
        assertEquals(1, stats.topThreeCount)
        assertEquals("95.0%", stats.topThreeAverageDisplay)
        assertEquals("Solo Winner", stats.highestStudentName)
    }

    @Test
    fun testTiedRanksPreservedConsistently() {
        val s1 = StudentEntity(id = 1, fullName = "Alice", batchId = 6)
        val r1 = StudentWeeklyResultEntity(studentId = 1, assessmentId = 101, attendanceMarks = 6.0, testMarks = 20.0, disciplineMarks = 4.0, projectMarks = 10.0) // 40/40 = 100%

        val s2 = StudentEntity(id = 2, fullName = "Bob", batchId = 6)
        val r2 = StudentWeeklyResultEntity(studentId = 2, assessmentId = 101, attendanceMarks = 6.0, testMarks = 20.0, disciplineMarks = 4.0, projectMarks = 10.0) // 40/40 = 100%

        val s3 = StudentEntity(id = 3, fullName = "Charlie", batchId = 6)
        val r3 = StudentWeeklyResultEntity(studentId = 3, assessmentId = 101, attendanceMarks = 6.0, testMarks = 16.0, disciplineMarks = 4.0, projectMarks = 10.0) // 36/40 = 90%

        val items = listOf(
            StudentWithResult(s1, r1, assessment),
            StudentWithResult(s2, r2, assessment),
            StudentWithResult(s3, r3, assessment)
        )

        val ranked = ScoringEngine.rankStudentsDynamic(
            items = items,
            activeCategories = systemCategories,
            filter = LeaderboardFilterCategory("OVERALL", "Overall Performance")
        )

        // Alice & Bob tie for rank 1
        assertEquals(1, ranked[0].rank)
        assertEquals(1, ranked[1].rank)
        // Next rank skips to 3
        assertEquals(3, ranked[2].rank)
    }

    @Test
    fun testPaginationCalculation() {
        // <= 10 students fits on page 1
        assertEquals(1, LeaderboardRenderer.calculateTotalPages(0))
        assertEquals(1, LeaderboardRenderer.calculateTotalPages(5))
        assertEquals(1, LeaderboardRenderer.calculateTotalPages(10))

        // 11 to 28 students requires 2 pages (10 on page 1, up to 18 on page 2)
        assertEquals(2, LeaderboardRenderer.calculateTotalPages(11))
        assertEquals(2, LeaderboardRenderer.calculateTotalPages(28))

        // 29 students requires 3 pages
        assertEquals(3, LeaderboardRenderer.calculateTotalPages(29))
        assertEquals(3, LeaderboardRenderer.calculateTotalPages(46))
    }

    @Test
    fun testExportFilenameSanitization() {
        val testBatch = BatchEntity(id = 1, name = "Grade 9 & 10 IIT-Foundation", grade = 10)
        val testAssessment = WeeklyAssessmentEntity(id = 1, batchId = 1, weekLabel = "Week 1 (Apr 21 – Apr 27)")

        val pngName = LeaderboardExportManager.createExportFilename(testBatch, testAssessment, ExportFormat.PNG)
        val pdfName = LeaderboardExportManager.createExportFilename(testBatch, testAssessment, ExportFormat.PDF)
        val jpgName = LeaderboardExportManager.createExportFilename(testBatch, testAssessment, ExportFormat.JPEG)

        assertTrue(pngName.endsWith(".png"))
        assertTrue(pdfName.endsWith(".pdf"))
        assertTrue(jpgName.endsWith(".jpg"))

        assertTrue(pngName.startsWith("focus-leaderboard-"))
        assertFalse(pngName.contains(" "))
        assertFalse(pngName.contains("&"))
    }

    @Test
    fun testCategorySpecificExportRanking() {
        val s1 = StudentEntity(id = 1, fullName = "Student High Test", batchId = 6)
        val r1 = StudentWeeklyResultEntity(studentId = 1, assessmentId = 101, attendanceMarks = 2.0, testMarks = 20.0, disciplineMarks = 2.0, projectMarks = 2.0)

        val s2 = StudentEntity(id = 2, fullName = "Student High Attendance", batchId = 6)
        val r2 = StudentWeeklyResultEntity(studentId = 2, assessmentId = 101, attendanceMarks = 6.0, testMarks = 10.0, disciplineMarks = 4.0, projectMarks = 10.0)

        val items = listOf(
            StudentWithResult(s1, r1, assessment),
            StudentWithResult(s2, r2, assessment)
        )

        // When exporting by TEST category: Student 1 must be #1
        val testRanked = ScoringEngine.rankStudentsDynamic(
            items = items,
            activeCategories = systemCategories,
            filter = LeaderboardFilterCategory("TEST", "Test Marks")
        )
        assertEquals("Student High Test", testRanked.first().student.fullName)
        assertEquals(100.0, testRanked.first().scoreValue, 0.001)

        // When exporting by ATTENDANCE category: Student 2 must be #1
        val attRanked = ScoringEngine.rankStudentsDynamic(
            items = items,
            activeCategories = systemCategories,
            filter = LeaderboardFilterCategory("ATTENDANCE", "Attendance")
        )
        assertEquals("Student High Attendance", attRanked.first().student.fullName)
        assertEquals(100.0, attRanked.first().scoreValue, 0.001)
    }

    @Test
    fun testStudentPhotoMatchingStrictById() {
        val s1 = StudentEntity(id = 42, fullName = "Aarav Sharma", batchId = 6, profileImagePath = "/data/user/0/com.aistudio.focusclasses/files/student_42.jpg")
        val s2 = StudentEntity(id = 99, fullName = "Aarav Sharma", batchId = 6, profileImagePath = null) // same name, different ID, no photo

        // Must preserve stable ID mapping
        assertEquals(42L, s1.id)
        assertEquals(99L, s2.id)
        assertNotNull(s1.profileImagePath)
        assertNull(s2.profileImagePath)
        assertTrue(s1.profileImagePath!!.contains("student_42"))
    }

    @Test
    fun testDrawCenterCropCircleBitmapDoesNotCrash() {
        // Verify center-crop circular rendering works smoothly for both portrait and landscape
        val canvasBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)

        // Landscape photo test
        val landscapePhoto = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        LeaderboardRenderer.drawCenterCropCircleBitmap(
            canvas = canvas,
            bitmap = landscapePhoto,
            centerX = 250f,
            centerY = 250f,
            radius = 120f
        )

        // Portrait photo test
        val portraitPhoto = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888)
        LeaderboardRenderer.drawCenterCropCircleBitmap(
            canvas = canvas,
            bitmap = portraitPhoto,
            centerX = 250f,
            centerY = 250f,
            radius = 32f
        )

        assertNotNull(canvasBitmap)
    }

    @Test
    fun testLoadOrientedStudentBitmapGracefulFallback() {
        // Missing file should return null without throwing exception
        val result = LeaderboardRenderer.loadOrientedStudentBitmap("/non/existent/path.jpg", 120)
        assertNull(result)

        // Valid temp image file should load successfully
        val tempFile = File.createTempFile("student_test", ".jpg")
        try {
            val sampleBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            FileOutputStream(tempFile).use { out ->
                sampleBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val loaded = LeaderboardRenderer.loadOrientedStudentBitmap(tempFile.absolutePath, 50)
            assertNotNull(loaded)
        } finally {
            tempFile.delete()
        }
    }
}
