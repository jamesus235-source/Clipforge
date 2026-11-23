package com.example.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Generates a real playable H.264/AAC studio sample MP4 file on-device using MediaCodec + MediaMuxer.
 * Includes animated studio visuals, topic cards, and speech-cadence audio with deliberate silent
 * pauses so ExoPlayer playback, real PCM silence detection, live editing, and Media3 Transformer
 * MP4 export work out-of-the-box without external downloads.
 */
object SampleVideoGenerator {

    private const val WIDTH = 960
    private const val HEIGHT = 540
    private const val FPS = 24
    private const val DURATION_SEC = 24
    private const val VIDEO_BITRATE = 2_400_000
    private const val SAMPLE_RATE = 44100
    private const val AUDIO_BITRATE = 128_000

    suspend fun ensureSampleVideo(context: Context): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "videos").apply { mkdirs() }
        val outFile = File(dir, "clipforge_sample_studio.mp4")
        if (outFile.exists() && outFile.length() > 50_000L) {
            return@withContext outFile
        }
        val tmpFile = File(dir, "clipforge_sample_studio.tmp.mp4")
        if (tmpFile.exists()) tmpFile.delete()

        generateInternal(tmpFile)
        if (outFile.exists()) outFile.delete()
        tmpFile.renameTo(outFile)
        outFile
    }

    private fun generateInternal(outputFile: File) {
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = videoEncoder.createInputSurface()
        videoEncoder.start()

        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder.start()

        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var muxerStarted = false

        val totalFrames = DURATION_SEC * FPS
        val frameDurationUs = 1_000_000L / FPS
        val totalAudioSamples = DURATION_SEC * SAMPLE_RATE

        val videoBufferInfo = MediaCodec.BufferInfo()
        val audioBufferInfo = MediaCodec.BufferInfo()

        // First render all video frames into videoEncoder; hold buffers until both tracks get format if needed,
        // or interleave audio + video cleanly so muxer starts immediately when both formats arrive.
        var frameIdx = 0
        var videoDone = false
        var audioSampleCursor = 0
        var audioInputDone = false
        var audioDone = false

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val chapters = listOf(
            "01 • THE 3-SECOND HOOK RULE",
            "02 • WHY AUDIO CLARITY BEATS 4K",
            "03 • ONE IDEA PER VERTICAL CLIP",
            "04 • CONTRAST & SAFE ZONES"
        )

        fun drainVideo(endOfStream: Boolean) {
            if (endOfStream) {
                videoEncoder.signalEndOfInputStream()
            }
            while (true) {
                val outIndex = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 5_000)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (videoTrackIndex == -1) {
                            videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                            if (videoTrackIndex != -1 && audioTrackIndex != -1 && !muxerStarted) {
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                    outIndex >= 0 -> {
                        val encoded = videoEncoder.getOutputBuffer(outIndex)
                        if (encoded != null && videoBufferInfo.size > 0 &&
                            (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        ) {
                            if (muxerStarted) {
                                encoded.position(videoBufferInfo.offset)
                                encoded.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                muxer.writeSampleData(videoTrackIndex, encoded, videoBufferInfo)
                            }
                        }
                        videoEncoder.releaseOutputBuffer(outIndex, false)
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            videoDone = true
                            return
                        }
                    }
                }
            }
        }

        fun feedAndDrainAudio() {
            if (!audioInputDone) {
                val inIndex = audioEncoder.dequeueInputBuffer(2_000)
                if (inIndex >= 0) {
                    val buf = audioEncoder.getInputBuffer(inIndex)!!
                    buf.clear()
                    val maxSamples = minOf(1024, totalAudioSamples - audioSampleCursor)
                    if (maxSamples <= 0) {
                        audioEncoder.queueInputBuffer(
                            inIndex,
                            0,
                            0,
                            (audioSampleCursor * 1_000_000L) / SAMPLE_RATE,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        audioInputDone = true
                    } else {
                        val ptsUs = (audioSampleCursor * 1_000_000L) / SAMPLE_RATE
                        for (i in 0 until maxSamples) {
                            val tSec = (audioSampleCursor + i).toDouble() / SAMPLE_RATE
                            val sample = synthesizeStudioSample(tSec)
                            buf.putShort(sample)
                        }
                        audioSampleCursor += maxSamples
                        audioEncoder.queueInputBuffer(inIndex, 0, maxSamples * 2, ptsUs, 0)
                    }
                }
            }

            while (true) {
                val outIndex = audioEncoder.dequeueOutputBuffer(audioBufferInfo, 2_000)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (audioTrackIndex == -1) {
                            audioTrackIndex = muxer.addTrack(audioEncoder.outputFormat)
                            if (videoTrackIndex != -1 && audioTrackIndex != -1 && !muxerStarted) {
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                    outIndex >= 0 -> {
                        val encoded = audioEncoder.getOutputBuffer(outIndex)
                        if (encoded != null && audioBufferInfo.size > 0 &&
                            (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        ) {
                            if (muxerStarted) {
                                encoded.position(audioBufferInfo.offset)
                                encoded.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                muxer.writeSampleData(audioTrackIndex, encoded, audioBufferInfo)
                            }
                        }
                        audioEncoder.releaseOutputBuffer(outIndex, false)
                        if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            audioDone = true
                            return
                        }
                    }
                }
            }
        }

        // Warm up formats first so muxer starts before dropping any frames
        while (!muxerStarted && (!videoDone || !audioDone)) {
            if (frameIdx < 2) {
                val canvas = inputSurface.lockCanvas(null)
                drawStudioFrame(canvas, frameIdx, totalFrames, chapters, paint, textPaint, subPaint)
                inputSurface.unlockCanvasAndPost(canvas)
                frameIdx++
            }
            drainVideo(false)
            feedAndDrainAudio()
        }

        while (!videoDone || !audioDone) {
            if (frameIdx < totalFrames) {
                val canvas = inputSurface.lockCanvas(null)
                drawStudioFrame(canvas, frameIdx, totalFrames, chapters, paint, textPaint, subPaint)
                inputSurface.unlockCanvasAndPost(canvas)
                frameIdx++
                drainVideo(frameIdx >= totalFrames)
            } else if (!videoDone) {
                drainVideo(true)
            }
            if (!audioDone) {
                feedAndDrainAudio()
            }
        }

        try {
            videoEncoder.stop()
            videoEncoder.release()
            audioEncoder.stop()
            audioEncoder.release()
            inputSurface.release()
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
        } catch (_: Exception) {
        }
    }

    private fun synthesizeStudioSample(tSec: Double): Short {
        // Deliberate silent pauses at 5.2-6.4s, 11.4-12.6s, 17.5-18.8s for real Silence Detection!
        val isPause = (tSec in 5.2..6.4) || (tSec in 11.4..12.6) || (tSec in 17.5..18.8)
        if (isPause) return 0

        val syllableMod = 0.55 + 0.45 * sin(2.0 * PI * 3.8 * tSec)
        val fundamental = 155.0 + 35.0 * sin(2.0 * PI * 0.7 * tSec)
        val wave = 0.55 * sin(2.0 * PI * fundamental * tSec) +
            0.25 * sin(2.0 * PI * fundamental * 2.0 * tSec) +
            0.12 * sin(2.0 * PI * 420.0 * tSec)
        val amplitude = (wave * syllableMod * 9000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        return amplitude.toShort()
    }

    private fun drawStudioFrame(
        canvas: Canvas,
        frameIdx: Int,
        totalFrames: Int,
        chapters: List<String>,
        paint: Paint,
        textPaint: Paint,
        subPaint: Paint
    ) {
        val tSec = frameIdx.toFloat() / FPS.toFloat()
        val chapterIndex = ((tSec / 6f).toInt()).coerceIn(0, chapters.lastIndex)

        // Rich dark studio gradient backdrop
        paint.shader = LinearGradient(
            0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
            intArrayOf(
                Color.parseColor("#0B0E17"),
                Color.parseColor("#13182B"),
                Color.parseColor("#0D101B")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
        paint.shader = null

        // Subtle studio grid lines
        paint.color = Color.argb(24, 148, 163, 184)
        paint.strokeWidth = 1f
        for (x in 0..WIDTH step 80) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), HEIGHT.toFloat(), paint)
        }
        for (y in 0..HEIGHT step 80) {
            canvas.drawLine(0f, y.toFloat(), WIDTH.toFloat(), y.toFloat(), paint)
        }

        // Studio badge card
        paint.color = Color.argb(210, 23, 27, 38)
        val card = RectF(72f, 64f, WIDTH - 72f, HEIGHT - 84f)
        canvas.drawRoundRect(card, 28f, 28f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(120, 124, 92, 255)
        canvas.drawRoundRect(card, 28f, 28f, paint)
        paint.style = Paint.Style.FILL

        // Speaker silhouette / studio avatar circle in center
        val cx = WIDTH / 2f
        val cy = HEIGHT * 0.43f
        val pulse = 1f + 0.08f * abs(sin(tSec * 5f))
        paint.color = Color.argb(45, 124, 92, 255)
        canvas.drawCircle(cx, cy, 86f * pulse, paint)
        paint.color = Color.parseColor("#7C5CFF")
        canvas.drawCircle(cx, cy, 56f, paint)
        // Simple mic/speaker geometric icon
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(cx - 12f, cy - 26f, cx + 12f, cy + 6f), 12f, 12f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawArc(RectF(cx - 22f, cy - 12f, cx + 22f, cy + 20f), 0f, 180f, false, paint)
        canvas.drawLine(cx, cy + 20f, cx, cy + 32f, paint)
        paint.style = Paint.Style.FILL

        // Header label
        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#38BDF8")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("CLIPFORGE STUDIO SAMPLE PODCAST", cx, 118f, textPaint)

        // Active Chapter Title
        textPaint.textSize = 30f
        textPaint.color = Color.WHITE
        canvas.drawText(chapters[chapterIndex], cx, HEIGHT * 0.67f, textPaint)

        // Audio Waveform Bars (reflect real pauses!)
        val isPause = (tSec in 5.2f..6.4f) || (tSec in 11.4f..12.6f) || (tSec in 17.5f..18.8f)
        val barCount = 32
        val barWidth = 10f
        val gap = 8f
        val totalWaveW = barCount * (barWidth + gap)
        val startX = cx - totalWaveW / 2f
        val waveBaseY = HEIGHT * 0.78f
        for (i in 0 until barCount) {
            val h = if (isPause) {
                4f
            } else {
                8f + 36f * abs(sin(tSec * 7f + i * 0.55f))
            }
            paint.color = if (i % 2 == 0) Color.parseColor("#7C5CFF") else Color.parseColor("#38BDF8")
            val bx = startX + i * (barWidth + gap)
            canvas.drawRoundRect(RectF(bx, waveBaseY - h / 2f, bx + barWidth, waveBaseY + h / 2f), 5f, 5f, paint)
        }

        // Timecode footer
        subPaint.textSize = 18f
        subPaint.textAlign = Paint.Align.CENTER
        val secInt = tSec.toInt()
        val frac = ((tSec - secInt) * 24).toInt()
        val statusLabel = if (isPause) "SILENT PAUSE DETECTABLE" else "ACTIVE SPEECH CADENCE"
        canvas.drawText(
            "00:%02d:%02d  •  %s".format(secInt, frac, statusLabel),
            cx,
            HEIGHT - 105f,
            subPaint
        )
    }
}
