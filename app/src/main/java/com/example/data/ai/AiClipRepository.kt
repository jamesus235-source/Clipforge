package com.example.data.ai

import com.example.domain.model.CaptionPreset
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SilenceSpan
import com.example.domain.model.SubtitleSegment
import com.example.domain.model.SubtitleStyle
import com.example.domain.model.YouTubeMetadata
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class ClipGenerationRequest(
    val sourceTitle: String,
    val totalDurationMs: Long,
    val clipCount: Int,
    val preferredDurationSec: Int,
    val manualStartMs: Long? = null,
    val manualEndMs: Long? = null,
    val silences: List<SilenceSpan> = emptyList(),
    val youtubeMetadata: YouTubeMetadata? = null
)

interface AiClipRepository {
    val isLiveAiEnabled: Boolean
    val statusNotice: String

    suspend fun generateClips(request: ClipGenerationRequest): List<GeneratedClip>
    suspend fun regenerateClip(
        existingClip: GeneratedClip,
        totalDurationMs: Long,
        existingClips: List<GeneratedClip>
    ): GeneratedClip
    suspend fun generateHooks(clip: GeneratedClip): List<String>
    suspend fun generateTitles(clip: GeneratedClip): List<String>
    suspend fun generateSocialCaption(clip: GeneratedClip): Pair<String, List<String>>
}

/**
 * Placeholder/demo AI repository used when Firebase AI Logic is not enabled.
 * Always clearly labels generated insights, transcripts, hooks, titles, and scores
 * as sample output requiring Firebase AI Logic for live Gemini multimodal inference.
 */
class DemoAiClipRepository : AiClipRepository {

    override val isLiveAiEnabled: Boolean = false
    override val statusNotice: String =
        "AI features require Firebase. Showing labeled sample AI clips & copy—video playback, silence scan, editor & MP4 export run locally."

    private data class SampleMomentTemplate(
        val title: String,
        val description: String,
        val selectionReason: String,
        val score: Int,
        val scoreReason: String,
        val preset: CaptionPreset,
        val lines: List<Pair<String, List<String>>>,
        val hooks: List<String>,
        val titles: List<String>,
        val socialCaption: String,
        val hashtags: List<String>
    )

