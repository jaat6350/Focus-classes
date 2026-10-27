package com.example.util

import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AttendanceSummaryStats(
    val totalRecords: Int,
    val presentCount: Int,
    val lateCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val percentage: Double?,
    val weeklyScore: Double?
)

object AttendanceDateUtils {
    private val dateIsoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private val monthDayFormatter = SimpleDateFormat("MMM d", Locale.US)

    @Synchronized
    fun toDateString(millis: Long): String {
        return dateIsoFormatter.format(Date(millis))
    }

    @Synchronized
    fun fromDateString(dateStr: String): Long {
        return try {
            dateIsoFormatter.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    @Synchronized
    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = dateIsoFormatter.parse(dateStr)
            if (date != null) displayDateFormatter.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    @Synchronized
    fun formatDisplayDate(millis: Long): String {
        return displayDateFormatter.format(Date(millis))
    }

    /**
     * Calculates the Monday of the week (at 00:00:00.000) for the given timestamp.
     * Weeks start on Monday.
     */
    fun getMondayMillis(refMillis: Long): Long {
        val cal = Calendar.getInstance(Locale.US).apply {
            timeInMillis = refMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val day = get(Calendar.DAY_OF_WEEK)
            val diff = if (day == Calendar.SUNDAY) 6 else day - Calendar.MONDAY
            add(Calendar.DAY_OF_MONTH, -diff)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates the Saturday of the week (at 23:59:59.999) for the given timestamp.
     * Assessment periods run Monday through Saturday.
     */
    fun getSaturdayMillis(refMillis: Long): Long {
        val mon = getMondayMillis(refMillis)
        val cal = Calendar.getInstance(Locale.US).apply {
            timeInMillis = mon
            add(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Day of week (1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun).
     */
    fun getDayOfWeek(refMillis: Long): Int {
        val cal = Calendar.getInstance(Locale.US).apply {
            timeInMillis = refMillis
        }
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - Calendar.MONDAY + 1
    }

    /**
     * Day name: "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
     */
    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "Day $dayOfWeek"
        }
    }

    /**
     * Generates standard weekly assessment label covering Monday through Saturday.
     * Format: e.g. "Week (Apr 20 – Apr 25)"
     */
    @Synchronized
    fun getWeekLabel(refMillis: Long): String {
        val mon = getMondayMillis(refMillis)
        val sat = getSaturdayMillis(refMillis)
        val monStr = monthDayFormatter.format(Date(mon))
        val satStr = monthDayFormatter.format(Date(sat))
        return "Week ($monStr – $satStr)"
    }

    /**
     * Shifts a date by a given number of days.
     */
    fun addDays(refMillis: Long, days: Int): Long {
        val cal = Calendar.getInstance(Locale.US).apply {
            timeInMillis = refMillis
            add(Calendar.DAY_OF_MONTH, days)
        }
        return cal.timeInMillis
    }

    /**
     * Calculate weekly attendance marks (out of 6.0) based on formula:
     * Marks = (Present days / (Present days + Absent days)) * 6.0
     * Approved Leave is excluded from the denominator.
     * Late is counted as 0.5 present.
     */
    fun calculateAttendanceScore(records: List<DailyAttendanceEntity>): Double? {
        if (records.isEmpty()) return null

        val present = records.count { it.status == AttendanceStatus.PRESENT }
        val late = records.count { it.status == AttendanceStatus.LATE }
        val absent = records.count { it.status == AttendanceStatus.ABSENT }
        val leave = records.count { it.status == AttendanceStatus.LEAVE }

        val effectivePresent = present + (late * 0.5)
        val denominator = present + late + absent

        if (denominator == 0) {
            // If student only has approved leave records and 0 absent days,
            // they are not penalized and receive full marks (6.0).
            return if (leave > 0) 6.0 else null
        }

        val rawScore = (effectivePresent / denominator.toDouble()) * 6.0
        val rounded = kotlin.math.round(rawScore * 10.0) / 10.0
        return rounded.coerceIn(0.0, 6.0)
    }

    /**
     * Comprehensive summary statistics for student profile and batch attendance.
     */
    fun calculateSummaryStats(records: List<DailyAttendanceEntity>): AttendanceSummaryStats {
        val present = records.count { it.status == AttendanceStatus.PRESENT }
        val late = records.count { it.status == AttendanceStatus.LATE }
        val absent = records.count { it.status == AttendanceStatus.ABSENT }
        val leave = records.count { it.status == AttendanceStatus.LEAVE }
        val total = records.size

        val effectivePresent = present + (late * 0.5)
        val denominator = present + late + absent

        val pct = if (denominator > 0) {
            val raw = (effectivePresent / denominator.toDouble()) * 100.0
            kotlin.math.round(raw * 10.0) / 10.0
        } else if (leave > 0) {
            100.0
        } else null

        val weeklyScore = calculateAttendanceScore(records)

        return AttendanceSummaryStats(
            totalRecords = total,
            presentCount = present,
            lateCount = late,
            absentCount = absent,
            leaveCount = leave,
            percentage = pct,
            weeklyScore = weeklyScore
        )
    }
}
