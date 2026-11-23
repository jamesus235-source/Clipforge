package com.example

import com.example.data.ai.ClipGenerationRequest
import com.example.data.ai.DemoAiClipRepository
import com.example.data.network.YouTubeParseResult
import com.example.data.network.YouTubeUrlParser
import com.example.domain.model.CaptionPreset
import com.example.domain.model.SubtitleStyle
import com.example.ui.theme.SubtitleFonts
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `youtube parser extracts IDs from watch, youtu_be, and shorts URLs`() {
        val watch = YouTubeUrlParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s")
        assertTrue(watch is YouTubeParseResult.Valid)
        assertEquals("dQw4w9WgXcQ", (watch as YouTubeParseResult.Valid).videoId)

        val shortUrl = YouTubeUrlParser.parse("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(shortUrl is YouTubeParseResult.Valid)
        assertEquals("dQw4w9WgXcQ", (shortUrl as YouTubeParseResult.Valid).videoId)

        val shorts = YouTubeUrlParser.parse("https://youtube.com/shorts/dQw4w9WgXcQ")
        assertTrue(shorts is YouTubeParseResult.Valid)
        assertEquals("dQw4w9WgXcQ", (shorts as YouTubeParseResult.Valid).videoId)

        val invalid = YouTubeUrlParser.parse("https://vimeo.com/123456")
        assertTrue(invalid is YouTubeParseResult.Invalid)
    }

    @Test
    fun `demo clip generator creates distinct non-overlapping clips`() = runTest {
        val repo = DemoAiClipRepository()
        val clips = repo.generateClips(
            ClipGenerationRequest(
                sourceTitle = "Studio Test",
                totalDurationMs = 120_000L,
                clipCount = 4,
                preferredDurationSec = 25
            )
        )
        assertEquals(4, clips.size)
        for (i in 0 until clips.lastIndex) {
            assertTrue(clips[i].endMs <= clips[i + 1].startMs)
        }
        assertTrue(clips.all { it.isAiDemoPlaceholder })
    }

    @Test
    fun `manual range generates single clip matching user range`() = runTest {
        val repo = DemoAiClipRepository()
        val clips = repo.generateClips(
            ClipGenerationRequest(
                sourceTitle = "Manual Range",
                totalDurationMs = 60_000L,
                clipCount = 4,
                preferredDurationSec = 30,
                manualStartMs = 10_000L,
                manualEndMs = 35_000L
            )
        )
        assertEquals(1, clips.size)
        assertEquals(10_000L, clips.first().startMs)
        assertEquals(35_000L, clips.first().endMs)
    }

    @Test
    fun `subtitle fonts and presets are properly configured`() {
        assertEquals(11, SubtitleFonts.all.size)
        CaptionPreset.entries.forEach { preset ->
            val style = SubtitleStyle.fromPreset(preset)
            assertEquals(preset, style.preset)
        }
    }
}
