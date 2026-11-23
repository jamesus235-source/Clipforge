package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.domain.model.AspectRatioOption
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportResolution
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SilenceSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.math.abs

data class ExportedClipResult(
    val clipId: String,
    val outputFile: File,
    val fileSizeBytes: Long,
    val resolutionLabel: String,
    val fps: Int,
    val hasBurnedSubtitles: Boolean
)

/**
 * Custom PCM AudioProcessor applying linear gain scaling (0.0f..1.5f) for real volume adjustment
 * in Media3 Transformer and ExoPlayer.
 */
@UnstableApi
class VolumeGainAudioProcessor(private val gain: Float) : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (abs(gain - 1f) < 0.01f) {
            AudioProcessor.AudioFormat.NOT_SET
        } else {
            inputAudioFormat
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short.toFloat() * gain
            val clamped = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            out.putShort(clamped)
        }
        out.flip()
    }
}

@UnstableApi
object VideoExportEngine {

    suspend fun exportClip(
        context: Context,
        sourcePath: String,
        clip: GeneratedClip,
        detectedSilences: List<SilenceSpan>,
        config: ExportConfig,
        onProgress: (Float) -> Unit
    ): Result<ExportedClipResult> = withContext(Dispatchers.Main) {
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = clip.title
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
            .take(32)
            .ifBlank { "clip" }
        val outFile = File(exportsDir, "${safeName}_${clip.id.take(6)}_${config.resolution.label}.mp4")
        if (outFile.exists()) outFile.delete()

        val (targetW, targetH) = computeOutputDimensions(clip.framing.aspectRatio, config.resolution)
        val renderer = SubtitleBitmapRenderer(
            context = context,
            outputWidth = targetW,
            outputHeight = targetH,
            clip = clip,
            burnSubtitles = config.burnSubtitles
        )

        val videoEffects = buildList<Effect> {
            val zoom = clip.framing.zoom.coerceIn(1f, 2.5f)
            if (zoom > 1.01f) {
                add(ScaleAndRotateTransformation.Builder().setScale(zoom, zoom).build())
            }
            add(
                Presentation.createForWidthAndHeight(
                    targetW,
                    targetH,
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                )
            )
            val b = clip.framing.brightness.coerceIn(-0.6f, 0.6f)
            if (abs(b) > 0.01f) {
                add(Brightness(b))
            }
            val c = (clip.framing.contrast - 1f).coerceIn(-0.8f, 0.8f)
            if (abs(c) > 0.01f) {
                add(Contrast(c))
            }

            val needsOverlay = config.burnSubtitles ||
                clip.framing.openingTitle.isNotBlank() ||
                clip.framing.closingText.isNotBlank() ||
                clip.framing.watermarkText.isNotBlank() ||
                clip.framing.watermarkImageUri != null

            if (needsOverlay) {
                val dynamicOverlay = object : BitmapOverlay() {
                    override fun getBitmap(presentationTimeUs: Long): Bitmap {
                        return renderer.renderFrameOverlay(presentationTimeUs / 1000L)
                    }
                }
                add(OverlayEffect(listOf(dynamicOverlay)))
            }
        }

        val audioProcessors = buildList<AudioProcessor> {
            val speed = clip.framing.speed.coerceIn(0.5f, 2.0f)
            if (abs(speed - 1f) > 0.01f) {
                add(SonicAudioProcessor().apply { setSpeed(speed) })
            }
            val vol = clip.framing.volume.coerceIn(0f, 1.5f)
            if (abs(vol - 1f) > 0.01f) {
                add(VolumeGainAudioProcessor(vol))
            }
        }

        val effects = Effects(audioProcessors, videoEffects)
        val sourceUri = Uri.fromFile(File(sourcePath))

        // Build segments (optionally cutting out detected silences inside the clip range)
        val segments = buildNonSilentSegments(
            clipStartMs = clip.startMs,
            clipEndMs = clip.endMs,
            silences = if (clip.framing.removeSilences) detectedSilences else emptyList()
        )

        val editedItems = segments.map { (segStart, segEnd) ->
            val item = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(segStart)
                        .setEndPositionMs(segEnd)
                        .build()
                )
                .build()
            EditedMediaItem.Builder(item)
                .setRemoveAudio(!config.includeAudio)
                .setEffects(effects)
                .build()
        }

