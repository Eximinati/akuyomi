package eu.kanade.tachiyomi.animeextension.en.porn00

import eu.kanade.tachiyomi.animesource.model.Video
import org.jsoup.Jsoup

class Porn00Extractor {

    fun videosFromPage(html: String): List<Video> {
        val videos = mutableListOf<Video>()

        videos.addAll(parseFlashvars(html))
        if (videos.isNotEmpty()) return videos

        videos.addAll(parseVideoTags(html))

        return videos
    }

    private fun parseFlashvars(html: String): List<Video> {
        val flashvarsRegex = Regex(
            """var\s+flashvars\s*=\s*\{([\s\S]*?)\};""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = flashvarsRegex.find(html) ?: return emptyList()

        val pairs = mutableMapOf<String, String>()

        val kvRegex = Regex(
            """(\w+)\s*:\s*['"]([^'"]*)['"]""",
        )
        for (kv in kvRegex.findAll(match.groupValues[1])) {
            pairs[kv.groupValues[1]] = kv.groupValues[2]
        }

        val videos = mutableListOf<Video>()

        fun cleanUrl(raw: String): String {
            val idx = raw.indexOf("https://")
            return if (idx >= 0) raw.substring(idx) else raw
        }

        val mainUrl = pairs["video_url"] ?: ""
        val mainText = pairs["video_url_text"] ?: "360p"
        if (mainUrl.isNotBlank() && mainUrl.contains("https://")) {
            val url = cleanUrl(mainUrl)
            if (url.isNotBlank()) {
                val label = mainText.let {
                    when {
                        it.toIntOrNull() != null -> "${it}p"
                        else -> it
                    }
                }
                videos.add(Video(url, label, url))
            }
        }

        val altUrl = pairs["video_alt_url"] ?: ""
        val altText = pairs["video_alt_url_text"] ?: "720p"
        if (altUrl.isNotBlank() && altUrl.contains("https://") && altUrl != mainUrl) {
            val url = cleanUrl(altUrl)
            if (url.isNotBlank()) {
                val label = altText.let {
                    when {
                        it.toIntOrNull() != null -> "${it}p"
                        else -> it
                    }
                }
                if (videos.none { it.url == url }) {
                    videos.add(Video(url, label, url))
                }
            }
        }

        var i = 2
        while (true) {
            val keyUrl = "video_alt_url$i"
            val keyText = "video_alt_url${i}_text"
            val urlRaw = pairs[keyUrl] ?: break
            val text = pairs[keyText] ?: "360p"

            if (urlRaw.contains("https://")) {
                val url = cleanUrl(urlRaw)
                if (url.isNotBlank() && videos.none { it.url == url }) {
                    val label = text.let {
                        when {
                            it.toIntOrNull() != null -> "${it}p"
                            else -> it
                        }
                    }
                    videos.add(Video(url, label, url))
                }
            }
            i++
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
                val label = if (type.contains("m3u8", true)) "HLS" else "Unknown"
                if (videos.none { it.url == src }) {
                    videos.add(Video(src, label, src))
                }
            }
        }

        doc.select("video[src]").forEach { video ->
            val src = video.attr("abs:src")
            if (src.isNotBlank() && videos.none { it.url == src }) {
                videos.add(Video(src, "Unknown", src))
            }
        }

        return videos
    }
}
