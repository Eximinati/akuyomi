package eu.kanade.tachiyomi.animeextension.en.pornhub

import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

class PornhubExtractor(private val client: OkHttpClient) {

    private val JSON_MEDIA = "application/json".toMediaType()

    fun videosFromPage(html: String): List<Video> {
        val videos = mutableListOf<Video>()

        videos.addAll(parseInitialState(html))
        if (videos.isNotEmpty()) return videos

        videos.addAll(parseNextData(html))
        if (videos.isNotEmpty()) return videos

        videos.addAll(parseQualityItems(html))
        if (videos.isNotEmpty()) return videos

        videos.addAll(parseMediaDefinitions(html))
        if (videos.isNotEmpty()) return videos

        videos.addAll(parseVideoTags(html))
        if (videos.isNotEmpty()) return videos

        videos.addAll(parseHlsUrls(html))

        return videos
    }

    fun videosFromApi(viewkey: String, headers: Headers): List<Video> {
        val videos = mutableListOf<Video>()

        videos.addAll(fetchMediaDefinitions(viewkey, headers))
        if (videos.isNotEmpty()) return videos

        videos.addAll(fetchEmbedVideo(viewkey, headers))

        return videos
    }

    private fun parseInitialState(html: String): List<Video> {
        val regex = Regex(
            """window\.__INITIAL_STATE__\s*=\s*(\{[\s\S]*?\});""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = regex.find(html) ?: return emptyList()

        return try {
            val root = JSONObject(match.groupValues[1])
            val videos = mutableListOf<Video>()

            val player = root.optJSONObject("player")
            if (player != null) {
                val definitions = player.optJSONArray("mediaDefinitions")
                if (definitions != null) {
                    videos.addAll(parseDefinitionArray(definitions))
                }
            }

            val videoData = root.optJSONObject("videoData")
            if (videoData != null) {
                val definitions = videoData.optJSONArray("mediaDefinitions")
                if (definitions != null) {
                    videos.addAll(parseDefinitionArray(definitions))
                }
            }

            videos
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNextData(html: String): List<Video> {
        val regex = Regex(
            """<script\s+id="__NEXT_DATA__"[^>]*type="application/json"[^>]*>(.*?)</script>""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = regex.find(html) ?: return emptyList()

        return try {
            val root = JSONObject(match.groupValues[1])
            val videos = mutableListOf<Video>()
            walkJsonForMediaDefinitions(root, videos)
            videos
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun walkJsonForMediaDefinitions(obj: JSONObject, out: MutableList<Video>) {
        val definitions = obj.optJSONArray("mediaDefinitions")
        if (definitions != null) {
            out.addAll(parseDefinitionArray(definitions))
            return
        }
        for (key in obj.keys()) {
            val child = obj.optJSONObject(key)
            if (child != null) walkJsonForMediaDefinitions(child, out)
            val childArr = obj.optJSONArray(key)
            if (childArr != null) {
                for (i in 0 until childArr.length()) {
                    val item = childArr.optJSONObject(i)
                    if (item != null) walkJsonForMediaDefinitions(item, out)
                }
            }
        }
    }

    private fun parseQualityItems(html: String): List<Video> {
        val videos = mutableListOf<Video>()

        val regex = Regex(
            """player_qualityItems\s*=\s*(\[[\s\S]*?\])\s*;""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = regex.find(html) ?: return videos

        return try {
            val jsonArray = JSONArray(match.groupValues[1])
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val quality = item.optString("quality", "")
                val videoUrl = item.optString("videoUrl", "")
                if (videoUrl.isNotBlank()) {
                    val label = quality.let {
                        when {
                            it.toIntOrNull() != null -> "${it}p"
                            else -> quality
                        }
                    }
                    videos.add(Video(videoUrl, label, videoUrl))
                }
            }
            videos
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseMediaDefinitions(html: String): List<Video> {
        val videos = mutableListOf<Video>()

        val regex = Regex(
            """mediaDefinitions\s*:\s*(\[[\s\S]*?\])\s*,""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = regex.find(html) ?: return videos

        return try {
            parseDefinitionArray(JSONArray(match.groupValues[1]))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDefinitionArray(jsonArray: JSONArray): List<Video> {
        val videos = mutableListOf<Video>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val quality = item.optString("quality", "")
            val videoUrl = item.optString("videoUrl", "")
            val url = item.optString("url", "")
            val defaultUrl = item.optString("defaultUrl", "")
            val finalUrl = videoUrl.ifBlank { url.ifBlank { defaultUrl } }

            if (finalUrl.isNotBlank()) {
                val label = when {
                    quality.toIntOrNull() != null -> "${quality}p"
                    quality.contains("1080", true) -> "1080p"
                    quality.contains("720", true) -> "720p"
                    quality.contains("480", true) -> "480p"
                    quality.contains("360", true) -> "360p"
                    quality.contains("240", true) -> "240p"
                    else -> quality
                }
                videos.add(Video(finalUrl, label, finalUrl))
            }
        }
        return videos
    }

    private fun parseVideoTags(html: String): List<Video> {
        val videos = mutableListOf<Video>()
        val doc = Jsoup.parse(html)

        doc.select("video source[src]").forEach { source ->
            val src = source.attr("abs:src")
            val type = source.attr("type")
            if (src.isNotBlank()) {
                val label = if (type.contains("m3u8", true)) "HLS" else extractQualityFromUrl(src) ?: "Unknown"
                videos.add(Video(src, label, src))
            }
        }

        doc.select("video[src]").forEach { video ->
            val src = video.attr("abs:src")
            if (src.isNotBlank() && videos.none { it.url == src }) {
                val label = extractQualityFromUrl(src) ?: "Unknown"
                videos.add(Video(src, label, src))
            }
        }

        return videos
    }

    private fun parseHlsUrls(html: String): List<Video> {
        val videos = mutableListOf<Video>()
        val hlsRegex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
        val matches = hlsRegex.findAll(html).toList().distinct()

        for (match in matches) {
            val url = match.value.trimEnd(')', ',', '"', '\'', '>', '<')
            val label = extractQualityFromUrl(url) ?: "HLS"
            if (videos.none { it.url == url }) {
                videos.add(Video(url, label, url))
            }
        }

        return videos
    }

    private fun fetchMediaDefinitions(viewkey: String, headers: Headers): List<Video> {
        return try {
            val body = "video_id=$viewkey".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url("https://www.pornhub.com/media/definitions/")
                .headers(headers)
                .addHeader("Referer", "https://www.pornhub.com/view_video.php?viewkey=$viewkey")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val json = response.body.string()
            response.close()

            val videos = mutableListOf<Video>()

            val root = JSONObject(json)
            val definitions = root.optJSONArray("mediaDefinitions")
            if (definitions != null) {
                videos.addAll(parseDefinitionArray(definitions))
            }

            val videoUrl = root.optString("videoUrl", "")
            val defaultQuality = root.optString("defaultQuality", "720")
            if (videoUrl.isNotBlank() && videos.none { it.url == videoUrl }) {
                videos.add(Video(videoUrl, "${defaultQuality}p", videoUrl))
            }

            videos
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchEmbedVideo(viewkey: String, headers: Headers): List<Video> {
        return try {
            val request = Request.Builder()
                .url("https://www.pornhub.com/embed/$viewkey")
                .headers(headers)
                .addHeader("Referer", "https://www.pornhub.com/view_video.php?viewkey=$viewkey")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body.string()
            response.close()

            val videos = mutableListOf<Video>()

            videos.addAll(parseQualityItems(html))
            if (videos.isNotEmpty()) return videos

            videos.addAll(parseMediaDefinitions(html))
            if (videos.isNotEmpty()) return videos

            videos.addAll(parseVideoTags(html))
            if (videos.isNotEmpty()) return videos

            videos.addAll(parseHlsUrls(html))

            videos
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun extractQualityFromUrl(url: String): String? {
            val patterns = listOf(
                Regex("""(\d{3,4})[pP]"""),
                Regex("""_(\d{3,4})_"""),
                Regex("""(\d{3,4})x(\d{3,4})"""),
                Regex("""height[=/](\d{3,4})"""),
            )

            for (pattern in patterns) {
                val match = pattern.find(url)
                if (match != null) {
                    val value = match.groupValues[1].toIntOrNull() ?: continue
                    return when {
                        value >= 2160 -> "2160p"
                        value >= 1080 -> "1080p"
                        value >= 720 -> "720p"
                        value >= 480 -> "480p"
                        value >= 360 -> "360p"
                        value >= 240 -> "240p"
                        else -> "${value}p"
                    }
                }
            }

            return null
        }
    }
}
