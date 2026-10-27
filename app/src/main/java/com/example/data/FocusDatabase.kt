package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AssessmentDao
import com.example.data.dao.AttendanceDao
import com.example.data.dao.BatchDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.CategoryMarkDao
import com.example.data.dao.ResultDao
import com.example.data.dao.ScoringConfigDao
import com.example.data.dao.StudentDao
import com.example.data.model.AssessmentCategoryEntity
import com.example.data.model.BatchEntity
import com.example.data.model.DailyAttendanceEntity
import com.example.data.model.ScoringConfigEntity
import com.example.data.model.StudentCategoryMarkEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentWeeklyResultEntity
import com.example.data.model.WeeklyAssessmentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BatchEntity::class,
        StudentEntity::class,
        WeeklyAssessmentEntity::class,
        StudentWeeklyResultEntity::class,
        ScoringConfigEntity::class,
        AssessmentCategoryEntity::class,
        StudentCategoryMarkEntity::class,
        DailyAttendanceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun studentDao(): StudentDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun resultDao(): ResultDao
    abstract fun scoringConfigDao(): ScoringConfigDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryMarkDao(): CategoryMarkDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getDatabase(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focus_classes.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDatabase(context)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        seedDefaultCategoriesIfEmpty(context)
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedDatabase(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                val defaultBatches = (1..12).map { grade ->
                    BatchEntity(
                        name = "Class $grade",
                        grade = grade,
                        description = "Class $grade batch for session 2026-27"
                    )
                }
                database.batchDao().insertBatches(defaultBatches)
                database.scoringConfigDao().setConfig(
                    ScoringConfigEntity(
                        id = 1,
                        attendanceWeight = 25f,
                        testWeight = 25f,
                        disciplineWeight = 25f,
                        projectWeight = 25f
                    )
                )
                seedCategories(database)
            }
        }

        private fun seedDefaultCategoriesIfEmpty(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                val categories = database.categoryDao().getAllCategoriesDirect()
                if (categories.isEmpty()) {
                    seedCategories(database)
                }
            }
        }

        private suspend fun seedCategories(database: FocusDatabase) {
            val defaults = listOf(
                AssessmentCategoryEntity(
                    name = "Attendance",
                    maxMarks = 6.0,
                    weight = 25f,
                    isSystemDefault = true,
                    systemKey = "ATTENDANCE",
                    displayOrder = 1,
                    colorHex = "#22C55E"
                ),
                AssessmentCategoryEntity(
                    name = "Test Marks",
                    maxMarks = 20.0,
                    weight = 25f,
                    isSystemDefault = true,
                    systemKey = "TEST",
                    displayOrder = 2,
                    colorHex = "#8B5CF6"
                ),
                AssessmentCategoryEntity(
                    name = "Discipline",
                    maxMarks = 4.0,
                    weight = 25f,
                    isSystemDefault = true,
                    systemKey = "DISCIPLINE",
                    displayOrder = 3,
                    colorHex = "#F59E0B"
                ),
                AssessmentCategoryEntity(
                    name = "Project",
                    maxMarks = 10.0,
                    weight = 25f,
                    isSystemDefault = true,
                    systemKey = "PROJECT",
                    displayOrder = 4,
                    colorHex = "#F59E0B"
                )
            )
            database.categoryDao().insertCategories(defaults)
        }
    }
}
