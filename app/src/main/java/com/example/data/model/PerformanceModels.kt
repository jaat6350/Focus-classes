package com.example.data.model

import java.util.Locale

data class DynamicCategoryItem(
    val id: Long,
    val name: String,
    val maxMarks: Double,
    val weight: Float,
    val isSystemDefault: Boolean,
    val systemKey: String?,
    val colorHex: String
)

data class StudentWithResult(
    val student: StudentEntity,
    val result: StudentWeeklyResultEntity?,
    val assessment: WeeklyAssessmentEntity?,
    val customCategoryMarks: Map<Long, Double?> = emptyMap() // categoryId -> marksObtained
) {
    val attendancePct: Double?
        get() = result?.attendanceMarks?.let { (it / 6.0) * 100.0 }

    val testPct: Double?
        get() = result?.testMarks?.let { marks ->
            val max = assessment?.testMaxMarks ?: 20.0
            if (max > 0) (marks / max) * 100.0 else 0.0
        }

    val disciplinePct: Double?
        get() = result?.disciplineMarks?.let { (it / 4.0) * 100.0 }

    val projectPct: Double?
        get() = result?.projectMarks?.let { (it / 10.0) * 100.0 }

    fun getCategoryMarks(category: AssessmentCategoryEntity): Double? {
        return when (category.systemKey) {
            "ATTENDANCE" -> result?.attendanceMarks
            "TEST" -> result?.testMarks
            "DISCIPLINE" -> result?.disciplineMarks
            "PROJECT" -> result?.projectMarks
            else -> customCategoryMarks[category.id]
        }
    }

    fun getCategoryMaxMarks(category: AssessmentCategoryEntity): Double {
        return if (category.systemKey == "TEST") {
            assessment?.testMaxMarks ?: category.maxMarks
        } else {
            category.maxMarks
        }
    }

    fun getCategoryPercentage(category: AssessmentCategoryEntity): Double? {
        val marks = getCategoryMarks(category) ?: return null
        val max = getCategoryMaxMarks(category)
        return if (max > 0) (marks / max) * 100.0 else 0.0
    }

    fun getTotalObtainedMarks(categories: List<AssessmentCategoryEntity>): Double {
        if (categories.isEmpty()) return rawTotalMarks
        return categories.sumOf { cat -> getCategoryMarks(cat) ?: 0.0 }
    }

    fun getTotalMaxMarks(categories: List<AssessmentCategoryEntity>): Double {
        if (categories.isEmpty()) {
            val testMax = assessment?.testMaxMarks ?: 20.0
            return 6.0 + testMax + 4.0 + 10.0
        }
        return categories.sumOf { cat -> getCategoryMaxMarks(cat) }
    }

    fun isAllCategoriesEvaluated(categories: List<AssessmentCategoryEntity>): Boolean {
        if (categories.isEmpty()) {
            return result?.attendanceMarks != null &&
                    result?.testMarks != null &&
                    result?.disciplineMarks != null &&
                    result?.projectMarks != null
        }
        return categories.all { cat -> getCategoryMarks(cat) != null }
    }

    fun hasAnyCategoryEvaluated(categories: List<AssessmentCategoryEntity>): Boolean {
        if (categories.isEmpty()) {
            return result?.attendanceMarks != null ||
                    result?.testMarks != null ||
                    result?.disciplineMarks != null ||
                    result?.projectMarks != null
        }
        return categories.any { cat -> getCategoryMarks(cat) != null }
    }

    /**
     * Calculates the overall percentage dynamically:
     * Percentage = (Sum of marks obtained across all categories) / (Sum of max marks of all categories) * 100.
     * Ensures categories with smaller max marks (e.g. Discipline out of 4, Attendance out of 6)
     * are proportional to the total (e.g. out of 100), rather than having an equal 25% weight.
     */
    fun calculateDynamicOverallPct(categories: List<AssessmentCategoryEntity>): Double? {
        if (categories.isEmpty()) return null
        if (!isAllCategoriesEvaluated(categories)) return null

        val totalMax = getTotalMaxMarks(categories)
        if (totalMax <= 0.0) return 0.0

        val totalObtained = getTotalObtainedMarks(categories)
        return (totalObtained / totalMax) * 100.0
    }

    fun calculateProvisionalOverallPct(categories: List<AssessmentCategoryEntity>): Double? {
        if (categories.isEmpty()) return null
        if (!hasAnyCategoryEvaluated(categories)) return null

        val totalMax = getTotalMaxMarks(categories)
        if (totalMax <= 0.0) return 0.0

        val totalObtained = getTotalObtainedMarks(categories)
        return (totalObtained / totalMax) * 100.0
    }

    val isComplete: Boolean
        get() = result?.attendanceMarks != null &&
                result?.testMarks != null &&
                result?.disciplineMarks != null &&
                result?.projectMarks != null

    val rawTotalMarks: Double
        get() = (result?.attendanceMarks ?: 0.0) +
                (result?.testMarks ?: 0.0) +
                (result?.disciplineMarks ?: 0.0) +
                (result?.projectMarks ?: 0.0) +
                customCategoryMarks.values.filterNotNull().sum()
}

