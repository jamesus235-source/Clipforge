package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.SilenceSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.sqrt

data class ImportedVideoInfo(
    val localFile: File,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long
)

object VideoAnalyzer {

    private const val MAX_FILE_SIZE_BYTES = 500L * 1024L * 1024L // 500 MB safety cap

    suspend fun importPickedVideo(context: Context, uri: Uri): Result<ImportedVideoInfo> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var displayName = "Imported Video.mp4"
                var reportedSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                        if (sizeIdx >= 0) reportedSize = cursor.getLong(sizeIdx)
                    }
                }

                if (reportedSize > MAX_FILE_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Video exceeds the 500 MB local studio limit.")
                    )
                }

                val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
                val destFile = File(videosDir, "import_${UUID.randomUUID()}.mp4")
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            copied += read
                            if (copied > MAX_FILE_SIZE_BYTES) {
                                destFile.delete()
                                return@withContext Result.failure(
                                    IllegalArgumentException("Video exceeds the 500 MB limit.")
                                )
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to open selected video."))

                if (!destFile.exists() || destFile.length() == 0L) {
                    return@withContext Result.failure(IllegalStateException("Selected video file is empty."))
                }

                val meta = inspectVideoFile(destFile, displayName)
                Result.success(meta)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun inspectVideoFile(file: File, displayName: String = file.name): ImportedVideoInfo =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(1000L) ?: 10_000L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 1280
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 720
                ImportedVideoInfo(
                    localFile = file,
                    displayName = displayName,
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    fileSizeBytes = file.length()
                )
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun extractFrameThumbnail(filePath: String, timeMs: Long = 1000L): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

    /**
     * Scans actual decoded PCM audio energy windows using MediaExtractor + MediaCodec
     * to detect genuine low-energy silence spans (>= 500ms).
     */
    suspend fun detectSilences(
        filePath: String,
        rmsThreshold: Double = 320.0,
        minSilenceDurationMs: Long = 550L
    ): List<SilenceSpan> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val spans = mutableListOf<SilenceSpan>()

        try {
            extractor.setDataSource(filePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex == -1 || audioFormat == null) return@withContext emptyList()

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var silenceStartMs: Long? = null
            var lastPtsMs = 0L

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(3_000)
                    if (inIdx >= 0) {
                        val inputBuf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val ptsUs = extractor.sampleTime
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, ptsUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 3_000)
                if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortCount = bufferInfo.size / 2
                        if (shortCount > 0) {
                            var sumSq = 0.0
                            for (i in 0 until shortCount) {
                                val s = outBuf.short.toDouble()
                                sumSq += s * s
                            }
                            val rms = sqrt(sumSq / shortCount)
                            val ptsMs = bufferInfo.presentationTimeUs / 1000L
                            lastPtsMs = ptsMs

                            if (rms < rmsThreshold) {
                                if (silenceStartMs == null) silenceStartMs = ptsMs
                            } else {
                                val start = silenceStartMs
                                if (start != null && (ptsMs - start) >= minSilenceDurationMs) {
                                    spans.add(SilenceSpan(startMs = start, endMs = ptsMs))
                                }
                                silenceStartMs = null
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            val trailingStart = silenceStartMs
            if (trailingStart != null && (lastPtsMs - trailingStart) >= minSilenceDurationMs) {
                spans.add(SilenceSpan(startMs = trailingStart, endMs = lastPtsMs))
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }

        spans
    }
}
