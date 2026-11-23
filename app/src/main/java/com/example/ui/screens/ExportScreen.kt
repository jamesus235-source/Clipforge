package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.domain.model.ClipProject
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportResolution
import com.example.media.VideoExportEngine
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.RoseError
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ExportUiState
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun ExportScreen(
    project: ClipProject?,
    exportState: ExportUiState,
    onUpdateConfig: ((ExportConfig) -> ExportConfig) -> Unit,
    onExportSingleClip: (String) -> Unit,
    onExportAllAndZip: () -> Unit,
    onCancelExport: () -> Unit,
    onSaveToGallery: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewExportedPath by remember { mutableStateOf<String?>(null) }

    if (project == null || project.clips.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Movie, contentDescription = "No clips to export", tint = ElectricBlue, modifier = Modifier.size(48.dp))
                Text("Nothing Ready to Export", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    "Generate clips from a video first, then customize and render MP4s here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Create Clips")
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Export MP4 & ZIP",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Render hardware-accelerated MP4 files with Media3 Transformer. Burned-in subtitles, crop framing, and color settings are baked directly into the exported video.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // Export Settings Card
            StudioSectionCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Output Format Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                    Text("Resolution (MP4 H.264)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportResolution.entries.forEach { res ->
                            FilterChip(
                                selected = exportState.config.resolution == res,
                                onClick = { onUpdateConfig { it.copy(resolution = res) } },
                                label = { Text("${res.label} (${res.shortSidePx}p)") },
                                modifier = Modifier.testTag("export_res_${res.name}")
                            )
                        }
                    }

                    Text("Frame Rate Target", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(30, 60).forEach { fps ->
                            FilterChip(
                                selected = exportState.config.fps == fps,
                                onClick = { onUpdateConfig { it.copy(fps = fps) } },
                                label = { Text("$fps FPS") },
                                modifier = Modifier.testTag("export_fps_$fps")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Burned-in Subtitles", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                text = if (exportState.config.burnSubtitles) "Bake styled captions into video frames" else "Export clean video without subtitles",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = exportState.config.burnSubtitles,
                            onCheckedChange = { burn -> onUpdateConfig { it.copy(burnSubtitles = burn) } },
                            modifier = Modifier.testTag("export_burn_subtitles_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("High-Quality AAC Audio", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(
                            checked = exportState.config.includeAudio,
                            onCheckedChange = { inc -> onUpdateConfig { it.copy(includeAudio = inc) } }
                        )
                    }
                }
            }

            // Real Rendering Progress Banner
            if (exportState.isExporting) {
                StudioSectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exportState.statusMessage ?: "Rendering...",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricBlue,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(exportState.progress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { exportState.progress.coerceIn(0f, 1f) },
                            color = ElectricViolet,
                            trackColor = ObsidianCanvas,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        OutlinedButton(
                            onClick = onCancelExport,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancel Export")
                        }
                    }
                }
            }

            if (exportState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseError.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = exportState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseError,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!exportState.isExporting && exportState.statusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldSuccess.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Export ready", tint = EmeraldSuccess)
                        Text(
                            text = exportState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Export All + ZIP Bundle CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onExportAllAndZip,
                    enabled = !exportState.isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("export_all_zip_button")
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = "Export all clips as ZIP", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export All + ZIP", fontWeight = FontWeight.Bold)
                }

                val zipPath = exportState.zipFilePath
                if (zipPath != null && File(zipPath).exists()) {
                    OutlinedButton(
                        onClick = {
                            val intent = VideoExportEngine.shareFileIntent(
                                context,
                                File(zipPath),
                                "application/zip"
                            )
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Clips ZIP"))
                        },
                        border = BorderStroke(1.dp, ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("download_zip_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Download ZIP", tint = ElectricBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("ZIP (${MainViewModel.formatBytes(exportState.zipFileSizeBytes)})", color = ElectricBlue)
                    }
                }
            }

            // Individual Clip Render & Download List
            Text(
                text = "Clips (${project.clips.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            project.clips.forEachIndexed { idx, clip ->
                val exportedFile = clip.exportedFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
                StudioSectionCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${clip.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${clip.framing.aspectRatio.label} • ${MainViewModel.formatTimestamp(clip.startMs)}–${MainViewModel.formatTimestamp(clip.endMs)} • ${if (exportState.config.burnSubtitles) "Subtitles On" else "Clean"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = { onExportSingleClip(clip.id) },
                                enabled = !exportState.isExporting,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianCanvas),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("render_clip_button_$idx")
                            ) {
                                Text(if (exportedFile == null) "Render MP4" else "Re-render", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (exportedFile != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianCanvas,
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Ready • ${clip.exportedResolution ?: "MP4"} • ${MainViewModel.formatBytes(exportedFile.length())}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = EmeraldSuccess
                                        )
                                        Text(
                                            text = exportedFile.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { previewExportedPath = exportedFile.absolutePath },
                                            modifier = Modifier.testTag("play_exported_clip_$idx")
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play exported MP4", tint = ElectricBlue)
                                        }
                                        IconButton(
                                            onClick = { onSaveToGallery(exportedFile.absolutePath) },
                                            modifier = Modifier.testTag("save_gallery_clip_$idx")
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = "Save MP4 to Movies", tint = EmeraldSuccess)
                                        }
                                        IconButton(
                                            onClick = {
                                                val intent = VideoExportEngine.shareFileIntent(context, exportedFile, "video/mp4")
                                                context.startActivity(android.content.Intent.createChooser(intent, "Share MP4 Clip"))
                                            },
                                            modifier = Modifier.testTag("share_clip_$idx")
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = "Share MP4", tint = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Exported MP4 Playback Dialog
    val pathToPlay = previewExportedPath
    if (pathToPlay != null) {
        ExportedMp4PlayerDialog(
            filePath = pathToPlay,
            onDismiss = { previewExportedPath = null }
        )
    }
}

@Composable
private fun ExportedMp4PlayerDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rendered MP4 Preview", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close preview")
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
