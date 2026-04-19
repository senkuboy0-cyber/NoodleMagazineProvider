package com.noodlemagazine

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

class NoodleMagazineProvider : MainAPI() {
    override var mainUrl = "https://noodlemagazine.com"
    override var name = "NoodleMagazine"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.Others)

    private val ua = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to "https://noodlemagazine.com/"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/video/japanese" to "Japanese",
        "$mainUrl/video/milf" to "MILF",
        "$mainUrl/video/teen" to "Teen",
        "$mainUrl/video/anal" to "Anal",
        "$mainUrl/video/lesbian" to "Lesbian",
        "$mainUrl/video/amateur" to "Amateur",
        "$mainUrl/video/asian" to "Asian",
        "$mainUrl/video/blowjob" to "Blowjob",
        "$mainUrl/video/creampie" to "Creampie",
        "$mainUrl/video/hentai" to "Hentai",
        "$mainUrl/video/indian" to "Indian",
        "$mainUrl/video/ebony" to "Ebony",
        "$mainUrl/now" to "Watching Now",
        "$mainUrl/new-video" to "New Videos",
        "$mainUrl/popular/month" to "Popular",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data
                  else "${request.data}?page=$page"

        val doc = app.get(url, headers = ua).document

        val items = doc.select("a.item_link[href*='/watch/']").mapNotNull { a ->
            val href = a.attr("abs:href").ifBlank { return@mapNotNull null }
            val title = a.attr("title").trim().ifBlank {
                a.selectFirst("img")?.attr("alt")?.trim()
            } ?: return@mapNotNull null
            // poster: preview_800.jpg pattern থেকে বের করি
            val id = href.substringAfterLast("/watch/")
            val parts = id.split("_")
            val poster = if (parts.size == 2) {
                "https://cdn2.pvvstream.pro/videos/${parts[0]}/${parts[1]}/preview_320.jpg"
            } else null

            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items, page < 20)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val doc = app.get("$mainUrl/video/$encoded", headers = ua).document
        return doc.select("a.item_link[href*='/watch/']").mapNotNull { a ->
            val href = a.attr("abs:href").ifBlank { return@mapNotNull null }
            val title = a.attr("title").trim().ifBlank {
                a.selectFirst("img")?.attr("alt")?.trim()
            } ?: return@mapNotNull null
            val id = href.substringAfterLast("/watch/")
            val parts = id.split("_")
            val poster = if (parts.size == 2) {
                "https://cdn2.pvvstream.pro/videos/${parts[0]}/${parts[1]}/preview_320.jpg"
            } else null
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = ua).document

        val title = doc.selectFirst("h1")?.text()?.trim()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
            ?: url.substringAfterLast("/")

        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: run {
                val id = url.substringAfterLast("/watch/")
                val parts = id.split("_")
                if (parts.size == 2)
                    "https://cdn2.pvvstream.pro/videos/${parts[0]}/${parts[1]}/preview_800.jpg"
                else null
            }

        val description = doc.selectFirst("meta[name=description]")?.attr("content")
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.isBlank()) return false

        val html = app.get(data, headers = ua).text

        // window.playlist = {...} থেকে sources বের করো
        val playlistMatch = Regex("""window\.playlist\s*=\s*(\{.+?\});""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: return false

        return try {
            val playlist = JSONObject(playlistMatch)
            val sources = playlist.getJSONArray("sources")
            var found = false

            for (i in 0 until sources.length()) {
                val source = sources.getJSONObject(i)
                val fileUrl = source.getString("file")
                val label = source.optString("label", "")
                val quality = when {
                    label.contains("1080") -> Qualities.P1080.value
                    label.contains("720")  -> Qualities.P720.value
                    label.contains("480")  -> Qualities.P480.value
                    label.contains("360")  -> Qualities.P360.value
                    label.contains("240")  -> Qualities.P240.value
                    else -> Qualities.Unknown.value
                }

                callback(newExtractorLink(name, "$name ${label}p", fileUrl, ExtractorLinkType.VIDEO) {
                    this.quality = quality
                    this.referer = mainUrl
                    this.headers = ua
                })
                found = true
            }
            found
        } catch (e: Exception) {
            false
        }
    }
}
