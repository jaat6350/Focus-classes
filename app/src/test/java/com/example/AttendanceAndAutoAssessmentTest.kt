package com.example

import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceEntity
import com.example.util.AttendanceDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class AttendanceAndAutoAssessmentTest {

    @Test
    fun testMondayToSaturdayPeriodCalculation() {
        // Create a Wednesday in April 2026
        val cal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.APRIL, 22, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val wednesdayMillis = cal.timeInMillis

        val mondayMillis = AttendanceDateUtils.getMondayMillis(wednesdayMillis)
        val saturdayMillis = AttendanceDateUtils.getSaturdayMillis(wednesdayMillis)

        val monCal = Calendar.getInstance(Locale.US).apply { timeInMillis = mondayMillis }
        val satCal = Calendar.getInstance(Locale.US).apply { timeInMillis = saturdayMillis }

        assertEquals(Calendar.MONDAY, monCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, monCal.get(Calendar.DAY_OF_MONTH)) // Monday is April 20, 2026
        assertEquals(0, monCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, monCal.get(Calendar.MINUTE))

        assertEquals(Calendar.SATURDAY, satCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(25, satCal.get(Calendar.DAY_OF_MONTH)) // Saturday is April 25, 2026
        assertEquals(23, satCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, satCal.get(Calendar.MINUTE))

        val label = AttendanceDateUtils.getWeekLabel(wednesdayMillis)
        assertEquals("Week (Apr 20 – Apr 25)", label)
    }

    @Test
    fun testAttendanceScoreFullAttendance() {
        // 6 days present
        val records = (1..6).map { day ->
            DailyAttendanceEntity(
                studentId = 1,
                batchId = 1,
                dayOfWeek = day,
                status = AttendanceStatus.PRESENT
            )
        }
        val score = AttendanceDateUtils.calculateAttendanceScore(records)
        assertNotNull(score)
        assertEquals(6.0, score!!, 0.001)
    }

    @Test
    fun testAttendanceScoreWithAbsences() {
        // 4 days present, 2 days absent
        val records = (1..4).map { day ->
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = day, status = AttendanceStatus.PRESENT)
        } + (5..6).map { day ->
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = day, status = AttendanceStatus.ABSENT)
        }

        // Score = (4 / (4 + 2)) * 6.0 = 4.0
        val score = AttendanceDateUtils.calculateAttendanceScore(records)
        assertNotNull(score)
        assertEquals(4.0, score!!, 0.001)

        val stats = AttendanceDateUtils.calculateSummaryStats(records)
        assertEquals(4, stats.presentCount)
        assertEquals(2, stats.absentCount)
        assertEquals(0, stats.leaveCount)
        assertEquals(66.7, stats.percentage!!, 0.1)
    }

    @Test
    fun testAttendanceScoreApprovedLeaveExcludedFromDenominator() {
        // User specification:
        // "attendance marks = (Present days / (Present days + Absent days)) * 6.0 (approved Leave is excluded from the denominator)"
        // Student attends 4 days, is on approved leave for 2 days.
        val records = (1..4).map { day ->
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = day, status = AttendanceStatus.PRESENT)
        } + (5..6).map { day ->
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = day, status = AttendanceStatus.LEAVE)
        }

        // Present = 4, Absent = 0, Leave = 2
        // Denominator = 4 + 0 = 4
        // Score = (4 / 4) * 6.0 = 6.0 (No penalty for approved leave!)
        val score = AttendanceDateUtils.calculateAttendanceScore(records)
        assertNotNull(score)
        assertEquals(6.0, score!!, 0.001)

        val stats = AttendanceDateUtils.calculateSummaryStats(records)
        assertEquals(4, stats.presentCount)
        assertEquals(2, stats.leaveCount)
        assertEquals(0, stats.absentCount)
        assertEquals(100.0, stats.percentage!!, 0.001)
    }

    @Test
    fun testAttendanceScoreMixedWithLeaveAndAbsent() {
        // 3 days present, 1 late (0.5), 1 absent, 1 leave
        val records = listOf(
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = 1, status = AttendanceStatus.PRESENT),
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = 2, status = AttendanceStatus.PRESENT),
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = 3, status = AttendanceStatus.PRESENT),
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = 4, status = AttendanceStatus.LATE),
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = 5, status = AttendanceStatus.ABSENT),
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = 6, status = AttendanceStatus.LEAVE)
        )

        // Effective present: 3 + (0.5 * 1) = 3.5
        // Denominator: 3 + 1 (late) + 1 (absent) = 5. (Leave is excluded!)
        // Score = (3.5 / 5) * 6.0 = 4.2
        val score = AttendanceDateUtils.calculateAttendanceScore(records)
        assertNotNull(score)
        assertEquals(4.2, score!!, 0.001)
    }

    @Test
    fun testAttendanceScoreEmptyRecords() {
        val score = AttendanceDateUtils.calculateAttendanceScore(emptyList())
        assertNull(score)
    }

    @Test
    fun testAttendanceScoreAllLeaveDays() {
        // Student was sick all week on approved leave: 4 days leave, 0 absent, 0 present
        val records = (1..4).map { day ->
            DailyAttendanceEntity(studentId = 1, batchId = 1, dayOfWeek = day, status = AttendanceStatus.LEAVE)
        }
        val score = AttendanceDateUtils.calculateAttendanceScore(records)
        assertNotNull(score)
        assertEquals(6.0, score!!, 0.001) // Student not penalized
    }
}
