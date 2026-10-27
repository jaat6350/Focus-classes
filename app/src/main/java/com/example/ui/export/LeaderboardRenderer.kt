package com.example.ui.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.BatchEntity
import com.example.data.model.LeaderboardFilterCategory
import com.example.data.model.RankedStudentItem
import com.example.data.model.WeeklyAssessmentEntity
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object LeaderboardRenderer {

    // 2:3 aspect ratio high resolution for PNG/JPEG
    const val POSTER_WIDTH = 2400
    const val POSTER_HEIGHT = 3600

    // Standard A4 dimensions in points (72 points/inch)
    const val A4_WIDTH_POINTS = 595
    const val A4_HEIGHT_POINTS = 842

    // Color Palette (Pure ARGB integer values, compatible with Android and local JVM tests)
    private val COLOR_NAVY = 0xFF0B2D5C.toInt()
    private val COLOR_ROYAL = 0xFF1459B3.toInt()
    private val COLOR_GOLD = 0xFFFFC928.toInt()
    private val COLOR_GOLD_DARK = 0xFFD49A00.toInt()
    private val COLOR_SILVER = 0xFFC0C0C0.toInt()
    private val COLOR_BRONZE = 0xFFCD7F32.toInt()
    private val COLOR_BG = 0xFFF6F8FC.toInt()
    private val COLOR_TEXT_DARK = 0xFF172B4D.toInt()
    private val COLOR_TEXT_MUTED = 0xFF6B778C.toInt()
    private val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private val COLOR_TABLE_HEADER = 0xFFE8F1FC.toInt()
    private val COLOR_ROW_ALT = 0xFFFAFCFE.toInt()
    private val COLOR_BORDER = 0xFFDCE5F0.toInt()
    private val COLOR_STATUS_COMPLETE_BG = 0xFFE3FCEF.toInt()
    private val COLOR_STATUS_COMPLETE_TEXT = 0xFF006644.toInt()
    private val COLOR_STATUS_INC_BG = 0xFFFFF0B3.toInt()
    private val COLOR_STATUS_INC_TEXT = 0xFF974F00.toInt()

    /**
     * Renders a specific page of the leaderboard into a high-res Bitmap (2400 x 3600).
     */
    fun renderPageBitmap(
        context: Context,
        batch: BatchEntity?,
        assessment: WeeklyAssessmentEntity?,
        filter: LeaderboardFilterCategory,
        rankedItems: List<RankedStudentItem>,
        stats: LeaderboardExportStats,
        pageIndex: Int = 0,
        totalPages: Int = 1
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(POSTER_WIDTH, POSTER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawPage(
            context = context,
            canvas = canvas,
            width = POSTER_WIDTH.toFloat(),
            height = POSTER_HEIGHT.toFloat(),
            batch = batch,
            assessment = assessment,
            filter = filter,
            rankedItems = rankedItems,
            stats = stats,
            pageIndex = pageIndex,
            totalPages = totalPages
        )

        return bitmap
    }

    /**
     * Calculates total pages required for the given number of students.
     */
    fun calculateTotalPages(totalStudents: Int): Int {
        if (totalStudents <= 10) return 1
        val remaining = totalStudents - 10
        val subsequentPages = (remaining + 17) / 18 // up to 18 rows on page 2+
        return 1 + subsequentPages
    }

    /**
     * Renders full multi-page PDF document.
     */
    fun renderPdfDocument(
        context: Context,
        batch: BatchEntity?,
        assessment: WeeklyAssessmentEntity?,
        filter: LeaderboardFilterCategory,
        rankedItems: List<RankedStudentItem>,
        stats: LeaderboardExportStats
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        val totalPages = calculateTotalPages(rankedItems.size)

        for (pageIdx in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_POINTS, A4_HEIGHT_POINTS, pageIdx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Render crisp at high scale to match A4
            val scaleX = A4_WIDTH_POINTS.toFloat() / POSTER_WIDTH.toFloat()
            val scaleY = A4_HEIGHT_POINTS.toFloat() / POSTER_HEIGHT.toFloat()

            canvas.save()
            canvas.scale(scaleX, scaleY)

            drawPage(
                context = context,
                canvas = canvas,
                width = POSTER_WIDTH.toFloat(),
                height = POSTER_HEIGHT.toFloat(),
                batch = batch,
                assessment = assessment,
                filter = filter,
                rankedItems = rankedItems,
                stats = stats,
                pageIndex = pageIdx,
                totalPages = totalPages
            )

            canvas.restore()
            pdfDocument.finishPage(page)
        }

        return pdfDocument
    }

    /**
     * Master draw routine for a single page.
     */
    private fun drawPage(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        batch: BatchEntity?,
        assessment: WeeklyAssessmentEntity?,
        filter: LeaderboardFilterCategory,
        rankedItems: List<RankedStudentItem>,
        stats: LeaderboardExportStats,
        pageIndex: Int,
        totalPages: Int
    ) {
        // 1. Background
        val bgPaint = Paint().apply { color = COLOR_BG }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Top Decorative Waves
        drawTopDecorativeWaves(canvas, width)

        // 3. Header (Logo, Class, Week, Slogan)
        val headerBottomY = drawHeader(
            context = context,
            canvas = canvas,
            width = width,
            batch = batch,
            assessment = assessment,
            filter = filter,
            pageIndex = pageIndex,
            totalPages = totalPages
        )

        var currentY = headerBottomY + 20f

        // 4. Podium (Page 1 Only)
        if (pageIndex == 0) {
            val eligiblePodiumStudents = rankedItems.filter { it.scoreValue >= 0 }.take(3)
            val podiumBottomY = drawPodium(
                context = context,
                canvas = canvas,
                width = width,
                startY = currentY,
                podiumStudents = eligiblePodiumStudents,
                filter = filter
            )
            currentY = podiumBottomY + 30f
        }

        // 5. Table of Students
        val isFirstPage = (pageIndex == 0)
        val studentsForThisPage: List<RankedStudentItem>
        val startIndex: Int

        if (isFirstPage) {
            startIndex = 0
            val maxRowsOnPage1 = if (totalPages > 1) 10 else 14
            studentsForThisPage = rankedItems.take(maxRowsOnPage1)
        } else {
            startIndex = 10 + (pageIndex - 1) * 18
            val endIndex = min(rankedItems.size, startIndex + 18)
            studentsForThisPage = if (startIndex < rankedItems.size) {
                rankedItems.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        }

        val tableBottomY = drawStudentTable(
            context = context,
            canvas = canvas,
            width = width,
            startY = currentY,
            students = studentsForThisPage,
            filter = filter,
            isFirstPage = isFirstPage
        )

        // 6. Bottom Statistics Strip (Page 1 only or if single page)
        if (isFirstPage) {
            val statsY = height - 360f
            drawStatsStrip(canvas, width, statsY, stats)
        }

        // 7. Footer
        drawFooter(canvas, width, height - 70f, pageIndex, totalPages)
    }

    private fun drawTopDecorativeWaves(canvas: Canvas, width: Float) {
        val paintRoyal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ROYAL }
        val paintNavy = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_NAVY }

        // Royal blue background arc
        val pathRoyal = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, 360f)
            cubicTo(width * 0.75f, 390f, width * 0.25f, 300f, 0f, 350f)
            close()
        }
        canvas.drawPath(pathRoyal, paintRoyal)

        // Navy blue overlay arc
        val pathNavy = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, 330f)
            cubicTo(width * 0.70f, 350f, width * 0.35f, 260f, 0f, 320f)
            close()
        }
        canvas.drawPath(pathNavy, paintNavy)
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        width: Float,
        batch: BatchEntity?,
        assessment: WeeklyAssessmentEntity?,
        filter: LeaderboardFilterCategory,
        pageIndex: Int,
        totalPages: Int
    ): Float {
        // Logo Container on Top Left
        val logoCardLeft = 90f
        val logoCardTop = 60f
        val logoCardWidth = 560f
        val logoCardHeight = 220f

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            setShadowLayer(16f, 0f, 8f, 0x28000000)
        }
        val cardRect = RectF(logoCardLeft, logoCardTop, logoCardLeft + logoCardWidth, logoCardTop + logoCardHeight)
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

        // Load authentic Focus Classes logo
        try {
            val logoOptions = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.focus_classes_logo, logoOptions)
            if (logoBitmap != null) {
                val padding = 20f
                val destRect = RectF(
                    logoCardLeft + padding,
                    logoCardTop + padding,
                    logoCardLeft + logoCardWidth - padding,
                    logoCardTop + logoCardHeight - padding
                )

                // Scale proportionally preserving aspect ratio
                val scale = min(destRect.width() / logoBitmap.width, destRect.height() / logoBitmap.height)
                val drawW = logoBitmap.width * scale
                val drawH = logoBitmap.height * scale
                val drawX = destRect.left + (destRect.width() - drawW) / 2f
                val drawY = destRect.top + (destRect.height() - drawH) / 2f

                val drawRect = RectF(drawX, drawY, drawX + drawW, drawY + drawH)
                canvas.drawBitmap(logoBitmap, null, drawRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
        } catch (_: Exception) {
            // Fallback text if logo fails to decode
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_NAVY
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("FOCUS CLASSES", cardRect.centerX(), cardRect.centerY() + 14f, textPaint)
        }

        // Top Right: Slogan "Track Progress. Build Leaders."
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Track Progress", width - 90f, 130f, sloganPaint)
        sloganPaint.color = COLOR_GOLD
        canvas.drawText("Build Leaders", width - 90f, 195f, sloganPaint)

        // Page Number Indicator if multi-page
        if (totalPages > 1) {
            val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_WHITE
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Page ${pageIndex + 1} of $totalPages", width - 90f, 255f, pageNumPaint)
        }

        // Center Content Below Wave
        val contentStartY = 390f

        // Crown above Class Name
        drawCrown(canvas, width / 2f, contentStartY + 10f, 80f, 50f, COLOR_GOLD)

        // Batch Name: e.g. "CLASS 6"
        val batchName = batch?.name?.uppercase(Locale.US) ?: "BATCH LEADERBOARD"
        val classPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 88f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        canvas.drawText(batchName, width / 2f, contentStartY + 120f, classPaint)

        // Leaderboard Title
        val title = when (filter.id) {
            "OVERALL" -> "OVERALL PERFORMANCE LEADERBOARD"
            "ATTENDANCE" -> "ATTENDANCE LEADERBOARD"
            "TEST" -> "TEST MARKS LEADERBOARD"
            "DISCIPLINE" -> "DISCIPLINE LEADERBOARD"
            "PROJECT" -> "PROJECT LEADERBOARD"
            else -> "${filter.title.uppercase(Locale.US)} LEADERBOARD"
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.02f
        }
        canvas.drawText(title, width / 2f, contentStartY + 195f, titlePaint)

        // Week Label & Date Range with Accent Lines
        val weekLabel = assessment?.weekLabel ?: "Weekly Assessment"
        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = contentStartY + 260f
        canvas.drawText(weekLabel, width / 2f, textY, weekPaint)

        // Accent lines flanking the week text
        val textWidth = weekPaint.measureText(weekLabel)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL
            strokeWidth = 3f
        }
        val lineMargin = 30f
        val lineLength = 260f
        val lineY = textY - 14f

        canvas.drawLine(
            (width / 2f) - (textWidth / 2f) - lineMargin - lineLength,
            lineY,
            (width / 2f) - (textWidth / 2f) - lineMargin,
            lineY,
            linePaint
        )
        canvas.drawLine(
            (width / 2f) + (textWidth / 2f) + lineMargin,
            lineY,
            (width / 2f) + (textWidth / 2f) + lineMargin + lineLength,
            lineY,
            linePaint
        )

        return contentStartY + 285f
    }

    private fun drawCrown(canvas: Canvas, centerX: Float, centerY: Float, crownWidth: Float, crownHeight: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val halfW = crownWidth / 2f
        val halfH = crownHeight / 2f

        val path = Path().apply {
            moveTo(centerX - halfW, centerY + halfH)
            lineTo(centerX - halfW, centerY - halfH * 0.4f)
            lineTo(centerX - halfW * 0.5f, centerY)
            lineTo(centerX, centerY - halfH)
            lineTo(centerX + halfW * 0.5f, centerY)
            lineTo(centerX + halfW, centerY - halfH * 0.4f)
            lineTo(centerX + halfW, centerY + halfH)
            close()
        }
        canvas.drawPath(path, paint)

        // Small circles on peaks
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX - halfW, centerY - halfH * 0.4f, 6f, circlePaint)
        canvas.drawCircle(centerX, centerY - halfH, 7f, circlePaint)
        canvas.drawCircle(centerX + halfW, centerY - halfH * 0.4f, 6f, circlePaint)
    }

    private fun drawPodium(
        context: Context,
        canvas: Canvas,
        width: Float,
        startY: Float,
        podiumStudents: List<RankedStudentItem>,
        filter: LeaderboardFilterCategory
    ): Float {
        if (podiumStudents.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_MUTED
                textSize = 38f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No eligible assessment scores available yet.", width / 2f, startY + 100f, emptyPaint)
            return startY + 160f
        }

        // Podium dimensions
        val centerX = width / 2f
        val pedestalW = 540f
        val gap = 60f

        val firstPlace = podiumStudents.getOrNull(0)
        val secondPlace = podiumStudents.getOrNull(1)
        val thirdPlace = podiumStudents.getOrNull(2)

        val pedestalBaseY = startY + 680f

        // Draw 2nd place (Left)
        if (secondPlace != null) {
            drawPodiumColumn(
                context = context,
                canvas = canvas,
                xCenter = centerX - pedestalW - gap,
                baseY = pedestalBaseY,
                pedestalHeight = 220f,
                student = secondPlace,
                rankLabel = "2nd",
                rankColor = COLOR_SILVER,
                filter = filter
            )
        }

        // Draw 1st place (Center, Taller)
        if (firstPlace != null) {
            drawPodiumColumn(
                context = context,
                canvas = canvas,
                xCenter = centerX,
                baseY = pedestalBaseY,
                pedestalHeight = 280f,
                student = firstPlace,
                rankLabel = "1st",
                rankColor = COLOR_GOLD,
                filter = filter
            )
        }

        // Draw 3rd place (Right)
        if (thirdPlace != null) {
            drawPodiumColumn(
                context = context,
                canvas = canvas,
                xCenter = centerX + pedestalW + gap,
                baseY = pedestalBaseY,
                pedestalHeight = 180f,
                student = thirdPlace,
                rankLabel = "3rd",
                rankColor = COLOR_BRONZE,
                filter = filter
            )
        }

        return pedestalBaseY
    }

    private fun drawPodiumColumn(
        context: Context,
        canvas: Canvas,
        xCenter: Float,
        baseY: Float,
        pedestalHeight: Float,
        student: RankedStudentItem,
        rankLabel: String,
        rankColor: Int,
        filter: LeaderboardFilterCategory
    ) {
        val pedestalW = 540f
        val pedestalTop = baseY - pedestalHeight
        val photoRadius = 120f
        val photoCenterY = pedestalTop - photoRadius - 60f

        // 1. Crown above student
        drawCrown(canvas, xCenter, photoCenterY - photoRadius - 30f, 60f, 38f, rankColor)

        // 2. Laurel wreath ring / Accent ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rankColor
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        canvas.drawCircle(xCenter, photoCenterY, photoRadius + 14f, ringPaint)

        // 3. Student Avatar / Photo
        drawCircularStudentImage(
            context = context,
            canvas = canvas,
            centerX = xCenter,
            centerY = photoCenterY,
            radius = photoRadius,
            student = student
        )

        // 4. Rank Badge Ribbon (e.g. "1st", "2nd", "3rd")
        val badgeW = 200f
        val badgeH = 64f
        val badgeRect = RectF(
            xCenter - (badgeW / 2f),
            photoCenterY + photoRadius - (badgeH / 2f) + 10f,
            xCenter + (badgeW / 2f),
            photoCenterY + photoRadius + (badgeH / 2f) + 10f
        )

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rankColor
            setShadowLayer(8f, 0f, 4f, 0x50000000)
        }
        canvas.drawRoundRect(badgeRect, 32f, 32f, badgePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (rankColor == COLOR_SILVER) COLOR_NAVY else COLOR_WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(rankLabel, xCenter, badgeRect.centerY() + 13f, badgeTextPaint)

        // 5. Dark Navy Pedestal with Subtle Top Bevel
        val pedestalRect = RectF(
            xCenter - (pedestalW / 2f),
            pedestalTop,
            xCenter + (pedestalW / 2f),
            baseY
        )
        val pedestalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            setShadowLayer(16f, 0f, 8f, 0x3C000000)
        }
        canvas.drawRoundRect(pedestalRect, 24f, 24f, pedestalPaint)

        // Pedestal Top Gold/Silver/Bronze Accent Line
        val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rankColor
            strokeWidth = 8f
        }
        canvas.drawLine(
            pedestalRect.left + 20f,
            pedestalRect.top + 4f,
            pedestalRect.right - 20f,
            pedestalRect.top + 4f,
            accentLinePaint
        )

        // Student Name on Pedestal
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.student.fullName, xCenter, pedestalTop + 75f, namePaint)

        // Score on Pedestal (e.g. "96.5%")
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (rankLabel == "1st") COLOR_GOLD else COLOR_WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val scoreStr = if (filter.id == "OVERALL") {
            String.format(Locale.US, "%.1f%%", student.scoreValue)
        } else {
            student.scoreDisplay
        }
        canvas.drawText(scoreStr, xCenter, pedestalTop + 155f, scorePaint)
    }

    private fun drawCircularStudentImage(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        student: RankedStudentItem
    ) {
        val imagePath = student.student.profileImagePath
        val targetDiameter = (radius * 2f).toInt()
        val loadedBitmap = if (!imagePath.isNullOrBlank()) {
            loadOrientedStudentBitmap(imagePath, targetDiameter)
        } else {
            null
        }

        if (loadedBitmap != null) {
            // Draw circle-cropped student photo with exact center alignment and upright orientation
            drawCenterCropCircleBitmap(canvas, loadedBitmap, centerX, centerY, radius)
        } else {
            // Consistent illustrated child avatar placeholder
            drawChildAvatar(context, canvas, centerX, centerY, radius, student.student.fullName)
        }
    }

    /**
     * Loads a student photo file, honoring EXIF orientation metadata (e.g. 90 deg, 270 deg, flip)
     * so that the rendered photo is always upright and in the exact same orientation as uploaded.
     * Uses efficient subsampling (inSampleSize) to prevent high-res camera OOM.
     */
    fun loadOrientedStudentBitmap(imagePath: String, targetDiameter: Int): Bitmap? {
        val file = File(imagePath)
        if (!file.exists() || !file.canRead()) return null

        return try {
            // 1. Read EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                val exif = ExifInterface(file.absolutePath)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Throwable) {}

            // 2. Decode bounds for inSampleSize calculation
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            val minDim = minOf(origW, origH)
            val targetSize = maxOf(targetDiameter * 2, 128) // 2x supersampling for high resolution
            while (minDim / (sampleSize * 2) >= targetSize && sampleSize < 16) {
                sampleSize *= 2
            }

            // 3. Decode sampled bitmap
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null

            // 4. Transform according to EXIF so orientation matches what user uploaded
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> {}
            }

            if (!matrix.isIdentity) {
                val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (transformed != decoded) {
                    decoded.recycle()
                }
                transformed
            } else {
                decoded
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Draws a bitmap cropped into a circle centered at (centerX, centerY) with the given radius,
     * maintaining aspect ratio and centering identically to Jetpack Compose ContentScale.Crop.
     */
    fun drawCenterCropCircleBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val diameter = radius * 2f
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f) return

        // Scale factor: maximum of width-ratio and height-ratio (ContentScale.Crop)
        val scale = maxOf(diameter / bw, diameter / bh)
        val scaledW = bw * scale
        val scaledH = bh * scale

        // Offsets to center the scaled image exactly within the circular bounds
        val dx = (centerX - radius) + (diameter - scaledW) * 0.5f
        val dy = (centerY - radius) + (diameter - scaledH) * 0.5f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }

        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(matrix)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
        }

        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    private fun drawChildAvatar(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        name: String
    ) {
        // Background Circle (Soft Blue)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEBF3FC.toInt() }
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        // Try to draw the vector child avatar if available
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_student_avatar_placeholder)
            if (drawable != null) {
                val size = (radius * 1.9f).toInt()
                val left = (centerX - size / 2f).toInt()
                val top = (centerY - size / 2f).toInt()
                drawable.setBounds(left, top, left + size, top + size)
                drawable.draw(canvas)
                return
            }
        } catch (_: Exception) {}

        // Fallback: Elegant stylized initials avatar
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL
            textSize = radius * 0.75f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(initials, centerX, centerY + (radius * 0.28f), initialsPaint)
    }

    private fun drawStudentTable(
        context: Context,
        canvas: Canvas,
        width: Float,
        startY: Float,
        students: List<RankedStudentItem>,
        filter: LeaderboardFilterCategory,
        isFirstPage: Boolean
    ): Float {
        val tableLeft = 90f
        val tableRight = width - 90f
        val tableWidth = tableRight - tableLeft

        val headerHeight = 84f
        val rowHeight = if (isFirstPage) 96f else 102f
        val totalTableHeight = headerHeight + (students.size * rowHeight)

        // Table Background Card
        val tableRect = RectF(tableLeft, startY, tableRight, startY + totalTableHeight)
        val tableBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            setShadowLayer(14f, 0f, 6f, 0x1E000000)
        }
        canvas.drawRoundRect(tableRect, 24f, 24f, tableBgPaint)

        // Table Header Background
        val headerRect = RectF(tableLeft, startY, tableRight, startY + headerHeight)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TABLE_HEADER }
        canvas.drawRoundRect(headerRect, 24f, 24f, headerBgPaint)
        // Square off bottom corners of header
        canvas.drawRect(tableLeft, startY + 24f, tableRight, startY + headerHeight, headerBgPaint)

        // Column X coordinates for Overall Performance
        val isOverall = (filter.id == "OVERALL")
        val colRank = tableLeft + 50f
        val colAvatar = tableLeft + 140f
        val colName = tableLeft + 195f
        val colClass = tableLeft + 720f
        val colAtt = tableLeft + 900f
        val colTest = tableLeft + 1140f
        val colDisc = tableLeft + 1380f
        val colProj = tableLeft + 1620f
        val colOverall = tableLeft + 1880f
        val colStatus = tableRight - 110f

        // Draw Header Text
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerY = startY + 54f
        headerTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("#", colRank, headerY, headerTextPaint)

        headerTextPaint.textAlign = Paint.Align.LEFT
        // Header aligns with the left edge of the student avatar (colAvatar - 32f)
        canvas.drawText("Student Name", colAvatar - 32f, headerY, headerTextPaint)

        headerTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Class", colClass, headerY, headerTextPaint)

        if (isOverall) {
            canvas.drawText("Attendance", colAtt, headerY, headerTextPaint)
            canvas.drawText("Test", colTest, headerY, headerTextPaint)
            canvas.drawText("Discipline", colDisc, headerY, headerTextPaint)
            canvas.drawText("Project", colProj, headerY, headerTextPaint)
            canvas.drawText("Overall %", colOverall, headerY, headerTextPaint)
        } else {
            canvas.drawText("Score / Marks", colTest, headerY, headerTextPaint)
            canvas.drawText("Percentage", colOverall, headerY, headerTextPaint)
        }

        canvas.drawText("Status", colStatus, headerY, headerTextPaint)

        // Divider under header
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 2f
        }
        canvas.drawLine(tableLeft, startY + headerHeight, tableRight, startY + headerHeight, dividerPaint)

        // Draw Rows
        var rowY = startY + headerHeight

        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val royalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        students.forEachIndexed { index, student ->
            val rowRect = RectF(tableLeft, rowY, tableRight, rowY + rowHeight)

            // Alternate row background
            if (index % 2 == 1) {
                val rowBgPaint = Paint().apply { color = COLOR_ROW_ALT }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            val textBaseline = rowY + (rowHeight / 2f) + 12f

            // 1. Rank: Medals for 1, 2, 3 or circular badge
            val rank = student.rank
            when (rank) {
                1 -> drawRankMedal(canvas, colRank, rowY + (rowHeight / 2f), COLOR_GOLD, "1")
                2 -> drawRankMedal(canvas, colRank, rowY + (rowHeight / 2f), COLOR_SILVER, "2")
                3 -> drawRankMedal(canvas, colRank, rowY + (rowHeight / 2f), COLOR_BRONZE, "3")
                else -> {
                    cellTextPaint.textAlign = Paint.Align.CENTER
                    cellTextPaint.color = COLOR_TEXT_MUTED
                    canvas.drawText(rank.toString(), colRank, textBaseline, cellTextPaint)
                }
            }

            // 2. Student Avatar
            val avatarRadius = 32f
            drawCircularStudentImage(
                context = context,
                canvas = canvas,
                centerX = colAvatar,
                centerY = rowY + (rowHeight / 2f),
                radius = avatarRadius,
                student = student
            )

            // 3. Student Name
            boldTextPaint.textAlign = Paint.Align.LEFT
            boldTextPaint.color = COLOR_TEXT_DARK
            val maxNameWidth = colClass - colName - 30f
            val truncatedName = ellipsizeText(student.student.fullName, boldTextPaint, maxNameWidth)
            canvas.drawText(truncatedName, colName, textBaseline, boldTextPaint)

            // 4. Class
            val gradeNumber = student.assessment?.batchId?.let { "6" } ?: "6" // e.g. class grade
            cellTextPaint.textAlign = Paint.Align.CENTER
            cellTextPaint.color = COLOR_TEXT_DARK
            canvas.drawText(gradeNumber, colClass, textBaseline, cellTextPaint)

            // 5. Category Scores
            if (isOverall) {
                // Attendance %
                val attStr = student.rawAttendance?.let { String.format(Locale.US, "%.0f%%", (it / 6.0) * 100.0) } ?: "-"
                canvas.drawText(attStr, colAtt, textBaseline, cellTextPaint)

                // Test %
                val maxTest = student.testMaxMarks
                val testStr = student.rawTest?.let { String.format(Locale.US, "%.0f%%", if (maxTest > 0) (it / maxTest) * 100.0 else 0.0) } ?: "-"
                canvas.drawText(testStr, colTest, textBaseline, cellTextPaint)

                // Discipline %
                val discStr = student.rawDiscipline?.let { String.format(Locale.US, "%.0f%%", (it / 4.0) * 100.0) } ?: "-"
                canvas.drawText(discStr, colDisc, textBaseline, cellTextPaint)

                // Project %
                val projStr = student.rawProject?.let { String.format(Locale.US, "%.0f%%", (it / 10.0) * 100.0) } ?: "-"
                canvas.drawText(projStr, colProj, textBaseline, cellTextPaint)

                // Overall %
                royalTextPaint.textAlign = Paint.Align.CENTER
                val overallStr = if (student.scoreValue >= 0) String.format(Locale.US, "%.1f%%", student.scoreValue) else "-"
                canvas.drawText(overallStr, colOverall, textBaseline, royalTextPaint)
            } else {
                // Specific category display
                cellTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(student.scoreDisplay, colTest, textBaseline, cellTextPaint)

                royalTextPaint.textAlign = Paint.Align.CENTER
                val pctStr = if (student.scoreValue >= 0) String.format(Locale.US, "%.1f%%", student.scoreValue) else "-"
                canvas.drawText(pctStr, colOverall, textBaseline, royalTextPaint)
            }

            // 6. Status Badge ("Complete" / "Pending")
            val isComplete = student.isComplete
            val statusBgColor = if (isComplete) COLOR_STATUS_COMPLETE_BG else COLOR_STATUS_INC_BG
            val statusTextColor = if (isComplete) COLOR_STATUS_COMPLETE_TEXT else COLOR_STATUS_INC_TEXT
            val statusLabel = if (isComplete) "Complete" else "Pending"

            val pillW = 160f
            val pillH = 48f
            val pillRect = RectF(
                colStatus - (pillW / 2f),
                rowY + (rowHeight / 2f) - (pillH / 2f),
                colStatus + (pillW / 2f),
                rowY + (rowHeight / 2f) + (pillH / 2f)
            )

            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusBgColor }
            canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)

            val statusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = statusTextColor
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(statusLabel, colStatus, pillRect.centerY() + 10f, statusTextPaint)

            // Row Bottom Divider
            canvas.drawLine(tableLeft, rowY + rowHeight, tableRight, rowY + rowHeight, dividerPaint)

            rowY += rowHeight
        }

        return rowY
    }

    private fun drawRankMedal(canvas: Canvas, x: Float, y: Float, color: Int, text: String) {
        val medalRadius = 26f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(x, y, medalRadius, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = COLOR_WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, x, y + 10f, textPaint)
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    private fun drawStatsStrip(canvas: Canvas, width: Float, startY: Float, stats: LeaderboardExportStats) {
        val stripLeft = 90f
        val stripRight = width - 90f
        val stripHeight = 180f

        val stripRect = RectF(stripLeft, startY, stripRight, startY + stripHeight)
        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            setShadowLayer(16f, 0f, 8f, 0x3C000000)
        }
        canvas.drawRoundRect(stripRect, 28f, 28f, stripPaint)

        // 4 Columns
        val colWidth = (stripRight - stripLeft) / 4f

        // 1. Total Students
        drawStatColumn(
            canvas = canvas,
            xCenter = stripLeft + (colWidth * 0.5f),
            centerY = startY + (stripHeight / 2f),
            label = "Total Students",
            value = stats.totalStudents.toString(),
            subtext = null,
            iconSymbol = "👥"
        )

        // Divider 1
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x3CFFFFFF
            strokeWidth = 2f
        }
        canvas.drawLine(stripLeft + colWidth, startY + 25f, stripLeft + colWidth, startY + stripHeight - 25f, divPaint)

        // 2. Highest Score
        val highestSubtext = stats.highestStudentName?.let { "($it)" }
        drawStatColumn(
            canvas = canvas,
            xCenter = stripLeft + (colWidth * 1.5f),
            centerY = startY + (stripHeight / 2f),
            label = "Highest Score",
            value = stats.highestScoreDisplay,
            subtext = highestSubtext,
            iconSymbol = "🏆"
        )

        // Divider 2
        canvas.drawLine(stripLeft + (colWidth * 2f), startY + 25f, stripLeft + (colWidth * 2f), startY + stripHeight - 25f, divPaint)

        // 3. Class Average
        drawStatColumn(
            canvas = canvas,
            xCenter = stripLeft + (colWidth * 2.5f),
            centerY = startY + (stripHeight / 2f),
            label = "Class Average",
            value = stats.classAverageDisplay,
            subtext = null,
            iconSymbol = "📊"
        )

        // Divider 3
        canvas.drawLine(stripLeft + (colWidth * 3f), startY + 25f, stripLeft + (colWidth * 3f), startY + stripHeight - 25f, divPaint)

        // 4. Top 3 Average
        val top3Subtext = if (stats.topThreeCount in 1..2) "(${stats.topThreeCount} students)" else null
        drawStatColumn(
            canvas = canvas,
            xCenter = stripLeft + (colWidth * 3.5f),
            centerY = startY + (stripHeight / 2f),
            label = "Top 3 Average",
            value = stats.topThreeAverageDisplay,
            subtext = top3Subtext,
            iconSymbol = "⭐"
        )
    }

    private fun drawStatColumn(
        canvas: Canvas,
        xCenter: Float,
        centerY: Float,
        label: String,
        value: String,
        subtext: String?,
        iconSymbol: String
    ) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA6C8E0.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        if (subtext != null) {
            canvas.drawText(label, xCenter, centerY - 28f, labelPaint)
            canvas.drawText(value, xCenter, centerY + 28f, valuePaint)
            canvas.drawText(subtext, xCenter, centerY + 62f, subtextPaint)
        } else {
            canvas.drawText(label, xCenter, centerY - 20f, labelPaint)
            canvas.drawText(value, xCenter, centerY + 42f, valuePaint)
        }
    }

    private fun drawFooter(canvas: Canvas, width: Float, footerY: Float, pageIndex: Int, totalPages: Int) {
        val slogan = "Better Students  •  Brighter Futures"
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL
            textSize = 40f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }

        val textW = sloganPaint.measureText(slogan)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 3f
        }

        val margin = 30f
        val lineLen = 300f
        canvas.drawLine((width / 2f) - (textW / 2f) - margin - lineLen, footerY - 14f, (width / 2f) - (textW / 2f) - margin, footerY - 14f, linePaint)
        canvas.drawLine((width / 2f) + (textW / 2f) + margin, footerY - 14f, (width / 2f) + (textW / 2f) + margin + lineLen, footerY - 14f, linePaint)

        canvas.drawText(slogan, width / 2f, footerY, sloganPaint)
    }
}
