package com.example.data.network

import com.example.domain.model.YouTubeMetadata
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

sealed class YouTubeParseResult {
    data class Valid(val videoId: String, val canonicalUrl: String) : YouTubeParseResult()
    data class Invalid(val errorMessage: String) : YouTubeParseResult()
}

object YouTubeUrlParser {
    private val videoIdRegex = Regex("^[A-Za-z0-9_-]{11}$")

    fun parse(rawInput: String): YouTubeParseResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return YouTubeParseResult.Invalid("Paste a YouTube video link to continue.")
        }

        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        val uri = try {
            java.net.URI(normalized)
        } catch (_: Exception) {
            return YouTubeParseResult.Invalid("That doesn't look like a valid URL.")
        }

        val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: ""
        val pathSegments = uri.path.orEmpty().split("/").filter { it.isNotEmpty() }
        val videoId: String? = when (host) {
            "youtu.be" -> pathSegments.firstOrNull()
            "youtube.com", "youtube-nocookie.com" -> {
                when {
                    uri.path == "/watch" -> extractQueryParam(uri.rawQuery, "v")
                    pathSegments.size >= 2 && pathSegments[0] == "shorts" -> pathSegments[1]
                    pathSegments.size >= 2 && pathSegments[0] == "embed" -> pathSegments[1]
                    pathSegments.size >= 2 && pathSegments[0] == "live" -> pathSegments[1]
                    else -> null
                }
            }
            else -> return YouTubeParseResult.Invalid(
                "Unsupported domain. Use youtube.com/watch?v=, youtu.be/, or youtube.com/shorts/."
            )
        }

        if (videoId.isNullOrBlank() || !videoIdRegex.matches(videoId)) {
            return YouTubeParseResult.Invalid(
                "Could not find a valid 11-character YouTube video ID in this URL."
            )
        }

        return YouTubeParseResult.Valid(
            videoId = videoId,
            canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
        )
    }

    private fun extractQueryParam(rawQuery: String?, key: String): String? {
        if (rawQuery.isNullOrBlank()) return null
        return rawQuery.split("&")
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) null else pair.substring(0, idx) to pair.substring(idx + 1)
            }
            .firstOrNull { it.first == key }
            ?.second
    }
}

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    val title: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_url") val authorUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

interface YouTubeOEmbedApi {
    @GET("oembed")
    suspend fun getOEmbed(
        @Query("url") url: String,
        @Query("format") format: String = "json"
    ): YouTubeOEmbedResponse
}

class YouTubeMetadataService {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val api: YouTubeOEmbedApi = Retrofit.Builder()
        .baseUrl("https://www.youtube.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(YouTubeOEmbedApi::class.java)

    suspend fun fetchMetadata(rawUrl: String): Result<YouTubeMetadata> = withContext(Dispatchers.IO) {
        when (val parsed = YouTubeUrlParser.parse(rawUrl)) {
            is YouTubeParseResult.Invalid -> Result.failure(IllegalArgumentException(parsed.errorMessage))
            is YouTubeParseResult.Valid -> {
                try {
                    val response = api.getOEmbed(parsed.canonicalUrl)
                    val title = response.title?.takeIf { it.isNotBlank() }
                        ?: "YouTube Video (${parsed.videoId})"
                    val author = response.authorName?.takeIf { it.isNotBlank() }
                        ?: "YouTube Creator"
                    val thumb = response.thumbnailUrl?.takeIf { it.isNotBlank() }
                        ?: "https://img.youtube.com/vi/${parsed.videoId}/hqdefault.jpg"
                    Result.success(
                        YouTubeMetadata(
                            videoId = parsed.videoId,
                            originalUrl = parsed.canonicalUrl,
                            title = title,
                            authorName = author,
                            thumbnailUrl = thumb,
                            authorUrl = response.authorUrl.orEmpty(),
                            width = response.width ?: 1280,
                            height = response.height ?: 720
                        )
                    )
                } catch (e: Exception) {
                    Result.failure(
                        IllegalStateException(
                            "Could not fetch metadata from YouTube oEmbed (${e.localizedMessage ?: "network error"}). Ensure the video is public and you have internet access."
                        )
                    )
                }
            }
        }
    }
}
