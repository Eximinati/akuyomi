package eu.kanade.tachiyomi.animeextension.en.animepahe

import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

class AnimePaheExtractor(private val client: OkHttpClient) {

    fun videosFromUrl(url: String, headers: Headers): List<Video> {
        return try {
            val response = client.newCall(
                okhttp3.Request.Builder()
                    .url(url)
                    .headers(headers)
                    .build(),
            ).execute()
            val body = response.body.string()
            response.close()
            videosFromResponse(body, url, headers)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun videosFromResponse(body: String, url: String, headers: Headers): List<Video> {
        val videos = mutableListOf<Video>()

        val jwVideos = parseJwPlayer(body)
        if (jwVideos.isNotEmpty()) {
            videos.addAll(jwVideos)
        }

        if (videos.isEmpty()) {
            val directVideos = parseDirectSources(body)
            videos.addAll(directVideos)
        }

        if (videos.isEmpty()) {
            val hlsVideos = parseHlsManifest(body)
            videos.addAll(hlsVideos)
        }

        if (videos.isEmpty()) {
            val okVideos = parseOkRuVideos(body)
            videos.addAll(okVideos)
        }

        if (videos.isEmpty()) {
            val iframeUrl = parseIframeUrl(body)
            if (iframeUrl != null) {
                val resolved = resolveIframe(iframeUrl, headers)
                if (resolved.isNotEmpty()) return resolved
            }
        }

        return videos
    }

    private fun parseJwPlayer(html: String): List<Video> {
        val videos = mutableListOf<Video>()
        val subtitles = parseSubtitles(html)

        val sourceRegex = Regex(
            """sources:\s*\[([\s\S]*?)\]""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val sourceMatch = sourceRegex.find(html)
        if (sourceMatch != null) {
            val sourcesStr = sourceMatch.groupValues[1]
            val fileRegex = Regex("""file:\s*"([^"]+)"""")
            val labelRegex = Regex("""label:\s*"([^"]+)"""")
            val files = fileRegex.findAll(sourcesStr).map { it.groupValues[1] }.toList()
            val labels = labelRegex.findAll(sourcesStr).map { it.groupValues[1] }.toList()

            for (i in files.indices) {
                val label = labels.getOrElse(i) { "Unknown" }
                videos.add(
                    Video(files[i], label, files[i], subtitleTracks = subtitles ?: emptyList()),
                )
            }
        }

        if (videos.isEmpty()) {
            val singleFileRegex = Regex("""file\s*:\s*"([^"]+)"""")
            val match = singleFileRegex.find(html)
            if (match != null) {
                val file = match.groupValues[1]
                val label = extractQuality(file) ?: "Unknown"
                videos.add(
                    Video(file, label, file, subtitleTracks = subtitles ?: emptyList()),
                )
            }
        }

        return videos
    }

    private fun parseSubtitles(html: String): List<Track>? {
        val tracks = mutableListOf<Track>()
        val trackRegex = Regex(
            """tracks:\s*\[([\s\S]*?)\]""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val trackMatch = trackRegex.find(html)
        if (trackMatch != null) {
            val tracksStr = trackMatch.groupValues[1]
            val fileRegex = Regex("""file:\s*"([^"]+)"""")
            val labelRegex = Regex("""label:\s*"([^"]+)"""")
            val kindRegex = Regex("""kind:\s*"([^"]+)"""")
            val files = fileRegex.findAll(tracksStr).map { it.groupValues[1] }.toList()
            val labels = labelRegex.findAll(tracksStr).map { it.groupValues[1] }.toList()
            val kinds = kindRegex.findAll(tracksStr).map { it.groupValues[1] }.toList()

            for (i in files.indices) {
                val kind = kinds.getOrElse(i) { "" }
                if (kind.contains("captions", true) || kind.contains("subtitles", true)) {
                    val lang = labels.getOrElse(i) { "English" }
                    tracks.add(Track(files[i], lang))
                }
            }
        }
        return tracks.ifEmpty { null }
    }

    private fun parseDirectSources(html: String): List<Video> {
        val videos = mutableListOf<Video>()
        val doc = Jsoup.parse(html)

        doc.select("source, video source").forEach { source ->
            val src = source.attr("abs:src").ifEmpty {
                source.parent()?.attr("abs:src") ?: return@forEach
            }
            val type = source.attr("type")
            val quality = when {
                type.contains("m3u8", true) -> "HLS"
                type.contains("mp4", true) -> extractQuality(src) ?: "Unknown"
                else -> extractQuality(src) ?: "Unknown"
            }
            videos.add(Video(src, quality, src))
        }

        doc.select("video[src]").forEach { video ->
            val src = video.attr("abs:src")
            if (videos.none { it.url == src }) {
                val quality = extractQuality(src) ?: "Unknown"
                videos.add(Video(src, quality, src))
            }
        }

        return videos
    }

    private fun parseIframeUrl(html: String): String? {
        val doc = Jsoup.parse(html)
        val iframe = doc.selectFirst("iframe[src]") ?: return null
        val src = iframe.attr("abs:src")
        return src.takeIf { it.isNotBlank() }
    }

    private fun resolveIframe(url: String, originalHeaders: Headers): List<Video> {
        return try {
            val response = client.newCall(
                okhttp3.Request.Builder()
                    .url(url)
                    .headers(originalHeaders)
                    .build(),
            ).execute()
            val body = response.body.string()
            response.close()

            val videos = parseJwPlayer(body)
            if (videos.isNotEmpty()) return videos

            val directVideos = parseDirectSources(body)
            if (directVideos.isNotEmpty()) return directVideos

            val m3u8Videos = parseM3u8(body)
            if (m3u8Videos.isNotEmpty()) return m3u8Videos

            listOf(Video(url, "Unknown", url))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseM3u8(body: String): List<Video> {
        val videos = mutableListOf<Video>()
        val m3u8Regex = Regex("""https?://[^\s"']+\.m3u8[^\s"']*""")
        val matches = m3u8Regex.findAll(body).toList().distinct()

        for (match in matches) {
            val url = match.value.trimEnd(')', ',', '"', '\'', '>')
            val quality = extractQuality(url) ?: "HLS"
            videos.add(Video(url, quality, url))
        }

        return videos
    }

    private fun parseHlsManifest(html: String): List<Video> {
        // OK.ru uses \&quot; (backslash + HTML entity) instead of plain quotes
        val regex = Regex(
            """hlsManifestUrl\\?&quot;\s*:\s*\\?&quot;(https?://.+?)\\?&quot;""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val match = regex.find(html) ?: return emptyList()
        val url = sanitizeOkUrl(match.groupValues[1])
        val quality = extractQuality(url) ?: "HLS"
        return listOf(Video(url, quality, url))
    }

    private fun parseOkRuVideos(html: String): List<Video> {
        val videos = mutableListOf<Video>()
        val arrayRegex = Regex(
            """videos\\?&quot;\s*:\s*\[(.*?)\]""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val arrayMatch = arrayRegex.find(html) ?: return emptyList()
        val arrayStr = arrayMatch.groupValues[1]

        val nameRegex = Regex("""name\\?&quot;\s*:\s*\\?&quot;([^"\\&]+?)\\?&quot;""")
        val urlRegex = Regex(
            """url\\?&quot;\s*:\s*\\?&quot;(https?://.+?)\\?&quot;""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val names = nameRegex.findAll(arrayStr).map { it.groupValues[1] }.toList()
        val urls = urlRegex.findAll(arrayStr).map { sanitizeOkUrl(it.groupValues[1]) }.toList()

        val qualityMap = mapOf(
            "mobile" to "360p",
            "lowest" to "480p",
            "low" to "480p",
            "sd" to "720p",
            "hd" to "1080p",
            "full" to "1080p",
        )

        for (i in urls.indices) {
            val name = names.getOrElse(i) { "Unknown" }
            val quality = qualityMap[name] ?: name
            videos.add(Video(urls[i], quality, urls[i]))
        }

        return videos
    }

    private fun sanitizeOkUrl(url: String): String {
        return url.replace("\\\\u0026", "&").replace("\\u0026", "&").replace("\\/", "/")
    }

    companion object {
        fun extractQuality(url: String): String? {
            val qualityPatterns = listOf(
                Regex("""(\d{3,4})[pP]"""),
                Regex("""(\d{3,4})x(\d{3,4})"""),
                Regex("""_(\d{3,4})_"""),
            )

            for (pattern in qualityPatterns) {
                val match = pattern.find(url)
                if (match != null) {
                    val value = match.groupValues[1].toIntOrNull() ?: continue
                    return when {
                        value >= 2160 -> "2160p"
                        value >= 1080 -> "1080p"
                        value >= 720 -> "720p"
                        value >= 480 -> "480p"
                        value >= 360 -> "360p"
                        else -> "${value}p"
                    }
                }
            }

            return null
        }
    }
}
