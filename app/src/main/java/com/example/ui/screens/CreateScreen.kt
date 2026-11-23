package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.SourceType
import com.example.ui.components.AiFirebaseNoticeBanner
import com.example.ui.components.ClipForgeLogoBadge
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CreateUiState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiAsyncState
import java.io.File
import kotlin.math.roundToInt

@Composable
fun CreateScreen(
    state: CreateUiState,
    aiNotice: String,
    onYouTubeUrlChanged: (String) -> Unit,
    onFetchYouTubeMetadata: () -> Unit,
    onPickLocalVideo: (Uri) -> Unit,
    onLoadSampleVideo: () -> Unit,
    onClipCountChanged: (Int) -> Unit,
    onPreferredDurationChanged: (Int) -> Unit,
    onToggleManualRange: (Boolean) -> Unit,
    onManualRangeChanged: (Float, Float) -> Unit,
    onGenerateClips: () -> Unit,
    onOpenProjectsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickLocalVideo(uri)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClipForgeLogoBadge()
                OutlinedButton(
                    onClick = onOpenProjectsTab,
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("header_projects_button")
                ) {
                    Text("Projects", color = TextPrimary)
                }
            }

            // Hero Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF15192B), CharcoalPanel)
                        )
                    )
                    .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ElectricViolet.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI studio",
                                tint = ElectricViolet,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "AI Shorts Studio • On-Device MP4 Export",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricViolet
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.hero_headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary
                    )

                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )

                    AiFirebaseNoticeBanner(noticeText = aiNotice)
                }
            }

            // Step 1: YouTube URL Metadata & Compliant Video Source Card
            StudioSectionCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Video Source",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = state.youtubeUrlInput,
                        onValueChange = onYouTubeUrlChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_url_input"),
                        label = { Text("YouTube Video URL") },
                        placeholder = { Text(stringResource(R.string.youtube_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = "YouTube URL link",
                                tint = ElectricBlue
                            )
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = onFetchYouTubeMetadata,
                                enabled = !state.isFetchingYouTube && state.youtubeUrlInput.isNotBlank(),
                                modifier = Modifier.testTag("fetch_youtube_button")
                            ) {
                                if (state.isFetchingYouTube) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Lookup", color = ElectricBlue, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        },
                        isError = state.youtubeValidationError != null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = SubtleBorder,
                            focusedContainerColor = ObsidianCanvas,
                            unfocusedContainerColor = ObsidianCanvas
                        )
                    )

                    if (state.youtubeValidationError != null) {
                        Text(
                            text = state.youtubeValidationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseError
                        )
                    }

                    // Quick sample YouTube link chip for 1-tap testing
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Try link:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateElevated,
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier
                                .clickable {
                                    onYouTubeUrlChanged("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                                    onFetchYouTubeMetadata()
                                }
                                .testTag("sample_youtube_chip")
                        ) {
                            Text(
                                text = "youtube.com/watch?v=...",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Real YouTube oEmbed Metadata Card
                    AnimatedVisibility(visible = state.youtubeMetadata != null) {
                        state.youtubeMetadata?.let { meta ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCanvas,
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = meta.thumbnailUrl,
                                            contentDescription = meta.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .width(108.dp)
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SlateElevated)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = meta.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${meta.authorName} • ID: ${meta.videoId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "Resolution: ${meta.width}×${meta.height} (oEmbed verified)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EmeraldSuccess
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = "Compliant source policy",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "YouTube URLs supply verified metadata via oEmbed without bypassing stream protections. Select your authorized video file below or use the built-in Studio Sample Video for rendering.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Upload Video File (Zero-Permission Photo Picker) + Sample Studio Video
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateElevated,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("upload_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Upload video file",
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Video", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = onLoadSampleVideo,
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("load_sample_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = "Load sample studio video",
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sample Video", color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Active Playable Source Preview
                    val activePath = state.selectedVideoPath
                    if (activePath != null && File(activePath).exists()) {
                        SourceVideoPreviewCard(
                            videoPath = activePath,
                            displayName = state.selectedVideoName,
                            durationMs = state.selectedVideoDurationMs,
                            width = state.selectedVideoWidth,
                            height = state.selectedVideoHeight,
                            silenceCount = state.detectedSilences.size,
                            isSample = state.sourceType == SourceType.SAMPLE_STUDIO
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_studio),
                            contentDescription = "Studio preview placeholder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            // Step 2: Clipping Controls (1–10 clips, preferred duration, manual time range)
            StudioSectionCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Clipping Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manual Time Range",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Pick an exact slice instead of auto-splitting clips",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.useManualRange,
                            onCheckedChange = onToggleManualRange,
                            modifier = Modifier.testTag("manual_range_switch")
                        )
                    }

                    if (state.useManualRange) {
                        val totalMs = state.selectedVideoDurationMs.coerceAtLeast(10_000L)
                        val startMs = (state.manualStartFraction * totalMs).toLong()
                        val endMs = (state.manualEndFraction * totalMs).toLong()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Start: ${MainViewModel.formatTimestamp(startMs)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "End: ${MainViewModel.formatTimestamp(endMs)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricViolet
                                )
                            }
                            RangeSlider(
                                value = state.manualStartFraction..state.manualEndFraction,
                                onValueChange = { range ->
                                    onManualRangeChanged(range.start, range.endInclusive)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricViolet,
                                    activeTrackColor = ElectricViolet
                                ),
                                modifier = Modifier.testTag("manual_range_slider")
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Number of Clips",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${state.clipCount} clips",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricViolet,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.clipCount.toFloat(),
                                onValueChange = { onClipCountChanged(it.roundToInt()) },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricViolet,
                                    activeTrackColor = ElectricViolet
                                ),
                                modifier = Modifier.testTag("clip_count_slider")
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Preferred Clip Duration",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(20 to "20s Fast", 30 to "30s Sweet", 45 to "45s Story", 60 to "60s Deep").forEach { (sec, label) ->
                                    val selected = state.preferredDurationSec == sec
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onPreferredDurationChanged(sec) },
                                        label = { Text(label, maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("duration_chip_$sec")
                                    )
                                }
                            }
                        }
                    }

                    // Error / Loading state for Generation
                    when (val gen = state.generationState) {
                        is UiAsyncState.Loading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ElectricViolet
                                )
                                Text(
                                    text = gen.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricBlue
                                )
                            }
                        }
                        is UiAsyncState.Error -> {
                            Text(
                                text = gen.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseError
                            )
                        }
                        UiAsyncState.Idle -> Unit
                    }

                    Button(
                        onClick = onGenerateClips,
                        enabled = state.generationState !is UiAsyncState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("generate_clips_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Generate clips",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.generate_clips),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // How ClipForge Works + Feature Showcase
            HowItWorksSection()
            FeatureShowcaseSection()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SourceVideoPreviewCard(
    videoPath: String,
    displayName: String,
    durationMs: Long,
    width: Int,
    height: Int,
    silenceCount: Int,
    isSample: Boolean
) {
    val context = LocalContext.current
    val player = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            kotlinx.coroutines.delay(150L)
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCanvas,
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { if (player.isPlaying) player.pause() else player.play() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ObsidianCanvas.copy(alpha = 0.78f))
                        .testTag("source_preview_play_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause sample video" else "Play sample video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Video ready",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${MainViewModel.formatTimestamp(durationMs)} • ${width}×${height} • $silenceCount pauses scanned",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                if (isSample) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricViolet.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "SAMPLE",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricViolet,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksSection() {
    StudioSectionCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "How ClipForge AI Works",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            StepItem("01", "Paste Link or Upload", "Fetch YouTube oEmbed metadata or pick an authorized video directly on-device.")
            StepItem("02", "Scan Pauses & Moments", "Audio RMS silence detection maps natural speech gaps and builds distinct clips.")
            StepItem("03", "Style & Export MP4", "Customize 9:16 framing, 11 bundled fonts, overlays, and export burned-in MP4 + ZIP.")
        }
    }
}

@Composable
private fun StepItem(num: String, title: String, subtitle: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ElectricViolet.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f))
        ) {
            Text(
                text = num,
                style = MaterialTheme.typography.labelLarge,
                color = ElectricViolet,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun FeatureShowcaseSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeatureMiniCard(
            icon = Icons.Filled.ContentCut,
            title = "Non-Overlapping Clip Engine",
            body = "Generate 1–10 clips or dial in an exact manual timeline slice with silence skipping."
        )
        FeatureMiniCard(
            icon = Icons.Filled.ClosedCaption,
            title = "11 Fonts & 6 Caption Presets",
            body = "Creator Bold, Clean Minimal, Gaming, Podcast, Cinematic & Highlight Words with manual timing."
        )
        FeatureMiniCard(
            icon = Icons.Filled.Tune,
            title = "Hardware-Accelerated Media3 Export",
            body = "Render real 720p/1080p MP4s with burned-in subtitles, color grading, and ZIP bundles."
        )
    }
}

@Composable
private fun FeatureMiniCard(icon: ImageVector, title: String, body: String) {
    StudioSectionCard {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = ElectricBlue, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
