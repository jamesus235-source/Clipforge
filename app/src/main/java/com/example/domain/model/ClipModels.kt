package com.example.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class SourceType {
    LOCAL_FILE,
    SAMPLE_STUDIO,
    YOUTUBE_METADATA
}

@Serializable
enum class AspectRatioOption(val label: String, val ratio: Float) {
    VERTICAL_9_16("9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    SQUARE_1_1("1:1", 1f)
}

@Serializable
enum class CaptionPreset(val displayName: String) {
    CREATOR_BOLD("Creator Bold"),
    CLEAN_MINIMAL("Clean Minimal"),
    GAMING("Gaming"),
    PODCAST("Podcast"),
    CINEMATIC("Cinematic"),
    HIGHLIGHT_WORDS("Highlight Words")
}

@Serializable
enum class SubtitlePosition(val label: String, val verticalBias: Float) {
    TOP("Top", 0.16f),
    MIDDLE("Middle", 0.50f),
    BOTTOM("Bottom", 0.80f)
}

@Serializable
enum class SubtitleAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

@Serializable
enum class SubtitleAnimation(val label: String) {
    NONE("None"),
    POP("Pop"),
    SLIDE_UP("Slide Up"),
    KARAOKE("Highlight"),
    FADE("Fade")
}

@Serializable
enum class BackgroundMode(val label: String) {
    SOLID_DARK("Charcoal"),
    GRADIENT_INDIGO("Indigo Glow"),
    MESH_STUDIO("Mesh Art"),
    CUSTOM_IMAGE("Custom Image")
}

@Serializable
enum class TransitionStyle(val label: String) {
    NONE("Cut"),
    FADE("Crossfade"),
    ZOOM_IN("Zoom Punch")
}

@Serializable
enum class ExportResolution(val label: String, val shortSidePx: Int, val longSidePx: Int) {
    HD_720("720p", 720, 1280),
    FHD_1080("1080p", 1080, 1920)
}

@Serializable
data class YouTubeMetadata(
    val videoId: String,
    val originalUrl: String,
    val title: String,
    val authorName: String,
    val thumbnailUrl: String,
    val authorUrl: String = "",
    val width: Int = 1280,
    val height: Int = 720
)

@Serializable
data class SilenceSpan(
    val startMs: Long,
    val endMs: Long
)

@Serializable
data class SubtitleSegment(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val highlightedWords: List<String> = emptyList()
)

@Serializable
data class SubtitleStyle(
    val preset: CaptionPreset = CaptionPreset.CREATOR_BOLD,
    val fontId: String = "montserrat",
    val fontSizeSp: Float = 24f,
    val isBold: Boolean = true,
    val fontColorHex: Long = 0xFFFFFFFF,
    val backgroundColorHex: Long = 0xFF0B0D14,
    val backgroundOpacity: Float = 0.65f,
    val outlineColorHex: Long = 0xFF000000,
    val outlineThickness: Float = 4f,
    val hasShadow: Boolean = true,
    val alignment: SubtitleAlignment = SubtitleAlignment.CENTER,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val lineSpacingMultiplier: Float = 1.15f,
    val wordsPerLine: Int = 4,
    val highlightKeywords: Boolean = true,
    val highlightColorHex: Long = 0xFF7C5CFF,
    val animation: SubtitleAnimation = SubtitleAnimation.POP,
    val timingOffsetMs: Long = 0L
) {
    companion object {
        fun fromPreset(preset: CaptionPreset): SubtitleStyle = when (preset) {
            CaptionPreset.CREATOR_BOLD -> SubtitleStyle(
                preset = preset,
                fontId = "montserrat",
                fontSizeSp = 26f,
                isBold = true,
                fontColorHex = 0xFFFFFFFF,
                backgroundColorHex = 0xFF090B10,
                backgroundOpacity = 0.45f,
                outlineColorHex = 0xFF000000,
                outlineThickness = 5f,
                hasShadow = true,
                highlightKeywords = true,
                highlightColorHex = 0xFF38BDF8,
                animation = SubtitleAnimation.POP
            )
            CaptionPreset.CLEAN_MINIMAL -> SubtitleStyle(
                preset = preset,
                fontId = "inter",
                fontSizeSp = 20f,
                isBold = false,
                fontColorHex = 0xFFF8FAFC,
                backgroundColorHex = 0xFF11141D,
                backgroundOpacity = 0.72f,
                outlineColorHex = 0xFF000000,
                outlineThickness = 0f,
                hasShadow = false,
                highlightKeywords = false,
                highlightColorHex = 0xFF7C5CFF,
                animation = SubtitleAnimation.FADE
            )
            CaptionPreset.GAMING -> SubtitleStyle(
                preset = preset,
                fontId = "bebas_neue",
                fontSizeSp = 30f,
                isBold = true,
                fontColorHex = 0xFFFACC15,
                backgroundColorHex = 0xFF090B10,
                backgroundOpacity = 0.25f,
                outlineColorHex = 0xFF000000,
                outlineThickness = 7f,
                hasShadow = true,
                highlightKeywords = true,
                highlightColorHex = 0xFF38BDF8,
                animation = SubtitleAnimation.POP
            )
            CaptionPreset.PODCAST -> SubtitleStyle(
                preset = preset,
                fontId = "plus_jakarta",
                fontSizeSp = 22f,
                isBold = true,
                fontColorHex = 0xFFFFFFFF,
                backgroundColorHex = 0xFF1E2332,
                backgroundOpacity = 0.85f,
                outlineColorHex = 0xFF000000,
                outlineThickness = 2f,
                hasShadow = true,
                highlightKeywords = true,
                highlightColorHex = 0xFF7C5CFF,
                animation = SubtitleAnimation.SLIDE_UP
            )
            CaptionPreset.CINEMATIC -> SubtitleStyle(
                preset = preset,
                fontId = "playfair",
                fontSizeSp = 21f,
                isBold = false,
                fontColorHex = 0xFFF1F5F9,
                backgroundColorHex = 0xFF000000,
                backgroundOpacity = 0.0f,
                outlineColorHex = 0xFF000000,
                outlineThickness = 2f,
                hasShadow = true,
                highlightKeywords = false,
                highlightColorHex = 0xFFF59E0B,
                animation = SubtitleAnimation.FADE
            )
            CaptionPreset.HIGHLIGHT_WORDS -> SubtitleStyle(
                preset = preset,
                fontId = "space_grotesk",
                fontSizeSp = 25f,
                isBold = true,
                fontColorHex = 0xFFFFFFFF,
                backgroundColorHex = 0xFF0B0D14,
                backgroundOpacity = 0.6f,
                outlineColorHex = 0xFF000000,
                outlineThickness = 4f,
                hasShadow = true,
                highlightKeywords = true,
                highlightColorHex = 0xFF10B981,
                animation = SubtitleAnimation.KARAOKE
            )
        }
    }
}

@Serializable
data class VideoFramingConfig(
    val aspectRatio: AspectRatioOption = AspectRatioOption.VERTICAL_9_16,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val autoSpeakerTracking: Boolean = true,
    val backgroundMode: BackgroundMode = BackgroundMode.GRADIENT_INDIGO,
    val customBackgroundUri: String? = null,
    val watermarkText: String = "",
    val watermarkImageUri: String? = null,
    val speed: Float = 1.0f,
    val brightness: Float = 0.0f, // -0.4f to +0.4f
    val contrast: Float = 1.0f,   // 0.6f to 1.6f
    val volume: Float = 1.0f,     // 0.0f to 1.5f
    val backgroundMusicUri: String? = null,
    val backgroundMusicName: String? = null,
    val backgroundMusicVolume: Float = 0.25f,
    val transition: TransitionStyle = TransitionStyle.FADE,
    val openingTitle: String = "",
    val closingText: String = "",
    val removeSilences: Boolean = false
)

@Serializable
data class GeneratedClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val startMs: Long,
    val endMs: Long,
    val scoreEstimate: Int,
    val scoreExplanation: String,
    val selectionReason: String,
    val suggestedPreset: CaptionPreset = CaptionPreset.CREATOR_BOLD,
    val transcriptSegments: List<SubtitleSegment>,
    val hooks: List<String>,
    val titleSuggestions: List<String>,
    val socialCaption: String,
    val hashtags: List<String>,
    val subtitleStyle: SubtitleStyle = SubtitleStyle.fromPreset(suggestedPreset),
    val framing: VideoFramingConfig = VideoFramingConfig(),
    val isAiDemoPlaceholder: Boolean = true,
    val exportedFilePath: String? = null,
    val exportedFileSizeBytes: Long = 0L,
    val exportedResolution: String? = null,
    val exportedFps: Int = 30,
    val exportedWithSubtitles: Boolean = true
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1000L)
}

@Serializable
data class ClipProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val sourceType: SourceType,
    val sourceVideoPath: String? = null,
    val sourceDisplayName: String = "",
    val sourceDurationMs: Long = 0L,
    val sourceWidth: Int = 1280,
    val sourceHeight: Int = 720,
    val youtubeMetadata: YouTubeMetadata? = null,
    val clips: List<GeneratedClip> = emptyList(),
    val detectedSilences: List<SilenceSpan> = emptyList(),
    val isSampleAiOutput: Boolean = true
)

@Serializable
data class BrandPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtitleStyle: SubtitleStyle,
    val watermarkText: String,
    val watermarkImageUri: String? = null,
    val aspectRatio: AspectRatioOption = AspectRatioOption.VERTICAL_9_16,
    val backgroundMode: BackgroundMode = BackgroundMode.GRADIENT_INDIGO,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.HD_720,
    val fps: Int = 30,
    val burnSubtitles: Boolean = true,
    val includeAudio: Boolean = true
)
