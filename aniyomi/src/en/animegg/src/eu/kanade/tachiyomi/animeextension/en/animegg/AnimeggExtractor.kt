package eu.kanade.tachiyomi.animeextension.en.animegg

import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

class AnimeggExtractor(private val client: OkHttpClient) {

    fun videosFromUrl(url: String, headers: Headers): List<Video> {
        return try {
            val response = client.newCall(
                Request.Builder()
                    .url(url)
                    .headers(headers)
                    .addHeader("Referer", "https://www.animegg.org/")
                    .build(),
            ).execute()
            val body = response.body.string()
            response.close()
            parseVideoSources(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseVideoSources(html: String): List<Video> {
        val videos = mutableListOf<Video>()

        val sourceRegex = Regex(
            """videoSources\s*=\s*\[([\s\S]*?)\]\s*;""",
            setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
        )
        val match = sourceRegex.find(html)
        if (match != null) {
            val sourcesStr = match.groupValues[1]

            val fileRegex = Regex("""file\s*:\s*"([^"]+)"""")
            val labelRegex = Regex("""label\s*:\s*"([^"]+)"""")

            val files = fileRegex.findAll(sourcesStr).map { it.groupValues[1] }.toList()
            val labels = labelRegex.findAll(sourcesStr).map { it.groupValues[1] }.toList()

            for (i in files.indices) {
                val fileUrl = files[i]
                val label = labels.getOrElse(i) { "Unknown" }
                val fullUrl = if (fileUrl.startsWith("http")) fileUrl else "https://www.animegg.org$fileUrl"
                videos.add(Video(fullUrl, label, fullUrl))
            }
        }

        if (videos.isEmpty()) {
            val singleFileRegex = Regex("""file\s*:\s*"([^"]+)"""")
            val singleMatch = singleFileRegex.find(html)
            if (singleMatch != null) {
                val fileUrl = singleMatch.groupValues[1]
                val fullUrl = if (fileUrl.startsWith("http")) fileUrl else "https://www.animegg.org$fileUrl"
                videos.add(Video(fullUrl, "Unknown", fullUrl))
            }
        }

        return videos
    }
}