        val mainSequence = EditedMediaItemSequence.Builder(editedItems).build()
        val composition = Composition.Builder(listOf(mainSequence)).build()

        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val progressHolder = ProgressHolder()
            var finished = false

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                        if (finished) return
                        finished = true
                        onProgress(1f)
                        if (outFile.exists() && outFile.length() > 0L) {
                            cont.resume(
                                Result.success(
                                    ExportedClipResult(
                                        clipId = clip.id,
                                        outputFile = outFile,
                                        fileSizeBytes = outFile.length(),
                                        resolutionLabel = "${targetW}×${targetH}",
                                        fps = config.fps,
                                        hasBurnedSubtitles = config.burnSubtitles
                                    )
                                )
                            )
                        } else {
                            cont.resume(Result.failure(IllegalStateException("Rendered MP4 file was empty.")))
                        }
                    }

                    override fun onError(
                        comp: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (finished) return
                        finished = true
                        cont.resume(Result.failure(exportException))
                    }
                })
                .build()

            val pollRunnable = object : Runnable {
                override fun run() {
                    if (finished) return
                    val state = transformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress((progressHolder.progress / 100f).coerceIn(0f, 0.99f))
                    }
                    if (!finished) {
                        handler.postDelayed(this, 200L)
                    }
                }
            }

            cont.invokeOnCancellation {
                finished = true
                handler.removeCallbacks(pollRunnable)
                transformer.cancel()
            }

            transformer.start(composition, outFile.absolutePath)
            handler.post(pollRunnable)
        }
    }

    suspend fun createZipBundle(context: Context, files: List<File>, zipName: String): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
                val safeZipName = zipName.replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifBlank { "clipforge_clips" }
                val zipFile = File(exportsDir, "${safeZipName}_clips.zip")
                if (zipFile.exists()) zipFile.delete()

                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    val buffer = ByteArray(16 * 1024)
                    files.filter { it.exists() && it.length() > 0L }.forEach { file ->
                        BufferedInputStream(FileInputStream(file)).use { bis ->
                            zos.putNextEntry(ZipEntry(file.name))
                            while (true) {
                                val read = bis.read(buffer)
                                if (read <= 0) break
                                zos.write(buffer, 0, read)
                            }
                            zos.closeEntry()
                        }
                    }
                }
                Result.success(zipFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun saveVideoToGallery(context: Context, file: File): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ClipForgeAI")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not create MediaStore entry."))

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                Result.success(uri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun shareFileIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun computeOutputDimensions(aspectRatio: AspectRatioOption, res: ExportResolution): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatioOption.VERTICAL_9_16 -> res.shortSidePx to res.longSidePx
            AspectRatioOption.LANDSCAPE_16_9 -> res.longSidePx to res.shortSidePx
            AspectRatioOption.SQUARE_1_1 -> res.shortSidePx to res.shortSidePx
        }
    }

    private fun buildNonSilentSegments(
        clipStartMs: Long,
        clipEndMs: Long,
        silences: List<SilenceSpan>
    ): List<Pair<Long, Long>> {
        if (silences.isEmpty()) return listOf(clipStartMs to clipEndMs)
        val sorted = silences
            .filter { it.endMs > clipStartMs && it.startMs < clipEndMs }
            .sortedBy { it.startMs }
        if (sorted.isEmpty()) return listOf(clipStartMs to clipEndMs)

        val segments = mutableListOf<Pair<Long, Long>>()
        var cursor = clipStartMs
        for (span in sorted) {
            val sStart = span.startMs.coerceIn(clipStartMs, clipEndMs)
            val sEnd = span.endMs.coerceIn(clipStartMs, clipEndMs)
            if (sStart - cursor >= 400L) {
                segments.add(cursor to sStart)
            }
            if (sEnd > cursor) cursor = sEnd
        }
        if (clipEndMs - cursor >= 400L) {
            segments.add(cursor to clipEndMs)
        }
        return if (segments.isEmpty()) listOf(clipStartMs to clipEndMs) else segments
    }
}
