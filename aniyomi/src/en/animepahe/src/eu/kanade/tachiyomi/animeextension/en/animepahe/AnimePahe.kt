package eu.kanade.tachiyomi.animeextension.en.animepahe

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
import java.util.Base64

class AnimePahe : AnimeHttpSource() {

    override val name = "AnimePahe"

    override val baseUrl = "https://animepahe.ch"

    override val lang = "en"

    private val extractor = AnimePaheExtractor(client)

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Referer", baseUrl)

    // ─── Popular Anime ────────────────────────────────────────────────

    override fun popularAnimeRequest(page: Int): Request {
        val url = if (page == 1) "$baseUrl/series/" else "$baseUrl/series/page/$page/"
        return GET(url, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val animes = doc.select(popularListSelector).mapNotNull { popularFromElement(it) }
        val hasNext = doc.selectFirst("link[rel=next]") != null
        return AnimesPage(animes, hasNext)
    }

    // ─── Latest Updates ───────────────────────────────────────────────

    override fun latestUpdatesRequest(page: Int): Request {
        return popularAnimeRequest(page)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        return popularAnimeParse(response)
    }

    // ─── Search ───────────────────────────────────────────────────────

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = if (page == 1) {
            "$baseUrl/?s=$query"
        } else {
            "$baseUrl/page/$page/?s=$query"
        }
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        return popularAnimeParse(response)
    }

    // ─── Anime Details ────────────────────────────────────────────────

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val anime = SAnime.create()

        anime.title = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: ""

        anime.thumbnail_url = doc.selectFirst(".thumb img, .bigcover .ime img")?.attr("abs:src") ?: ""

        val descEl = doc.selectFirst(".ninfo > p, .entry-content p, .description p")
        anime.description = descEl?.text()?.trim() ?: ""

        val genres = doc.select(".genxed a").map { it.text().trim() }
        anime.genre = genres.joinToString(", ")

        val statusText = doc.selectFirst(".spe span:contains(Status)")?.text() ?: ""
        anime.status = when {
            statusText.contains("Ongoing", true) -> SAnime.ONGOING
            statusText.contains("Completed", true) -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }

        anime.initialized = true
        return anime
    }

    // ─── Episode List ─────────────────────────────────────────────────

    override fun episodeListParse(response: Response): List<SEpisode> {
        val doc = response.asJsoup()
        val episodes = mutableListOf<SEpisode>()

        doc.select(".bixbox.bxcl.epcheck .eplister ul li").forEach { item ->
            val link = item.selectFirst("a") ?: return@forEach
            val url = link.attr("abs:href")
            val epNumText = item.selectFirst(".epl-num")?.text()?.trim() ?: return@forEach
            val epNum = epNumText.toFloatOrNull() ?: return@forEach
            val title = item.selectFirst(".epl-title")?.text()?.trim() ?: "Episode ${epNum.toInt()}"

            val episode = SEpisode.create()
            episode.setUrlWithoutDomain(url)
            episode.name = title
            episode.episode_number = epNum
            episodes.add(episode)
        }

        return episodes.sortedByDescending { it.episode_number }
    }

    // ─── Videos ───────────────────────────────────────────────────────

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val episodeUrl = baseUrl + episode.url
        val response = client.newCall(GET(episodeUrl, headers)).execute()
        val doc = response.asJsoup()
        response.close()

        val allVideos = mutableListOf<Video>()
        doc.select(".gov-multipart .gov-the-embed").forEach { el ->
            val iframeUrl = resolveEmbedUrl(el)
            if (iframeUrl != null) {
                allVideos.addAll(extractor.videosFromUrl(iframeUrl, headers))
            }
        }

        if (allVideos.isEmpty()) {
            val fallbackIframe = doc.selectFirst("#embed_holder iframe")
            if (fallbackIframe != null) {
                val src = fallbackIframe.attr("abs:src")
                if (src.isNotBlank()) {
                    allVideos.addAll(extractor.videosFromUrl(src, headers))
                }
            }
        }

        return allVideos
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private val popularListSelector = ".listupd .bs"

    private fun popularFromElement(el: Element): SAnime? {
        val link = el.selectFirst("a") ?: return null
        val href = link.attr("abs:href")

        if (!href.contains("/category/") && !href.contains("/series/")) return null

        val anime = SAnime.create()
        anime.setUrlWithoutDomain(href)
        anime.title = el.selectFirst(".tt")?.ownText()?.trim() ?: ""
        anime.thumbnail_url = el.selectFirst(".limit img")?.attr("abs:src") ?: ""

        return anime
    }

    private fun resolveEmbedUrl(el: Element): String? {
        val onclick = el.attr("onclick") ?: return null
        val base64Regex = Regex("""putMi\s*\([^,]+,\s*'([^']+)'""")
        val match = base64Regex.find(onclick) ?: return null
        val encoded = match.groupValues[1]

        return try {
            val decoded = Base64.getDecoder().decode(encoded).decodeToString()
            val srcRegex = Regex("""src\s*=\s*["']([^"']+)["']""")
            val srcMatch = srcRegex.find(decoded)
            val src = srcMatch?.groupValues?.get(1)
            if (src != null && src.startsWith("//")) "https:$src" else src
        } catch (_: Exception) {
            null
        }
    }
}