    private val templates = listOf(
        SampleMomentTemplate(
            title = "The 3-Second Retention Rule",
            description = "Sample clip showcasing a bold opening claim and crisp pacing shift.",
            selectionReason = "Sample AI Reason: Strong opening statement paired with high cadence.",
            score = 91,
            scoreReason = "Sample Estimate: Opens with a direct contrarian hook and clean payoff.",
            preset = CaptionPreset.CREATOR_BOLD,
            lines = listOf(
                "Most creators lose half their viewers" to listOf("half", "viewers"),
                "in the first three seconds." to listOf("three", "seconds"),
                "Cut the throat-clearing and start mid-thought." to listOf("start", "mid-thought"),
                "Your retention graph changes overnight." to listOf("retention", "overnight")
            ),
            hooks = listOf(
                "Stop introducing yourself on Shorts.",
                "Why 70% of viewers swipe away at 0:02",
                "Delete the first 5 seconds of your video."
            ),
            titles = listOf(
                "The 3-Second Retention Rule",
                "Why Viewers Swipe Away Immediately",
                "Fix Your Hook in 5 Seconds",
                "The Editing Mistake Killing Your Shorts",
                "How Top Creators Open Every Clip"
            ),
            socialCaption = "Sample caption: Start with the payoff, not the intro. Watch how pacing shifts retention.",
            hashtags = listOf("#VideoEditing", "#ContentCreator", "#ShortsStrategy", "#Retention")
        ),
        SampleMomentTemplate(
            title = "Why Audio Clarity Beats 4K",
            description = "Sample insight explaining why crisp voice audio outperforms camera upgrades.",
            selectionReason = "Sample AI Reason: Memorable takeaway with clear visual framing opportunity.",
            score = 87,
            scoreReason = "Sample Estimate: Relatable creator truth with strong shareability.",
            preset = CaptionPreset.PODCAST,
            lines = listOf(
                "Viewers forgive soft 1080p video," to listOf("1080p", "video"),
                "but they leave bad audio in seconds." to listOf("bad", "audio"),
                "Tighten your mic placement and trim pauses." to listOf("trim", "pauses"),
                "Clarity keeps people locked into the story." to listOf("Clarity", "story")
            ),
            hooks = listOf(
                "Your camera isn't the bottleneck.",
                "The cheapest upgrade that doubles watch time",
                "Never publish before checking this audio rule"
            ),
            titles = listOf(
                "Why Audio Clarity Beats 4K",
                "Stop Buying Cameras Until You Fix This",
                "The Audio Secret Behind Viral Clips",
                "Crisp Sound = Higher Watch Time",
                "Creator Studio Setup Truth"
            ),
            socialCaption = "Sample caption: Crisp audio + tight silence removal > expensive lenses.",
            hashtags = listOf("#PodcastTips", "#CreatorGear", "#AudioDesign", "#ReelsTips")
        ),
        SampleMomentTemplate(
            title = "One Idea Per Vertical Clip",
            description = "Sample story breakdown on isolating self-contained moments from long talks.",
            selectionReason = "Sample AI Reason: Complete self-contained mini-story without context gaps.",
            score = 84,
            scoreReason = "Sample Estimate: Self-contained narrative arc fits 25–45s vertical window.",
            preset = CaptionPreset.HIGHLIGHT_WORDS,
            lines = listOf(
                "Every viral short carries one sharp idea." to listOf("one", "sharp", "idea"),
                "Not three topics squeezed together." to listOf("three", "topics"),
                "Find the emotional spike, frame the speaker," to listOf("emotional", "spike"),
                "and end right on the punchline." to listOf("punchline")
            ),
            hooks = listOf(
                "One video can feed your channel all week.",
                "How to spot a viral moment in a 60-minute talk",
                "The golden rule of vertical storytelling"
            ),
            titles = listOf(
                "One Idea Per Vertical Clip",
                "How to Repurpose Long Podcasts",
                "Stop Overstuffing Your Shorts",
                "Cut Right on the Punchline",
                "Long-Form to Shorts Workflow"
            ),
            socialCaption = "Sample caption: One strong moment, clean captions, zero filler.",
            hashtags = listOf("#Repurposing", "#ShortFormVideo", "#CreatorWorkflow", "#ClipForge")
        ),
        SampleMomentTemplate(
            title = "Color, Contrast & Visual Rhythm",
            description = "Sample visual tip on making vertical framing pop on mobile feeds.",
            selectionReason = "Sample AI Reason: Actionable visual pacing cue ideal for on-screen text.",
            score = 81,
            scoreReason = "Sample Estimate: Practical checklist format encourages saves.",
            preset = CaptionPreset.CINEMATIC,
            lines = listOf(
                "Small mobile screens reward high contrast." to listOf("high", "contrast"),
                "Keep subtitles clear of bottom UI icons," to listOf("subtitles", "clear"),
                "and let bold typography guide the eye." to listOf("bold", "typography")
            ),
            hooks = listOf(
                "Make your shorts look like a studio production",
                "3 framing tweaks for instant polish",
                "Are your captions hidden behind UI buttons?"
            ),
            titles = listOf(
                "Color, Contrast & Visual Rhythm",
                "Subtitle Safe Zones Explained",
                "3 Framing Tweaks for Vertical Video",
                "Cinematic Shorts Without Complex Rigs",
                "Design Your Captions Like a Pro"
            ),
            socialCaption = "Sample caption: Respect mobile safe zones and contrast for instant feed polish.",
            hashtags = listOf("#MotionDesign", "#MobileVideo", "#Filmmaking", "#Typography")
        )
    )

    override suspend fun generateClips(request: ClipGenerationRequest): List<GeneratedClip> {
        delay(450)
        val totalMs = request.totalDurationMs.coerceAtLeast(10_000L)

        // Manual range selection bypasses AI moment splitting and builds a clean single clip for the range
        if (request.manualStartMs != null && request.manualEndMs != null) {
            val start = request.manualStartMs.coerceIn(0L, totalMs - 2_000L)
            val end = request.manualEndMs.coerceIn(start + 2_000L, totalMs)
            val tmpl = templates.first()
            return listOf(
                buildClipFromTemplate(
                    index = 0,
                    template = tmpl,
                    startMs = start,
                    endMs = end,
                    customTitle = "[Sample AI] Manual Selection (${formatRange(start, end)})",
                    selectionReason = "Manual range selected by user (${formatRange(start, end)}). Sample captions generated."
                )
            )
        }

        val count = request.clipCount.coerceIn(1, 10)
        val preferredMs = (request.preferredDurationSec.coerceIn(15, 60)) * 1000L

        // Partition the actual video duration into non-overlapping distinct windows
        val slotMs = totalMs / count
        return (0 until count).map { idx ->
            val template = templates[idx % templates.size]
            val slotStart = idx * slotMs
            val slotEnd = if (idx == count - 1) totalMs else (idx + 1) * slotMs
            val rawClipLen = min(preferredMs, max(4_000L, slotEnd - slotStart))

            // Snap away from detected leading silence if available
            val leadingSilence = request.silences.firstOrNull {
                it.startMs <= slotStart + 1500L && it.endMs > slotStart && (it.endMs - slotStart) < (slotEnd - slotStart) / 2
            }
            val startMs = (leadingSilence?.endMs ?: slotStart).coerceIn(0L, max(0L, totalMs - 3_000L))
            val endMs = (startMs + rawClipLen).coerceIn(min(totalMs, startMs + 3_000L), totalMs)

            val suffix = if (idx >= templates.size) " #${idx + 1}" else ""
            buildClipFromTemplate(
                index = idx,
                template = template,
                startMs = startMs,
                endMs = endMs,
                customTitle = "[Sample] ${template.title}$suffix",
                selectionReason = template.selectionReason
            )
        }
    }

