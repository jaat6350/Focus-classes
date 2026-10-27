package com.example.ui.export

import com.example.data.model.RankedStudentItem
import java.util.Locale

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    PNG("png", "image/png", "PNG Image (High-Res)"),
    PDF("pdf", "application/pdf", "PDF Document (Print-Ready)"),
    JPEG("jpg", "image/jpeg", "JPEG Image")
}

data class LeaderboardExportStats(
    val totalStudents: Int,
    val completedStudents: Int,
    val highestScoreDisplay: String,
    val highestStudentName: String?,
    val classAverageDisplay: String,
    val topThreeAverageDisplay: String,
    val topThreeCount: Int
)

object ExportStatsCalculator {
    fun calculate(
        rankedItems: List<RankedStudentItem>,
        totalEnrolled: Int = rankedItems.size
    ): LeaderboardExportStats {
        val totalStudents = if (totalEnrolled > 0) totalEnrolled else rankedItems.size
        val completedStudents = rankedItems.count { it.isComplete }

        // Eligible students are those with valid scores (scoreValue >= 0)
        val eligibleStudents = rankedItems.filter { it.scoreValue >= 0.0 }

        if (eligibleStudents.isEmpty()) {
            return LeaderboardExportStats(
                totalStudents = totalStudents,
                completedStudents = completedStudents,
                highestScoreDisplay = "-",
                highestStudentName = null,
                classAverageDisplay = "-",
                topThreeAverageDisplay = "-",
                topThreeCount = 0
            )
        }

        // Highest score is the first eligible student (list is sorted descending)
        val topStudent = eligibleStudents.first()
        val highestScoreDisplay = String.format(Locale.US, "%.1f%%", topStudent.scoreValue)
        val highestStudentName = topStudent.student.fullName

        // Class average
        val classAvg = eligibleStudents.map { it.scoreValue }.average()
        val classAverageDisplay = String.format(Locale.US, "%.1f%%", classAvg)

        // Top 3 average (or up to 3 if fewer are available)
        val top3 = eligibleStudents.take(3)
        val top3Avg = top3.map { it.scoreValue }.average()
        val topThreeAverageDisplay = String.format(Locale.US, "%.1f%%", top3Avg)

        return LeaderboardExportStats(
            totalStudents = totalStudents,
            completedStudents = completedStudents,
            highestScoreDisplay = highestScoreDisplay,
            highestStudentName = highestStudentName,
            classAverageDisplay = classAverageDisplay,
            topThreeAverageDisplay = topThreeAverageDisplay,
            topThreeCount = top3.size
        )
    }
}
