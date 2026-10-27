package com.example.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.BatchEntity
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.RankedStudentItem
import com.example.data.model.WeeklyAssessmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LeaderboardExportManager {

    /**
     * Generates a sanitized file name based on batch, week, and timestamp.
     */
    fun createExportFilename(
        batch: BatchEntity?,
        assessment: WeeklyAssessmentEntity?,
        format: ExportFormat
    ): String {
        val safeBatch = (batch?.name ?: "All-Batches")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]"), "-")
            .trim('-')

        val safeWeek = (assessment?.weekLabel ?: "Week")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]"), "-")
            .trim('-')

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "focus-leaderboard-${safeBatch}-${safeWeek}-${timestamp}.${format.extension}"
    }

    /**
     * Generates and writes the exported file (PNG, JPEG, or PDF) to the app's cache directory.
     * Returns the generated File and its secure content Uri.
     */
    suspend fun generateExportFile(
        context: Context,
        batch: BatchEntity?,
        assessment: WeeklyAssessmentEntity?,
        filter: LeaderboardFilterCategory,
        rankedItems: List<RankedStudentItem>,
        stats: LeaderboardExportStats,
        format: ExportFormat
    ): Result<Pair<File, Uri>> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val filename = createExportFilename(batch, assessment, format)
            val outputFile = File(exportDir, filename)

            when (format) {
                ExportFormat.PNG -> {
                    val bitmap = LeaderboardRenderer.renderPageBitmap(
                        context = context,
                        batch = batch,
                        assessment = assessment,
                        filter = filter,
                        rankedItems = rankedItems,
                        stats = stats,
                        pageIndex = 0,
                        totalPages = 1
                    )
                    FileOutputStream(outputFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    bitmap.recycle()
                }

                ExportFormat.JPEG -> {
                    val bitmap = LeaderboardRenderer.renderPageBitmap(
                        context = context,
                        batch = batch,
                        assessment = assessment,
                        filter = filter,
                        rankedItems = rankedItems,
                        stats = stats,
                        pageIndex = 0,
                        totalPages = 1
                    )
                    FileOutputStream(outputFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        out.flush()
                    }
                    bitmap.recycle()
                }

                ExportFormat.PDF -> {
                    val pdfDocument = LeaderboardRenderer.renderPdfDocument(
                        context = context,
                        batch = batch,
                        assessment = assessment,
                        filter = filter,
                        rankedItems = rankedItems,
                        stats = stats
                    )
                    FileOutputStream(outputFile).use { out ->
                        pdfDocument.writeTo(out)
                        out.flush()
                    }
                    pdfDocument.close()
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, outputFile)
            Result.success(Pair(outputFile, uri))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves the file to device public storage (Pictures for images, Downloads for PDF)
     * using MediaStore for Android 10+ without requiring dangerous permissions.
     */
    suspend fun saveToDeviceStorage(
        context: Context,
        file: File,
        format: ExportFormat
    ): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            }

            val collectionUri: Uri = when (format) {
                ExportFormat.PNG, ExportFormat.JPEG -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FocusClasses")
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }
                ExportFormat.PDF -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FocusClasses")
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Files.getContentUri("external")
                    }
                }
            }

            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create storage record"))

            resolver.openOutputStream(itemUri).use { outStream ->
                if (outStream == null) return@withContext Result.failure(IllegalStateException("Cannot open output stream"))
                file.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens Android Sharesheet to share the exported file.
     */
    fun shareExportedFile(
        context: Context,
        fileUri: Uri,
        format: ExportFormat,
        title: String = "Share Leaderboard"
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