data class RankedStudentItem(
    val rank: Int,
    val student: StudentEntity,
    val result: StudentWeeklyResultEntity?,
    val assessment: WeeklyAssessmentEntity?,
    val scoreValue: Double,
    val scoreDisplay: String,
    val isComplete: Boolean,
    val rawAttendance: Double? = result?.attendanceMarks,
    val rawTest: Double? = result?.testMarks,
    val rawDiscipline: Double? = result?.disciplineMarks,
    val rawProject: Double? = result?.projectMarks,
    val testMaxMarks: Double = assessment?.testMaxMarks ?: 20.0,
    val customMarks: Map<Long, Double?> = emptyMap()
)

data class LeaderboardFilterCategory(
    val id: String, // "OVERALL", "ATTENDANCE", "TEST", "DISCIPLINE", "PROJECT", or "CUSTOM_{id}"
    val title: String,
    val customCategoryId: Long? = null
)

object ScoringEngine {
    fun rankStudentsDynamic(
        items: List<StudentWithResult>,
        activeCategories: List<AssessmentCategoryEntity>,
        filter: LeaderboardFilterCategory
    ): List<RankedStudentItem> {
        val evaluated = items.map { item ->
            val score: Double
            val display: String

            if (filter.id == "OVERALL") {
                val complete = item.isAllCategoriesEvaluated(activeCategories)
                val overallPct = item.calculateDynamicOverallPct(activeCategories)
                val provPct = item.calculateProvisionalOverallPct(activeCategories)
                val totalObtained = item.getTotalObtainedMarks(activeCategories)
                val totalMax = item.getTotalMaxMarks(activeCategories)

                val obtainedStr = if (totalObtained % 1.0 == 0.0) totalObtained.toInt().toString() else String.format(Locale.US, "%.1f", totalObtained)
                val maxStr = if (totalMax % 1.0 == 0.0) totalMax.toInt().toString() else String.format(Locale.US, "%.1f", totalMax)

                if (complete && overallPct != null) {
                    score = overallPct
                    display = String.format(Locale.US, "%.0f%% (%s/%s)", overallPct, obtainedStr, maxStr)
                } else if (provPct != null) {
                    score = provPct
                    display = String.format(Locale.US, "%.0f%% Inc (%s/%s)", provPct, obtainedStr, maxStr)
                } else {
                    score = -1.0
                    display = "Pending"
                }
            } else if (filter.customCategoryId != null) {
                val cat = activeCategories.find { it.id == filter.customCategoryId }
                val marks = item.customCategoryMarks[filter.customCategoryId]
                val max = cat?.maxMarks ?: 10.0
                score = marks?.let { if (max > 0) (it / max) * 100.0 else 0.0 } ?: -1.0
                display = marks?.let { String.format(Locale.US, "%.1f / %.0f", it, max) } ?: "Pending"
            } else {
                when (filter.id) {
                    "ATTENDANCE" -> {
                        score = item.attendancePct ?: -1.0
                        display = item.result?.attendanceMarks?.let {
                            String.format(Locale.US, "%.1f / 6", it)
                        } ?: "Pending"
                    }
                    "TEST" -> {
                        score = item.testPct ?: -1.0
                        val max = item.assessment?.testMaxMarks ?: 20.0
                        display = item.result?.testMarks?.let {
                            String.format(Locale.US, "%.1f / %.0f", it, max)
                        } ?: "Pending"
                    }
                    "DISCIPLINE" -> {
                        score = item.disciplinePct ?: -1.0
                        display = item.result?.disciplineMarks?.let {
                            String.format(Locale.US, "%.1f / 4", it)
                        } ?: "Pending"
                    }
                    "PROJECT" -> {
                        score = item.projectPct ?: -1.0
                        display = item.result?.projectMarks?.let {
                            String.format(Locale.US, "%.1f / 10", it)
                        } ?: "Pending"
                    }
                    else -> {
                        score = -1.0
                        display = "Pending"
                    }
                }
            }

            RankedStudentItem(
                rank = 0,
                student = item.student,
                result = item.result,
                assessment = item.assessment,
                scoreValue = score,
                scoreDisplay = display,
                isComplete = item.isAllCategoriesEvaluated(activeCategories),
                rawAttendance = item.result?.attendanceMarks,
                rawTest = item.result?.testMarks,
                rawDiscipline = item.result?.disciplineMarks,
                rawProject = item.result?.projectMarks,
                testMaxMarks = item.assessment?.testMaxMarks ?: 20.0,
                customMarks = item.customCategoryMarks
            )
        }

        // Sort descending: items with valid score first, then by score descending, then by name
        val sorted = evaluated.sortedWith(
            compareByDescending<RankedStudentItem> { it.scoreValue >= 0 }
                .thenByDescending { it.scoreValue }
                .thenBy { it.student.fullName }
        )

        var currentRank = 1
        var previousScore: Double? = null

        return sorted.mapIndexed { index, item ->
            if (item.scoreValue < 0) {
                item.copy(rank = sorted.size)
            } else {
                val assignedRank = if (previousScore != null && kotlin.math.abs(previousScore!! - item.scoreValue) < 0.001) {
                    currentRank
                } else {
                    currentRank = index + 1
                    currentRank
                }
                previousScore = item.scoreValue
                item.copy(rank = assignedRank)
            }
        }
    }
}

enum class LeaderboardCategory(val title: String) {
    OVERALL("Overall Performance"),
    ATTENDANCE("Attendance"),
    TEST_MARKS("Test Marks"),
    DISCIPLINE("Discipline"),
    PROJECT("Project")
}

data class BatchWithStudentCount(
    val batch: BatchEntity,
    val studentCount: Int
)
