package eu.kanade.tachiyomi.animeextension.en.pornhub

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element

class Pornhub : AnimeHttpSource() {

    override val name = "Pornhub"

    override val baseUrl = "https://www.pornhub.com"

    override val lang = "en"

    private val extractor = PornhubExtractor(client)

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Referer", baseUrl)

    // ─── Popular ─────────────────────────────────────────────────────

    override fun popularAnimeRequest(page: Int): Request {
        val url = if (page == 1) "$baseUrl/video" else "$baseUrl/video?page=$page"
        return GET(url, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val animes = doc.select(videoCardSelector).mapNotNull { videoCard -> videoCardToAnime(videoCard) }
        val hasNext = doc.selectFirst(loadMoreSelector) != null
        return AnimesPage(animes, hasNext)
    }

    // ─── Latest Updates ──────────────────────────────────────────────

    override fun latestUpdatesRequest(page: Int): Request {
        return popularAnimeRequest(page)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        return popularAnimeParse(response)
    }

    // ─── Search ──────────────────────────────────────────────────────

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = if (page == 1) {
            "$baseUrl/video/search?search=$query"
        } else {
            "$baseUrl/video/search?search=$query&page=$page"
        }
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        return popularAnimeParse(response)
    }

    // ─── Anime Details ───────────────────────────────────────────────

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val anime = SAnime.create()

        anime.title = doc.selectFirst("h1.title")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim()
            ?: ""

        anime.thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".video-cover img")?.attr("abs:src")
            ?: ""

        val descEl = doc.selectFirst("div.videoDescription")
        anime.description = descEl?.text()?.trim()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: ""

        val tags = doc.select("div.video-tags a, div.tags a").map { it.text().trim() }
        anime.genre = tags.joinToString(", ")

        anime.initialized = true
        return anime
    }

    // ─── Episode List ────────────────────────────────────────────────

    override fun episodeListParse(response: Response): List<SEpisode> {
        val doc = response.asJsoup()
        val viewkey = doc.selectFirst("meta[property=og:url]")?.attr("content")
            ?.substringAfter("viewkey=")
            ?: response.request.url.queryParameter("viewkey")
            ?: ""

        val episode = SEpisode.create()
        episode.setUrlWithoutDomain("/view_video.php?viewkey=$viewkey")
        episode.name = "Video"
        episode.episode_number = 1f

        return listOf(episode)
    }

    // ─── Videos ──────────────────────────────────────────────────────

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val episodeUrl = baseUrl + episode.url
        val viewkey = episode.url.substringAfter("viewkey=").substringBefore("&")
        val response = client.newCall(GET(episodeUrl, headers)).execute()
        val body = response.body.string()
        response.close()

        val videos = extractor.videosFromPage(body)

        if (videos.isEmpty()) {
            val apiVideos = extractor.videosFromApi(viewkey, headers)
            if (apiVideos.isNotEmpty()) return apiVideos.sortedByDescending { parseQuality(it.quality) }
            return emptyList()
        }

        return videos.sortedByDescending { parseQuality(it.quality) }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private val videoCardSelector = "li.videoblock, div.videoblock, li.pcVideoListItem, div.videoWrapper"

    private val loadMoreSelector = "a.nextPage, a[rel=next], li.page_next a"

    private fun videoCardToAnime(el: Element): SAnime? {
        val link = el.selectFirst("a[href*=viewkey]") ?: return null
        val href = link.attr("href")
        val viewkey = href.substringAfter("viewkey=").substringBefore("&")
        if (viewkey.isBlank()) return null

        val anime = SAnime.create()
        anime.setUrlWithoutDomain("/view_video.php?viewkey=$viewkey")
        anime.title = link.attr("title").ifBlank {
            el.selectFirst(".title, .videoTitle")?.text()?.trim() ?: "Video $viewkey"
        }
        anime.thumbnail_url = el.selectFirst("img")?.attr("abs:src") ?: ""
        return anime
    }

    private fun parseQuality(quality: String): Int {
        val num = quality.replace(Regex("[^0-9]"), "")
        return num.toIntOrNull() ?: 0
    }
}
