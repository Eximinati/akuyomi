package eu.kanade.tachiyomi.animeextension.en.animegg

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

class Animegg : AnimeHttpSource() {

    override val name = "Animegg"

    override val baseUrl = "https://www.animegg.org"

    override val lang = "en"

    private val extractor = AnimeggExtractor(client)

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Referer", baseUrl)

    // ─── Popular Anime ────────────────────────────────────────────────

    override fun popularAnimeRequest(page: Int): Request {
        val offset = (page - 1) * 25
        val url = "$baseUrl/popular-series?sortBy=hits&sortDirection=DESC&limit=25&start=$offset"
        return GET(url, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val animes = doc.select(popularListSelector).mapNotNull { popularFromElement(it) }
        val hasNext = doc.selectFirst("ul.pagination a:contains(Next)") != null
        return AnimesPage(animes, hasNext)
    }

    // ─── Latest Updates ───────────────────────────────────────────────

    override fun latestUpdatesRequest(page: Int): Request {
        val offset = (page - 1) * 25
        return GET("$baseUrl/releases?start=$offset", headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val animes = mutableListOf<SAnime>()
        doc.select("li.fea.release").forEach { item ->
            val seriesLink = item.selectFirst("a.releaseLink") ?: return@forEach
            val slug = seriesLink.attr("href")
            val anime = SAnime.create()
            anime.setUrlWithoutDomain(slug)
            anime.title = seriesLink.text().trim()
            anime.thumbnail_url = item.selectFirst(".releaseImg img")?.attr("abs:src") ?: ""
            animes.add(anime)
        }
        val hasNext = doc.selectFirst("ul.pagination a:contains(Next)") != null
        return AnimesPage(animes, hasNext)
    }

    // ─── Search ───────────────────────────────────────────────────────

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$baseUrl/search?q=$query", headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val animes = doc.select("a.mse").mapNotNull { el ->
            val link = el.attr("abs:href")
            if (link.isBlank() || !link.contains("/series/")) return@mapNotNull null
            val anime = SAnime.create()
            anime.setUrlWithoutDomain(link)
            anime.title = el.selectFirst("h2")?.text()?.trim() ?: ""
            anime.thumbnail_url = el.selectFirst(".media-object")?.attr("abs:src") ?: ""
            anime
        }
        return AnimesPage(animes, false)
    }

    // ─── Anime Details ────────────────────────────────────────────────

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val anime = SAnime.create()

        anime.title = doc.selectFirst("div.first h1")?.text()?.trim() ?: ""

        anime.thumbnail_url = doc.selectFirst("a.pull-left img.media-object")?.attr("abs:src") ?: ""

        anime.description = doc.selectFirst("p.ptext")?.text()?.trim() ?: ""

        val genres = doc.select("ul.tagscat li a").map { it.text().trim() }
        anime.genre = genres.joinToString(", ")

        val statusText = doc.selectFirst("p.infoami span:contains(Status)")?.text() ?: ""
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

        doc.select("ul.newmanga li").forEach { item ->
            val link = item.selectFirst("a.anm_det_pop") ?: return@forEach
            val href = link.attr("href")
            val epText = item.selectFirst("i.anititle")?.text() ?: return@forEach

            val epNum = epText.removePrefix("Episode ").toFloatOrNull() ?: return@forEach

            val episode = SEpisode.create()
            episode.setUrlWithoutDomain(href)
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

        val iframe = doc.selectFirst(".embed-responsive iframe[src]") ?: return emptyList()
        val embedUrl = iframe.attr("abs:src")
        if (embedUrl.isBlank()) return emptyList()

        return extractor.videosFromUrl(embedUrl, headers)
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private val popularListSelector = "ul.popanime.cats > li.fea"

    private fun popularFromElement(el: Element): SAnime? {
        val link = el.selectFirst("div.rightpop > a") ?: return null
        val href = link.attr("href")
        if (href.isBlank() || !href.contains("/series/")) return null

        val anime = SAnime.create()
        anime.setUrlWithoutDomain(href)
        anime.title = link.text().trim()
        anime.thumbnail_url = el.selectFirst(".img img")?.attr("abs:src") ?: ""
        return anime
    }
}
