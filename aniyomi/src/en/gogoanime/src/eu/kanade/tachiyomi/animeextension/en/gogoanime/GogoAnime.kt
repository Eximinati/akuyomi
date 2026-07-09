package eu.kanade.tachiyomi.animeextension.en.gogoanime

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class GogoAnime : AnimeHttpSource() {

    override val name = "GogoAnime"

    override val baseUrl = "https://gogoanime.by"

    override val lang = "en"

    private val extractor = GogoExtractor(client)

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

        doc.select(".episodes-container .episode-item").forEach { item ->
            val link = item.selectFirst("a") ?: return@forEach
            val url = link.attr("abs:href")
            val epNum = item.attr("data-episode-number").toFloatOrNull() ?: return@forEach

            val episode = SEpisode.create()
            episode.setUrlWithoutDomain(url)
            episode.name = "Episode ${epNum.toInt()}"
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
        doc.select("#w-servers .player-type-link").forEach { el ->
            val serverUrl = resolveServerUrl(el, doc)
            if (serverUrl != null) {
                val serverResponse = client.newCall(GET(serverUrl, headers)).execute()
                val body = serverResponse.body.string()
                serverResponse.close()
                allVideos.addAll(extractor.videosFromResponse(body, serverUrl, headers))
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

    private fun extractEpisodeNumber(text: String, url: String): Float {
        val numRegex = Regex("""(\d+)""")
        val attr = try {
            val docUrl = url.toHttpUrlOrNull()
            docUrl?.pathSegments?.firstOrNull { it.contains("episode") }
                ?.let { numRegex.find(it)?.groupValues?.get(1) }
        } catch (_: Exception) { null }

        return attr?.toFloatOrNull()
            ?: numRegex.find(text)?.groupValues?.get(1)?.toFloatOrNull()
            ?: 1f
    }

    private fun resolveServerUrl(el: Element, doc: Document): String? {
        val plainUrl = el.attr("data-plain-url")
        if (plainUrl.isNotEmpty()) return plainUrl

        if (el.hasAttr("data-encrypted-url1")) {
            return try {
                val type = el.attr("data-type")
                val url1 = el.attr("data-encrypted-url1")
                val url2 = el.attr("data-encrypted-url2")
                val url3 = el.attr("data-encrypted-url3")
                val ref = el.attr("data-ref").ifEmpty { "gogoanime.by" }
                val postId = doc.selectFirst("article")?.id()?.removePrefix("post-") ?: ""

                val proxyUrl = HttpUrl.Builder().apply {
                    scheme("https")
                    host("9animetv.be")
                    encodedPath("/wp-content/plugins/video-player/includes/player/player.php")
                    addQueryParameter(type, url1)
                    addQueryParameter("url2", url2)
                    addQueryParameter("url3", url3)
                    addQueryParameter("ref", ref)
                    addQueryParameter("post_id", postId)
                }.build()

                val proxyResponse = client.newCall(GET(proxyUrl.toString(), headers)).execute()
                val proxyDoc = proxyResponse.body.string()
                proxyResponse.close()

                val iframeRegex = Regex("""<iframe[^>]+src=["']([^"']+)["']""")
                val match = iframeRegex.find(proxyDoc)
                match?.groupValues?.get(1)
            } catch (_: Exception) {
                null
            }
        }

        return null
    }
}
