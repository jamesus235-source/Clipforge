package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AspectRatioOption
import com.example.domain.model.BackgroundMode
import com.example.domain.model.BrandPreset
import com.example.domain.model.CaptionPreset
import com.example.domain.model.ClipProject
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SubtitleAlignment
import com.example.domain.model.SubtitleAnimation
import com.example.domain.model.SubtitlePosition
import com.example.domain.model.SubtitleStyle
import com.example.domain.model.TransitionStyle
import com.example.domain.model.VideoFramingConfig
import com.example.ui.components.AiFirebaseNoticeBanner
import com.example.ui.components.ScoreEstimatePill
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SubtitleFonts
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun ClipsAndTranscriptPanel(
    project: ClipProject,
    clip: GeneratedClip,
    onUpdateClipRange: (Long, Long) -> Unit,
    onToggleRemoveSilences: (Boolean) -> Unit,
    onUpdateSegmentText: (String, String) -> Unit,
    onUpdateSegmentTiming: (String, Long, Long) -> Unit,
    onAddSubtitleSegment: (String) -> Unit
) {
    val totalDur = project.sourceDurationMs.coerceAtLeast(5_000L)
    var newLineText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Trim Timeline Range
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Clip Trim Window", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        text = "${MainViewModel.formatTimestamp(clip.startMs)} – ${MainViewModel.formatTimestamp(clip.endMs)} (${clip.durationMs / 1000}s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue
                    )
                }
                val startFrac = (clip.startMs.toFloat() / totalDur).coerceIn(0f, 0.95f)
                val endFrac = (clip.endMs.toFloat() / totalDur).coerceIn(startFrac + 0.04f, 1f)
                RangeSlider(
                    value = startFrac..endFrac,
                    onValueChange = { r ->
                        val s = (r.start * totalDur).toLong()
                        val e = (r.endInclusive * totalDur).toLong().coerceAtLeast(s + 2_000L)
                        onUpdateClipRange(s, e.coerceAtMost(totalDur))
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("editor_trim_slider")
                )
            }
        }

        // Real PCM Silence Detection Card
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Silence detection",
                            tint = ElectricBlue
                        )
                        Column {
                            Text("Silence Detection", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "${project.detectedSilences.size} pauses detected in audio track",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = clip.framing.removeSilences,
                        onCheckedChange = onToggleRemoveSilences,
                        modifier = Modifier.testTag("remove_silences_switch")
                    )
                }
                if (project.detectedSilences.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        project.detectedSilences.forEach { span ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SlateElevated
                            ) {
                                Text(
                                    text = "${MainViewModel.formatTimestamp(span.startMs)}–${MainViewModel.formatTimestamp(span.endMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manual Subtitle Text & Timing Editor
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Edit Subtitles & Timing", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

                clip.transcriptSegments.forEachIndexed { idx, seg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCanvas,
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cue #${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricViolet
                                )
                                Text(
                                    text = "${MainViewModel.formatTimestamp(seg.startMs)} → ${MainViewModel.formatTimestamp(seg.endMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            OutlinedTextField(
                                value = seg.text,
                                onValueChange = { onUpdateSegmentText(seg.id, it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("subtitle_text_field_$idx"),
                                singleLine = false,
                                maxLines = 2
                            )
                            val dur = clip.durationMs.coerceAtLeast(1000L)
                            val sf = (seg.startMs.toFloat() / dur).coerceIn(0f, 0.95f)
                            val ef = (seg.endMs.toFloat() / dur).coerceIn(sf + 0.03f, 1f)
                            RangeSlider(
                                value = sf..ef,
                                onValueChange = { r ->
                                    onUpdateSegmentTiming(
                                        seg.id,
                                        (r.start * dur).toLong(),
                                        (r.endInclusive * dur).toLong()
                                    )
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newLineText,
                        onValueChange = { newLineText = it },
                        placeholder = { Text("Add custom subtitle line...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_subtitle_input")
                    )
                    IconButton(
                        onClick = {
                            if (newLineText.isNotBlank()) {
                                onAddSubtitleSegment(newLineText)
                                newLineText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricViolet)
                            .testTag("add_subtitle_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add subtitle line", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SubtitleCustomizationPanel(
    style: SubtitleStyle,
    brandPresets: List<BrandPreset>,
    onApplyPreset: (CaptionPreset) -> Unit,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onSaveBrandPreset: (String) -> Unit,
    onApplyBrandPreset: (BrandPreset) -> Unit
) {
    var showBrandDialog by remember { mutableStateOf(false) }
    var brandName by remember { mutableStateOf("") }

    val colorSwatches = listOf(
        0xFFFFFFFF to "White",
        0xFFFACC15 to "Yellow",
        0xFF38BDF8 to "Blue",
        0xFF7C5CFF to "Violet",
        0xFF10B981 to "Emerald",
        0xFF090B10 to "Obsidian",
        0xFF000000 to "Black"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Ready-made Presets + Brand Presets
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
                    Text("Caption Presets", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    TextButton(
                        onClick = { showBrandDialog = true },
                        modifier = Modifier.testTag("save_brand_preset_button")
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = "Save brand preset", tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Brand", color = ElectricBlue)
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CaptionPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = style.preset == preset,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("caption_preset_${preset.name}")
                        )
                    }
                }

                if (brandPresets.isNotEmpty()) {
                    Text("Your Brand Presets", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        brandPresets.forEach { bp ->
                            FilterChip(
                                selected = false,
                                onClick = { onApplyBrandPreset(bp) },
                                label = { Text(bp.name) }
                            )
                        }
                    }
                }
            }
        }

        // 11 Font Options + Weight + Size
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Font Family (11 Bundled Fonts)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitleFonts.all.forEach { font ->
                        val selected = style.fontId == font.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) ElectricViolet.copy(alpha = 0.25f) else ObsidianCanvas,
                            border = BorderStroke(1.dp, if (selected) ElectricViolet else SubtleBorder),
                            modifier = Modifier
                                .clickable { onUpdateStyle { it.copy(fontId = font.id) } }
                                .testTag("font_option_${font.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = font.displayName,
                                    fontFamily = font.composeFontFamily,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = font.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                LabeledSlider("Font Size", "${style.fontSizeSp.roundToInt()} sp", style.fontSizeSp, 14f..40f) { v ->
                    onUpdateStyle { it.copy(fontSizeSp = v) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bold Weight", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = style.isBold, onCheckedChange = { b -> onUpdateStyle { it.copy(isBold = b) } })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Drop Shadow", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = style.hasShadow, onCheckedChange = { s -> onUpdateStyle { it.copy(hasShadow = s) } })
                }
            }
        }

        // Colors, Outline, Background Opacity
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorRowSelector("Text Color", colorSwatches, style.fontColorHex) { hex ->
                    onUpdateStyle { it.copy(fontColorHex = hex) }
                }
                ColorRowSelector("Background Box Color", colorSwatches, style.backgroundColorHex) { hex ->
                    onUpdateStyle { it.copy(backgroundColorHex = hex) }
                }
                LabeledSlider("Background Opacity", "${(style.backgroundOpacity * 100).roundToInt()}%", style.backgroundOpacity, 0f..1f) { v ->
                    onUpdateStyle { it.copy(backgroundOpacity = v) }
                }
                ColorRowSelector("Outline Color", colorSwatches, style.outlineColorHex) { hex ->
                    onUpdateStyle { it.copy(outlineColorHex = hex) }
                }
                LabeledSlider("Outline Thickness", "${style.outlineThickness.roundToInt()} px", style.outlineThickness, 0f..10f) { v ->
                    onUpdateStyle { it.copy(outlineThickness = v) }
                }
            }
        }

        // Layout, Highlighting, Animation, Timing
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Alignment & Position", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtitleAlignment.entries.forEach { align ->
                        FilterChip(
                            selected = style.alignment == align,
                            onClick = { onUpdateStyle { it.copy(alignment = align) } },
                            label = { Text(align.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtitlePosition.entries.forEach { pos ->
                        FilterChip(
                            selected = style.position == pos,
                            onClick = { onUpdateStyle { it.copy(position = pos) } },
                            label = { Text(pos.label) }
                        )
                    }
                }

                LabeledSlider("Words Per Line", "${style.wordsPerLine}", style.wordsPerLine.toFloat(), 1f..8f) { v ->
                    onUpdateStyle { it.copy(wordsPerLine = v.roundToInt()) }
                }
                LabeledSlider("Line Spacing", String.format("%.2fx", style.lineSpacingMultiplier), style.lineSpacingMultiplier, 0.9f..1.6f) { v ->
                    onUpdateStyle { it.copy(lineSpacingMultiplier = v) }
                }
                LabeledSlider("Subtitle Timing Offset", "${style.timingOffsetMs} ms", style.timingOffsetMs.toFloat(), -1000f..1000f) { v ->
                    onUpdateStyle { it.copy(timingOffsetMs = v.roundToLongIfZero()) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Highlight Keywords", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(
                        checked = style.highlightKeywords,
                        onCheckedChange = { h -> onUpdateStyle { it.copy(highlightKeywords = h) } }
                    )
                }
                if (style.highlightKeywords) {
                    ColorRowSelector("Highlight Color", colorSwatches, style.highlightColorHex) { hex ->
                        onUpdateStyle { it.copy(highlightColorHex = hex) }
                    }
                }

                Text("Animation Style", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitleAnimation.entries.forEach { anim ->
                        FilterChip(
                            selected = style.animation == anim,
                            onClick = { onUpdateStyle { it.copy(animation = anim) } },
                            label = { Text(anim.label) }
                        )
                    }
                }
            }
        }
    }

    if (showBrandDialog) {
        AlertDialog(
            onDismissRequest = { showBrandDialog = false },
            title = { Text("Save Brand Preset") },
            text = {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g., Podcast Brand") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveBrandPreset(brandName)
                        brandName = ""
                        showBrandDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBrandDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun VideoCustomizationPanel(
    framing: VideoFramingConfig,
    onUpdateFraming: ((VideoFramingConfig) -> VideoFramingConfig) -> Unit,
    onPickBackgroundMusic: (Uri) -> Unit
) {
    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateFraming {
                it.copy(
                    backgroundMode = BackgroundMode.CUSTOM_IMAGE,
                    customBackgroundUri = uri.toString()
                )
            }
        }
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateFraming { it.copy(watermarkImageUri = uri.toString()) }
        }
    }

    val musicPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onPickBackgroundMusic(uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Aspect Ratio & Crop / Speaker Tracking
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Aspect Ratio & Framing", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        FilterChip(
                            selected = framing.aspectRatio == ratio,
                            onClick = { onUpdateFraming { it.copy(aspectRatio = ratio) } },
                            label = { Text(ratio.label) },
                            modifier = Modifier.testTag("aspect_ratio_${ratio.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Speaker Tracking", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Centers active speaker framing in 9:16", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = framing.autoSpeakerTracking,
                        onCheckedChange = { enabled ->
                            onUpdateFraming {
                                it.copy(
                                    autoSpeakerTracking = enabled,
                                    panX = if (enabled) 0f else it.panX,
                                    panY = if (enabled) 0f else it.panY
                                )
                            }
                        }
                    )
                }

                LabeledSlider("Crop Zoom", String.format("%.2fx", framing.zoom), framing.zoom, 1f..2.2f) { z ->
                    onUpdateFraming { it.copy(zoom = z) }
                }
                LabeledSlider("Horizontal Offset", String.format("%.2f", framing.panX), framing.panX, -1f..1f) { px ->
                    onUpdateFraming { it.copy(panX = px, autoSpeakerTracking = false) }
                }
                LabeledSlider("Vertical Offset", String.format("%.2f", framing.panY), framing.panY, -1f..1f) { py ->
                    onUpdateFraming { it.copy(panY = py, autoSpeakerTracking = false) }
                }
            }
        }

        // Background & Watermark
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Vertical Background & Branding", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundMode.entries.forEach { mode ->
                        FilterChip(
                            selected = framing.backgroundMode == mode,
                            onClick = {
                                if (mode == BackgroundMode.CUSTOM_IMAGE) {
                                    bgImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    onUpdateFraming { it.copy(backgroundMode = mode) }
                                }
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = framing.watermarkText,
                    onValueChange = { txt -> onUpdateFraming { it.copy(watermarkText = txt) } },
                    label = { Text("Creator Handle / Watermark Text") },
                    placeholder = { Text("@yourchannel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "Pick watermark logo", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (framing.watermarkImageUri == null) "Add Logo" else "Change Logo")
                    }
                    if (framing.watermarkImageUri != null) {
                        TextButton(onClick = { onUpdateFraming { it.copy(watermarkImageUri = null) } }) {
                            Text("Clear Logo")
                        }
                    }
                }
            }
        }

        // Color, Speed, Audio, Music, Transitions, Titles
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Color, Audio & Titles", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

                Text("Playback Speed", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        FilterChip(
                            selected = kotlin.math.abs(framing.speed - spd) < 0.02f,
                            onClick = { onUpdateFraming { it.copy(speed = spd) } },
                            label = { Text("${spd}x") }
                        )
                    }
                }

                LabeledSlider("Brightness", "${(framing.brightness * 100).roundToInt()}%", framing.brightness, -0.4f..0.4f) { b ->
                    onUpdateFraming { it.copy(brightness = b) }
                }
                LabeledSlider("Contrast", String.format("%.2fx", framing.contrast), framing.contrast, 0.6f..1.6f) { c ->
                    onUpdateFraming { it.copy(contrast = c) }
                }
                LabeledSlider("Audio Volume", "${(framing.volume * 100).roundToInt()}%", framing.volume, 0f..1.5f) { v ->
                    onUpdateFraming { it.copy(volume = v) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { musicPicker.launch("audio/*") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Pick background music", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(framing.backgroundMusicName ?: "Add Background Music", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (framing.backgroundMusicUri != null) {
                        TextButton(onClick = { onUpdateFraming { it.copy(backgroundMusicUri = null, backgroundMusicName = null) } }) {
                            Text("Remove")
                        }
                    }
                }

                Text("Transition Style", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransitionStyle.entries.forEach { t ->
                        FilterChip(
                            selected = framing.transition == t,
                            onClick = { onUpdateFraming { it.copy(transition = t) } },
                            label = { Text(t.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = framing.openingTitle,
                    onValueChange = { txt -> onUpdateFraming { it.copy(openingTitle = txt) } },
                    label = { Text("Opening Title (0.0s–2.5s)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = framing.closingText,
                    onValueChange = { txt -> onUpdateFraming { it.copy(closingText = txt) } },
                    label = { Text("Closing Call-To-Action (Last 2.5s)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AiToolsPanel(
    clip: GeneratedClip,
    aiNotice: String,
    aiBusyMessage: String?,
    onRegenerateClip: () -> Unit,
    onRefreshHooks: () -> Unit,
    onApplyHookAsOpeningTitle: (String) -> Unit,
    onRefreshTitles: () -> Unit,
    onApplyTitle: (String) -> Unit,
    onRefreshSocialCaption: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AiFirebaseNoticeBanner(noticeText = aiNotice)

        if (aiBusyMessage != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ElectricViolet)
            Text(aiBusyMessage, style = MaterialTheme.typography.bodySmall, color = ElectricBlue)
        }

        // AI Clip Score + Regenerate
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Clip Engagement Score", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    ScoreEstimatePill(score = clip.scoreEstimate)
                }
                Text(clip.scoreExplanation, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = stringResource(R.string.score_estimate_disclaimer),
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlue
                )
                Text(
                    text = "Why Selected: ${clip.selectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                OutlinedButton(
                    onClick = onRegenerateClip,
                    border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("regenerate_clip_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Regenerate clip", tint = ElectricViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Regenerate Clip Moment", color = TextPrimary)
                }
            }
        }

        // AI Hook Generator (3 hooks)
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Hook Generator (3 Options)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    IconButton(onClick = onRefreshHooks, modifier = Modifier.testTag("refresh_hooks_button")) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Regenerate hooks", tint = ElectricViolet)
                    }
                }
                clip.hooks.forEach { hook ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCanvas,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApplyHookAsOpeningTitle(hook) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(hook, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("Use", style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                        }
                    }
                }
            }
        }

        // AI Title Generator (5 titles)
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Title Generator (5 Options)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    IconButton(onClick = onRefreshTitles, modifier = Modifier.testTag("refresh_titles_button")) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Regenerate titles", tint = ElectricViolet)
                    }
                }
                clip.titleSuggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCanvas,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApplyTitle(suggestion) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(suggestion, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("Apply", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                        }
                    }
                }
            }
        }

        // AI Social Caption & Hashtags
        StudioSectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Caption & Hashtags", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Row {
                        IconButton(onClick = onRefreshSocialCaption) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Regenerate social caption", tint = ElectricBlue)
                        }
                        IconButton(onClick = {
                            val full = "${clip.socialCaption}\n\n${clip.hashtags.joinToString(" ")}"
                            clipboard.setText(AnnotatedString(full))
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy caption and hashtags", tint = TextPrimary)
                        }
                    }
                }
                Text(clip.socialCaption, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Text(clip.hashtags.joinToString(" "), style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ColorRowSelector(
    label: String,
    swatches: List<Pair<Long, String>>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { (hex, name) ->
                val isSelected = selectedHex == hex
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) ElectricViolet else SubtleBorder,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }
}

private fun Float.roundToLongIfZero(): Long = (this / 50f).roundToInt() * 50L
