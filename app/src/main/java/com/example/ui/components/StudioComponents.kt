package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.BackgroundMode
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SilenceSpan
import com.example.domain.model.SubtitleAlignment
import com.example.domain.model.SubtitleAnimation
import com.example.domain.model.SubtitlePosition
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SubtitleFonts
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ClipForgeLogoBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ElectricViolet, ElectricBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCut,
                contentDescription = "ClipForge AI logo",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = "ClipForge AI",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "VIDEO STUDIO",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricBlue,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun AiFirebaseNoticeBanner(
    noticeText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AmberWarning.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "AI mode notice",
                tint = AmberWarning,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = noticeText,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun ScoreEstimatePill(
    score: Int,
    modifier: Modifier = Modifier
) {
    val tint = if (score >= 85) EmeraldSuccess else ElectricBlue
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Estimated engagement score",
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$score Est.",
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StudioSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SlateCard,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        content()
    }
}

/**
 * Live Media3 ExoPlayer stage with real-time aspect ratio framing, zoom/pan,
 * background fill, opening/closing title, watermark, and frame-synced animated subtitles.
 */
@Composable
fun LiveVideoPreviewStage(
    sourceVideoPath: String,
    clip: GeneratedClip,
    detectedSilences: List<SilenceSpan>,
    modifier: Modifier = Modifier,
    onPositionChanged: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val player = remember(sourceVideoPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentClipLocalMs by remember { mutableLongStateOf(0L) }

    // Update clipped MediaItem whenever clip start/end changes
    LaunchedEffect(sourceVideoPath, clip.id, clip.startMs, clip.endMs) {
        val file = File(sourceVideoPath)
        if (file.exists()) {
            val item = MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()
            player.setMediaItem(item)
            player.prepare()
        }
    }

    // Apply live playback speed & volume
    LaunchedEffect(clip.framing.speed, clip.framing.volume) {
        player.playbackParameters = PlaybackParameters(clip.framing.speed.coerceIn(0.5f, 2.0f))
        player.volume = clip.framing.volume.coerceIn(0f, 1.0f)
    }

    // Track live playhead & auto-skip detected silences if enabled
    LaunchedEffect(player, clip.id, clip.framing.removeSilences, detectedSilences) {
        while (true) {
            isPlaying = player.isPlaying
            val pos = player.currentPosition.coerceAtLeast(0L)
            currentClipLocalMs = pos
            onPositionChanged(pos)

            if (clip.framing.removeSilences && player.isPlaying) {
                val absPos = clip.startMs + pos
                val inSilence = detectedSilences.firstOrNull {
                    absPos >= it.startMs && absPos < it.endMs - 120L
                }
                if (inSilence != null) {
                    val skipToLocal = (inSilence.endMs - clip.startMs).coerceIn(0L, clip.durationMs)
                    player.seekTo(skipToLocal)
                }
            }
            delay(80L)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    val framing = clip.framing
    val style = clip.subtitleStyle
    val fontOption = remember(style.fontId) { SubtitleFonts.byId(style.fontId) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ObsidianCanvas)
                .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .height(330.dp)
                    .aspectRatio(framing.aspectRatio.ratio)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCanvas)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
            ) {
                // Background layer for vertical / framed videos
                when (framing.backgroundMode) {
                    BackgroundMode.SOLID_DARK -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B0D14))
                    )
                    BackgroundMode.GRADIENT_INDIGO -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF15192E), Color(0xFF090B10), Color(0xFF1A1436))
                                )
                            )
                    )
                    BackgroundMode.MESH_STUDIO -> Image(
                        painter = painterResource(id = R.drawable.img_bg_mesh_indigo),
                        contentDescription = "Studio background mesh",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    BackgroundMode.CUSTOM_IMAGE -> {
                        if (framing.customBackgroundUri != null) {
                            AsyncImage(
                                model = framing.customBackgroundUri,
                                contentDescription = "Custom video background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(CharcoalPanel)
                            )
                        }
                    }
                }

                // Live Video Surface with framing zoom/pan & color matrix preview
                val colorMatrix = remember(framing.brightness, framing.contrast) {
                    val c = framing.contrast
                    val b = framing.brightness * 255f
                    val translate = (-.5f * c + .5f) * 255f + b
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, translate,
                            0f, c, 0f, 0f, translate,
                            0f, 0f, c, 0f, translate,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.player = player
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = framing.zoom
                            scaleY = framing.zoom
                            translationX = framing.panX * 90f
                            translationY = framing.panY * 90f
                        }
                )

                // Subtle color grade overlay approximating brightness/contrast shift cleanly over SurfaceView
                val brightnessAlpha = kotlin.math.abs(framing.brightness).coerceIn(0f, 0.35f)
                if (brightnessAlpha > 0.01f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (framing.brightness >= 0f) Color.White.copy(alpha = brightnessAlpha)
                                else Color.Black.copy(alpha = brightnessAlpha)
                            )
                    )
                }
                val _unused = ColorFilter.colorMatrix(colorMatrix)

                // Watermark / Logo Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (framing.watermarkImageUri != null) {
                        AsyncImage(
                            model = framing.watermarkImageUri,
                            contentDescription = "Creator watermark logo",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (framing.watermarkText.isNotBlank()) {
                        Text(
                            text = framing.watermarkText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Opening Title / Closing Text
                androidx.compose.animation.AnimatedVisibility(
                    visible = framing.openingTitle.isNotBlank() && currentClipLocalMs <= 2500L,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp, start = 14.dp, end = 14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricViolet.copy(alpha = 0.88f)
                    ) {
                        Text(
                            text = framing.openingTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = framing.closingText.isNotBlank() &&
                        currentClipLocalMs >= (clip.durationMs - 2500L).coerceAtLeast(0L),
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp, start = 14.dp, end = 14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricBlue.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = framing.closingText,
                            style = MaterialTheme.typography.titleSmall,
                            color = ObsidianCanvas,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Synced Live Subtitle Overlay
                val adjustedPos = currentClipLocalMs - style.timingOffsetMs
                val activeSegment = clip.transcriptSegments.firstOrNull {
                    adjustedPos >= it.startMs && adjustedPos <= it.endMs
                } ?: clip.transcriptSegments.firstOrNull()

                if (activeSegment != null) {
                    val words = activeSegment.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    val wrapped = words.chunked(style.wordsPerLine.coerceIn(1, 8))
                        .joinToString("\n") { it.joinToString(" ") }
                    val segDuration = (activeSegment.endMs - activeSegment.startMs).coerceAtLeast(1L)
                    val elapsed = (adjustedPos - activeSegment.startMs).coerceAtLeast(0L)
                    val karaokeIdx = ((elapsed.toFloat() / segDuration.toFloat()) * words.size)
                        .toInt().coerceIn(0, (words.size - 1).coerceAtLeast(0))

                    val scaleAnim by animateFloatAsState(
                        targetValue = if (style.animation == SubtitleAnimation.POP && elapsed < 140L) 1.08f else 1.0f,
                        animationSpec = tween(120),
                        label = "subtitlePop"
                    )

                    val boxAlignment = when (style.position) {
                        SubtitlePosition.TOP -> Alignment.TopCenter
                        SubtitlePosition.MIDDLE -> Alignment.Center
                        SubtitlePosition.BOTTOM -> Alignment.BottomCenter
                    }
                    val padVertical = when (style.position) {
                        SubtitlePosition.TOP -> 36.dp
                        SubtitlePosition.MIDDLE -> 0.dp
                        SubtitlePosition.BOTTOM -> 28.dp
                    }

                    Box(
                        modifier = Modifier
                            .align(boxAlignment)
                            .padding(horizontal = 14.dp, vertical = padVertical)
                            .graphicsLayer {
                                scaleX = scaleAnim
                                scaleY = scaleAnim
                            }
                            .offset {
                                if (style.animation == SubtitleAnimation.SLIDE_UP && elapsed < 140L) {
                                    IntOffset(0, 10)
                                } else IntOffset.Zero
                            }
                    ) {
                        val annotated = buildAnnotatedString {
                            val highlightColor = Color(style.highlightColorHex)
                            val baseColor = Color(style.fontColorHex)
                            val lines = wrapped.split("\n")
                            var wordCursor = 0
                            lines.forEachIndexed { lineIdx, line ->
                                val lineWords = line.split(" ")
                                lineWords.forEachIndexed { wIdx, w ->
                                    val isExplicit = activeSegment.highlightedWords.any {
                                        w.contains(it, ignoreCase = true)
                                    }
                                    val isKaraoke = style.animation == SubtitleAnimation.KARAOKE && wordCursor == karaokeIdx
                                    val useHighlight = style.highlightKeywords && (isExplicit || isKaraoke)
                                    withStyle(
                                        SpanStyle(
                                            color = if (useHighlight) highlightColor else baseColor,
                                            fontWeight = if (style.isBold) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    ) {
                                        append(w)
                                    }
                                    if (wIdx < lineWords.lastIndex) append(" ")
                                    wordCursor++
                                }
                                if (lineIdx < lines.lastIndex) append("\n")
                            }
                        }

                        val align = when (style.alignment) {
                            SubtitleAlignment.LEFT -> TextAlign.Start
                            SubtitleAlignment.CENTER -> TextAlign.Center
                            SubtitleAlignment.RIGHT -> TextAlign.End
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Color(style.backgroundColorHex).copy(alpha = style.backgroundOpacity)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = annotated,
                                fontFamily = fontOption.composeFontFamily,
                                fontSize = (style.fontSizeSp * 0.65f).coerceIn(11f, 24f).sp,
                                lineHeight = (style.fontSizeSp * 0.65f * style.lineSpacingMultiplier).sp,
                                textAlign = align,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    shadow = if (style.hasShadow || style.outlineThickness > 0f) {
                                        Shadow(
                                            color = Color(style.outlineColorHex),
                                            blurRadius = (style.outlineThickness + 3f).coerceAtLeast(2f)
                                        )
                                    } else null
                                )
                            )
                        }
                    }
                }

                // Play / Pause Floating Trigger
                IconButton(
                    onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ObsidianCanvas.copy(alpha = 0.72f))
                        .testTag("preview_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${framing.aspectRatio.label} • ${framing.speed}x • ${if (framing.autoSpeakerTracking) "Speaker Track On" else "Manual Frame"}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatMs(currentClipLocalMs)} / ${formatMs(clip.durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(s / 60L, s % 60L)
}