    override suspend fun regenerateClip(
        existingClip: GeneratedClip,
        totalDurationMs: Long,
        existingClips: List<GeneratedClip>
    ): GeneratedClip {
        delay(350)
        val safeTotal = totalDurationMs.coerceAtLeast(10_000L)
        val span = existingClip.durationMs.coerceIn(4_000L, safeTotal)
        val shift = (safeTotal / 5L).coerceAtLeast(2_500L)
        val candidateStart = (existingClip.startMs + shift) % max(1L, safeTotal - span)
        val candidateEnd = (candidateStart + span).coerceAtMost(safeTotal)
        val altTemplate = templates[(existingClips.indexOfFirst { it.id == existingClip.id } + 1).coerceAtLeast(0) % templates.size]

        return buildClipFromTemplate(
            index = 0,
            template = altTemplate,
            startMs = candidateStart,
            endMs = candidateEnd,
            customTitle = "[Sample Regenerated] ${altTemplate.title}",
            selectionReason = "Regenerated alternate window (${formatRange(candidateStart, candidateEnd)}). Sample AI analysis."
        ).copy(
            id = existingClip.id,
            subtitleStyle = existingClip.subtitleStyle,
            framing = existingClip.framing
        )
    }

    override suspend fun generateHooks(clip: GeneratedClip): List<String> {
        delay(220)
        return listOf(
            "[Sample Hook] Stop scrolling if you edit ${clip.title.removePrefix("[Sample] ").take(24)}...",
            "[Sample Hook] Nobody tells beginners this about pacing.",
            "[Sample Hook] Watch the first 3 seconds closely."
        )
    }

    override suspend fun generateTitles(clip: GeneratedClip): List<String> {
        delay(220)
        val base = clip.title.removePrefix("[Sample] ").removePrefix("[Sample Regenerated] ")
        return listOf(
            "Sample: $base",
            "Sample: Why $base Works",
            "Sample: The Truth About $base",
            "Sample: Fix Your Shorts Today",
            "Sample: Pro Framing Breakdown"
        )
    }

    override suspend fun generateSocialCaption(clip: GeneratedClip): Pair<String, List<String>> {
        delay(220)
        return "[Sample Caption] ${clip.description} (Enable Firebase AI Logic for live Gemini copy.)" to
            listOf("#ClipForgeAI", "#Shorts", "#CreatorTips", "#VideoEditing")
    }

    private fun buildClipFromTemplate(
        index: Int,
        template: SampleMomentTemplate,
        startMs: Long,
        endMs: Long,
        customTitle: String,
        selectionReason: String
    ): GeneratedClip {
        val duration = (endMs - startMs).coerceAtLeast(2000L)
        val lines = template.lines
        val segDuration = duration / lines.size.coerceAtLeast(1)
        val segments = lines.mapIndexed { segIdx, (text, highlights) ->
            val segStart = segIdx * segDuration
            val segEnd = if (segIdx == lines.lastIndex) duration else (segIdx + 1) * segDuration
            SubtitleSegment(
                id = UUID.randomUUID().toString(),
                startMs = segStart,
                endMs = segEnd,
                text = text,
                highlightedWords = highlights
            )
        }

        return GeneratedClip(
            title = customTitle,
            description = template.description,
            startMs = startMs,
            endMs = endMs,
            scoreEstimate = (template.score - (index * 2)).coerceIn(65, 96),
            scoreExplanation = template.scoreReason,
            selectionReason = selectionReason,
            suggestedPreset = template.preset,
            transcriptSegments = segments,
            hooks = template.hooks,
            titleSuggestions = template.titles,
            socialCaption = template.socialCaption,
            hashtags = template.hashtags,
            subtitleStyle = SubtitleStyle.fromPreset(template.preset),
            isAiDemoPlaceholder = true
        )
    }

    private fun formatRange(startMs: Long, endMs: Long): String {
        fun fmt(ms: Long): String {
            val sec = ms / 1000
            return "%02d:%02d".format(sec / 60, sec % 60)
        }
        return "${fmt(startMs)}–${fmt(endMs)}"
    }
}
