package eu.kanade.tachiyomi.animeextension.en.porn00

import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
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

class Porn00 : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "Porn00"

    override val baseUrl = "https://www.porn00.org"

    override val lang = "en"

    private val extractor = Porn00Extractor()

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Referer", baseUrl)

    // ─── Preferences ──────────────────────────────────────────────────

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val qualityPref = ListPreference(screen.context).apply {
            key = "preferred_quality"
            title = "Preferred quality"
            entries = arrayOf("Auto (highest)", "2160p (4K)", "1080p", "720p", "480p", "360p")
            entryValues = arrayOf("auto", "2160", "1080", "720", "480", "360")
            setDefaultValue("auto")
            summary = "%s"
        }
        screen.addPreference(qualityPref)
    }

    // ─── Popular (Most Viewed) ────────────────────────────────────────

    override fun popularAnimeRequest(page: Int): Request {
        val url = if (page == 1) "$baseUrl/popular-vids/" else "$baseUrl/popular-vids/$page/"
        return GET(url, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        return parseVideoListing(response)
    }

    // ─── Latest Updates ──────────────────────────────────────────────

    override fun latestUpdatesRequest(page: Int): Request {
        val url = if (page == 1) "$baseUrl/latest-vids/" else "$baseUrl/latest-vids/$page/"
        return GET(url, headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        return parseVideoListing(response)
    }

    // ─── Search ──────────────────────────────────────────────────────

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val q = query.replace(" ", "+")
        val url = "$baseUrl/searching/$q/"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        return parseVideoListing(response)
    }

    // ─── Anime Details ───────────────────────────────────────────────

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val anime = SAnime.create()

        anime.title = doc.selectFirst("h1")?.text()?.trim()
            ?: doc.selectFirst("title")?.text()?.replace(" - Porn00", "")?.trim()
            ?: ""

        anime.thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: ""

        val descEl = doc.selectFirst("meta[property=og:description]")
        anime.description = descEl?.attr("content")?.trim() ?: ""

        val cats = doc.select("a[href*='/category-name/']").map { it.text().trim() }
        val stars = doc.select("a[href*='/star-name/']").map { it.text().trim() }
        anime.genre = (cats + stars).joinToString(", ")

        anime.initialized = true
        return anime
    }

    // ─── Episode List ────────────────────────────────────────────────

    override fun episodeListParse(response: Response): List<SEpisode> {
        val doc = response.asJsoup()
        val slug = doc.selectFirst("link[rel=canonical]")?.attr("href")
            ?.substringAfter("/video/")?.trimEnd('/')
            ?: response.request.url.pathSegments.takeIf { it.size >= 2 }?.let {
                if (it[0] == "video") it[1] else null
            }
            ?: ""

        val episode = SEpisode.create()
        episode.setUrlWithoutDomain("/video/$slug/")
        episode.name = "Video"
        episode.episode_number = 1f

        return listOf(episode)
    }

    // ─── Videos ──────────────────────────────────────────────────────

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val episodeUrl = baseUrl + episode.url
        val response = client.newCall(GET(episodeUrl, headers)).execute()
        val body = response.body.string()
        response.close()

        val videos = extractor.videosFromPage(body)

        return videos.sortedByDescending { parseQuality(it.quality) }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private val videoCardSelector = "div.item"

    private val loadMoreSelector = "a[rel=next]"

    private fun parseVideoListing(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val animes = doc.select(videoCardSelector).mapNotNull { videoCardToAnime(it) }
        val hasNext = doc.selectFirst(loadMoreSelector) != null
        return AnimesPage(animes, hasNext)
    }

    private fun videoCardToAnime(el: Element): SAnime? {
        val link = el.selectFirst("a") ?: return null
        val href = link.attr("href")

        val slug = href.trim('/').substringAfterLast("/video/").substringAfter("video/")
        if (slug.isBlank()) return null

        val anime = SAnime.create()
        anime.setUrlWithoutDomain("/video/$slug/")
        anime.title = link.attr("title").ifBlank {
            link.selectFirst("strong.title")?.text()?.trim() ?: slug
        }
        anime.thumbnail_url = link.selectFirst("img.thumb")?.attr("data-original")
            ?: link.selectFirst("img")?.attr("data-original")
            ?: link.selectFirst("img")?.attr("src")
            ?: ""
        return anime
    }

    private fun parseQuality(quality: String): Int {
        val num = quality.replace(Regex("[^0-9]"), "")
        return num.toIntOrNull() ?: 0
    }
}
