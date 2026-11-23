package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.BrandPreset
import com.example.domain.model.CaptionPreset
import com.example.domain.model.ClipProject
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SubtitleStyle
import com.example.domain.model.VideoFramingConfig
import com.example.ui.components.LiveVideoPreviewStage
import com.example.ui.components.ScoreEstimatePill
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun EditorScreen(
    project: ClipProject?,
    selectedClip: GeneratedClip?,
    brandPresets: List<BrandPreset>,
    aiNotice: String,
    aiBusyMessage: String?,
    onSelectClip: (String) -> Unit,
    onAddManualClip: () -> Unit,
    onUpdateClip: ((GeneratedClip) -> GeneratedClip) -> Unit,
    onApplyPreset: (CaptionPreset) -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onUpdateFraming: ((VideoFramingConfig) -> VideoFramingConfig) -> Unit,
    onUpdateSegmentText: (String, String) -> Unit,
    onUpdateSegmentTiming: (String, Long, Long) -> Unit,
    onAddSubtitleSegment: (String) -> Unit,
    onPickBackgroundMusic: (Uri) -> Unit,
    onRegenerateClip: () -> Unit,
    onRefreshHooks: () -> Unit,
    onRefreshTitles: () -> Unit,
    onRefreshSocialCaption: () -> Unit,
    onSaveBrandPreset: (String) -> Unit,
    onApplyBrandPreset: (BrandPreset) -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (project == null || selectedClip == null || project.sourceVideoPath == null) {
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
                Icon(
                    imageVector = Icons.Filled.MovieFilter,
                    contentDescription = "No project open",
                    tint = ElectricViolet,
                    modifier = Modifier.size(48.dp)
                )
                Text("No Clip Selected Yet", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    "Start by choosing a video or generating clips on the Create tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Go to Create")
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Clips" to Icons.Filled.ContentCut,
        "Subtitles" to Icons.Filled.ClosedCaption,
        "Video" to Icons.Filled.Tune,
        "AI Tools" to Icons.Filled.AutoAwesome
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Editor Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.clips.size} clips • Tap a clip to customize",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onNavigateToExport,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("editor_go_export_button")
            ) {
                Icon(Icons.Filled.Output, contentDescription = "Go to export", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export")
            }
        }

        // Clip Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            project.clips.forEachIndexed { idx, clip ->
                val isSelected = clip.id == selectedClip.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ElectricViolet.copy(alpha = 0.22f) else SlateCard,
                    border = BorderStroke(1.dp, if (isSelected) ElectricViolet else SubtleBorder),
                    modifier = Modifier
                        .clickable { onSelectClip(clip.id) }
                        .testTag("clip_selector_card_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Clip ${idx + 1}: ${clip.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${MainViewModel.formatTimestamp(clip.startMs)}–${MainViewModel.formatTimestamp(clip.endMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricBlue
                            )
                        }
                        ScoreEstimatePill(score = clip.scoreEstimate)
                    }
                }
            }

            OutlinedButton(
                onClick = onAddManualClip,
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("add_manual_clip_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add manual clip", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Manual")
            }
        }

        // Adaptive Wide vs Compact Layout: Side-by-side on tablets/landscape, stacked on phones
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 720.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(0.45f)) {
                        LiveVideoPreviewStage(
                            sourceVideoPath = project.sourceVideoPath,
                            clip = selectedClip,
                            detectedSilences = project.detectedSilences
                        )
                    }
                    Column(modifier = Modifier.weight(0.55f)) {
                        EditorTabStrip(tabs, selectedTab) { selectedTab = it }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            ActiveEditorTabContent(
                                selectedTab = selectedTab,
                                project = project,
                                selectedClip = selectedClip,
                                brandPresets = brandPresets,
                                aiNotice = aiNotice,
                                aiBusyMessage = aiBusyMessage,
                                onUpdateClip = onUpdateClip,
                                onApplyPreset = onApplyPreset,
                                onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                                onUpdateFraming = onUpdateFraming,
                                onUpdateSegmentText = onUpdateSegmentText,
                                onUpdateSegmentTiming = onUpdateSegmentTiming,
                                onAddSubtitleSegment = onAddSubtitleSegment,
                                onPickBackgroundMusic = onPickBackgroundMusic,
                                onRegenerateClip = onRegenerateClip,
                                onRefreshHooks = onRefreshHooks,
                                onRefreshTitles = onRefreshTitles,
                                onRefreshSocialCaption = onRefreshSocialCaption,
                                onSaveBrandPreset = onSaveBrandPreset,
                                onApplyBrandPreset = onApplyBrandPreset
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiveVideoPreviewStage(
                        sourceVideoPath = project.sourceVideoPath,
                        clip = selectedClip,
                        detectedSilences = project.detectedSilences
                    )
                    EditorTabStrip(tabs, selectedTab) { selectedTab = it }
                    ActiveEditorTabContent(
                        selectedTab = selectedTab,
                        project = project,
                        selectedClip = selectedClip,
                        brandPresets = brandPresets,
                        aiNotice = aiNotice,
                        aiBusyMessage = aiBusyMessage,
                        onUpdateClip = onUpdateClip,
                        onApplyPreset = onApplyPreset,
                        onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                        onUpdateFraming = onUpdateFraming,
                        onUpdateSegmentText = onUpdateSegmentText,
                        onUpdateSegmentTiming = onUpdateSegmentTiming,
                        onAddSubtitleSegment = onAddSubtitleSegment,
                        onPickBackgroundMusic = onPickBackgroundMusic,
                        onRegenerateClip = onRegenerateClip,
                        onRefreshHooks = onRefreshHooks,
                        onRefreshTitles = onRefreshTitles,
                        onRefreshSocialCaption = onRefreshSocialCaption,
                        onSaveBrandPreset = onSaveBrandPreset,
                        onApplyBrandPreset = onApplyBrandPreset
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorTabStrip(
    tabs: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = SlateElevated,
        contentColor = TextPrimary,
        edgePadding = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateElevated, RoundedCornerShape(12.dp))
    ) {
        tabs.forEachIndexed { idx, (label, icon) ->
            Tab(
                selected = selectedTab == idx,
                onClick = { onSelect(idx) },
                modifier = Modifier.testTag("editor_tab_$idx"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selectedTab == idx) ElectricViolet else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            label,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) Color.White else TextSecondary
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ActiveEditorTabContent(
    selectedTab: Int,
    project: ClipProject,
    selectedClip: GeneratedClip,
    brandPresets: List<BrandPreset>,
    aiNotice: String,
    aiBusyMessage: String?,
    onUpdateClip: ((GeneratedClip) -> GeneratedClip) -> Unit,
    onApplyPreset: (CaptionPreset) -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onUpdateFraming: ((VideoFramingConfig) -> VideoFramingConfig) -> Unit,
    onUpdateSegmentText: (String, String) -> Unit,
    onUpdateSegmentTiming: (String, Long, Long) -> Unit,
    onAddSubtitleSegment: (String) -> Unit,
    onPickBackgroundMusic: (Uri) -> Unit,
    onRegenerateClip: () -> Unit,
    onRefreshHooks: () -> Unit,
    onRefreshTitles: () -> Unit,
    onRefreshSocialCaption: () -> Unit,
    onSaveBrandPreset: (String) -> Unit,
    onApplyBrandPreset: (BrandPreset) -> Unit
) {
    when (selectedTab) {
        0 -> ClipsAndTranscriptPanel(
            project = project,
            clip = selectedClip,
            onUpdateClipRange = { s, e -> onUpdateClip { it.copy(startMs = s, endMs = e) } },
            onToggleRemoveSilences = { remove -> onUpdateFraming { it.copy(removeSilences = remove) } },
            onUpdateSegmentText = onUpdateSegmentText,
            onUpdateSegmentTiming = onUpdateSegmentTiming,
            onAddSubtitleSegment = onAddSubtitleSegment
        )
        1 -> SubtitleCustomizationPanel(
            style = selectedClip.subtitleStyle,
            brandPresets = brandPresets,
            onApplyPreset = onApplyPreset,
            onUpdateStyle = onUpdateSubtitleStyle,
            onSaveBrandPreset = onSaveBrandPreset,
            onApplyBrandPreset = onApplyBrandPreset
        )
        2 -> VideoCustomizationPanel(
            framing = selectedClip.framing,
            onUpdateFraming = onUpdateFraming,
            onPickBackgroundMusic = onPickBackgroundMusic
        )
        3 -> AiToolsPanel(
            clip = selectedClip,
            aiNotice = aiNotice,
            aiBusyMessage = aiBusyMessage,
            onRegenerateClip = onRegenerateClip,
            onRefreshHooks = onRefreshHooks,
            onApplyHookAsOpeningTitle = { hook -> onUpdateFraming { it.copy(openingTitle = hook) } },
            onRefreshTitles = onRefreshTitles,
            onApplyTitle = { title -> onUpdateClip { it.copy(title = title) } },
            onRefreshSocialCaption = onRefreshSocialCaption
        )
    }
}
