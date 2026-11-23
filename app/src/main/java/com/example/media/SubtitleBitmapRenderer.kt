package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SubtitleAlignment
import com.example.domain.model.SubtitleAnimation
import com.example.domain.model.SubtitleSegment
import com.example.domain.model.SubtitleStyle
import com.example.ui.theme.SubtitleFonts
import kotlin.math.min

/**
 * Renders frame-accurate transparent overlay bitmaps containing styled subtitles,
 * opening title, closing text, and creator watermark for burned-in MP4 export.
 */
class SubtitleBitmapRenderer(
    private val context: Context,
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val clip: GeneratedClip,
    private val burnSubtitles: Boolean
) {
    private val style: SubtitleStyle = clip.subtitleStyle
    private val framing = clip.framing
    private val typeface: Typeface = SubtitleFonts.byId(style.fontId).loadTypeface(context, style.isBold)

    private val scaleFactor = outputWidth / 380f

    private val watermarkBitmap: Bitmap? = framing.watermarkImageUri?.let { uriStr ->
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        this.typeface = Typeface.create(this@SubtitleBitmapRenderer.typeface, Typeface.BOLD)
        textSize = 22f * scaleFactor
        setShadowLayer(8f, 0f, 3f, Color.argb(190, 0, 0, 0))
    }

    private val watermarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 248, 250, 252)
        this.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 13f * scaleFactor
        setShadowLayer(4f, 0f, 2f, Color.argb(180, 0, 0, 0))
    }

    private val bitmap: Bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)

    fun renderFrameOverlay(clipLocalTimeMs: Long): Bitmap {
        bitmap.eraseColor(Color.TRANSPARENT)

        // 1. Creator watermark image or text
        drawWatermark()

        // 2. Opening Title (first 2.5s) & Closing Text (last 2.5s)
        val totalDuration = clip.durationMs
        if (framing.openingTitle.isNotBlank() && clipLocalTimeMs <= 2_500L) {
            drawBannerText(framing.openingTitle, yFraction = 0.14f)
        }
        if (framing.closingText.isNotBlank() && clipLocalTimeMs >= (totalDuration - 2_500L).coerceAtLeast(0L)) {
            drawBannerText(framing.closingText, yFraction = 0.22f)
        }

        // 3. Burned-in Subtitles
        if (burnSubtitles) {
            val adjustedTime = clipLocalTimeMs - style.timingOffsetMs
            val activeSeg = clip.transcriptSegments.firstOrNull {
                adjustedTime >= it.startMs && adjustedTime <= it.endMs
            }
            if (activeSeg != null) {
                drawSubtitleSegment(activeSeg, adjustedTime)
            }
        }

        return bitmap
    }

    private fun drawWatermark() {
        val margin = 20f * scaleFactor
        watermarkBitmap?.let { logo ->
            val targetH = (32f * scaleFactor).toInt().coerceAtLeast(24)
            val targetW = ((logo.width.toFloat() / logo.height.coerceAtLeast(1)) * targetH).toInt()
            val dest = RectF(
                outputWidth - margin - targetW,
                margin,
                outputWidth - margin,
                margin + targetH
            )
            bgPaint.alpha = 210
            canvas.drawBitmap(logo, null, dest, bgPaint)
            bgPaint.alpha = 255
        }
        if (framing.watermarkText.isNotBlank()) {
            watermarkPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                framing.watermarkText,
                outputWidth - margin,
                margin + 18f * scaleFactor,
                watermarkPaint
            )
        }
    }

    private fun drawBannerText(text: String, yFraction: Float) {
        val maxW = (outputWidth * 0.84f).toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, titlePaint, maxW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        val boxPadX = 16f * scaleFactor
        val boxPadY = 10f * scaleFactor
        val boxW = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: maxW.toFloat()
        val cx = outputWidth / 2f
        val topY = outputHeight * yFraction
        val rect = RectF(
            cx - boxW / 2f - boxPadX,
            topY - boxPadY,
            cx + boxW / 2f + boxPadX,
            topY + layout.height + boxPadY
        )
        bgPaint.color = Color.argb(185, 124, 92, 255)
        canvas.drawRoundRect(rect, 12f * scaleFactor, 12f * scaleFactor, bgPaint)

        canvas.save()
        canvas.translate(cx - maxW / 2f, topY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawSubtitleSegment(segment: SubtitleSegment, currentMs: Long) {
        val words = segment.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return

        // Respect wordsPerLine wrapping
        val perLine = style.wordsPerLine.coerceIn(1, 8)
        val wrapped = words.chunked(perLine).joinToString("\n") { it.joinToString(" ") }

        val elapsed = (currentMs - segment.startMs).coerceAtLeast(0L)
        val animScale = when (style.animation) {
            SubtitleAnimation.POP -> {
                val p = min(1f, elapsed / 140f)
                0.88f + 0.12f * p
            }
            else -> 1f
        }
        val animOffsetY = when (style.animation) {
            SubtitleAnimation.SLIDE_UP -> {
                val p = min(1f, elapsed / 160f)
                (1f - p) * 18f * scaleFactor
            }
            else -> 0f
        }
        val animAlpha = when (style.animation) {
            SubtitleAnimation.FADE -> (min(1f, elapsed / 180f) * 255).toInt()
            else -> 255
        }

        val spannable = SpannableStringBuilder(wrapped)
        val highlightRgb = style.highlightColorHex.toInt()
        if (style.highlightKeywords) {
            val segDuration = (segment.endMs - segment.startMs).coerceAtLeast(1L)
            val progressRatio = (elapsed.toFloat() / segDuration.toFloat()).coerceIn(0f, 1f)
            val activeWordIdx = (progressRatio * words.size).toInt().coerceIn(0, words.lastIndex)

            var searchOffset = 0
            words.forEachIndexed { idx, w ->
                val foundIdx = wrapped.indexOf(w, startIndex = searchOffset)
                if (foundIdx >= 0) {
                    val isExplicitHighlight = segment.highlightedWords.any {
                        w.contains(it, ignoreCase = true)
                    }
                    val isKaraokeActive = style.animation == SubtitleAnimation.KARAOKE && idx == activeWordIdx
                    if (isExplicitHighlight || isKaraokeActive) {
                        spannable.setSpan(
                            ForegroundColorSpan(highlightRgb),
                            foundIdx,
                            foundIdx + w.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    searchOffset = foundIdx + w.length
                }
            }
        }

        val maxW = (outputWidth * 0.86f).toInt()
        val align = when (style.alignment) {
            SubtitleAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
            SubtitleAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            SubtitleAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val subStyle = this.style
        val fillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = this@SubtitleBitmapRenderer.typeface
            textSize = subStyle.fontSizeSp * scaleFactor
            color = subStyle.fontColorHex.toInt()
            alpha = animAlpha
            if (subStyle.hasShadow) {
                setShadowLayer(6f * scaleFactor, 0f, 3f * scaleFactor, Color.argb(200, 0, 0, 0))
            }
        }

        val fillLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, fillPaint, maxW)
            .setAlignment(align)
            .setLineSpacing(0f, subStyle.lineSpacingMultiplier)
            .build()

        val maxLineW = (0 until fillLayout.lineCount).maxOfOrNull { fillLayout.getLineWidth(it) } ?: maxW.toFloat()
        val boxPadX = 14f * scaleFactor
        val boxPadY = 8f * scaleFactor
        val centerY = outputHeight * subStyle.position.verticalBias + animOffsetY
        val topY = (centerY - fillLayout.height / 2f).coerceIn(16f, outputHeight - fillLayout.height - 16f)

        val boxLeft = when (subStyle.alignment) {
            SubtitleAlignment.LEFT -> outputWidth * 0.07f - boxPadX
            SubtitleAlignment.RIGHT -> outputWidth * 0.93f - maxLineW - boxPadX
            SubtitleAlignment.CENTER -> (outputWidth - maxLineW) / 2f - boxPadX
        }
        val boxRight = boxLeft + maxLineW + boxPadX * 2f

        canvas.save()
        canvas.scale(animScale, animScale, outputWidth / 2f, topY + fillLayout.height / 2f)

        if (subStyle.backgroundOpacity > 0.02f) {
            val bgRgb = subStyle.backgroundColorHex.toInt()
            bgPaint.color = Color.argb(
                ((subStyle.backgroundOpacity * 255).toInt() * animAlpha / 255).coerceIn(0, 255),
                Color.red(bgRgb),
                Color.green(bgRgb),
                Color.blue(bgRgb)
            )
            canvas.drawRoundRect(
                RectF(boxLeft, topY - boxPadY, boxRight, topY + fillLayout.height + boxPadY),
                10f * scaleFactor,
                10f * scaleFactor,
                bgPaint
            )
        }

        val translateX = (outputWidth - maxW) / 2f
        canvas.translate(translateX, topY)

        if (subStyle.outlineThickness > 0.2f) {
            val strokePaint = TextPaint(fillPaint).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = subStyle.outlineThickness * scaleFactor
                strokeJoin = Paint.Join.ROUND
                color = subStyle.outlineColorHex.toInt()
                alpha = animAlpha
                clearShadowLayer()
            }
            val strokeLayout = StaticLayout.Builder.obtain(
                wrapped, 0, wrapped.length, strokePaint, maxW
            )
                .setAlignment(align)
                .setLineSpacing(0f, subStyle.lineSpacingMultiplier)
                .build()
            strokeLayout.draw(canvas)
        }

        fillLayout.draw(canvas)
        canvas.restore()
    }
}
