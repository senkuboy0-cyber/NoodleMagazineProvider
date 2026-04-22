package com.pmovie

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class PmovieProvider : MainAPI() {
    override var mainUrl = "https://560pmovie.com"
    override var name = "560pmovie"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.NSFW, TvType.Others)

    override val mainPage = mainPageOf(
        "$mainUrl/movies/page/" to "Movies",
        "$mainUrl/tvshows/page/" to "TV Shows",
        "$mainUrl/genre/web-series/page/" to "Web Series",
        "$mainUrl/genre/18/page/" to "18+ Movies",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.item").mapNotNull {
            val title = it.selectFirst("div.imagens a img")?.attr("alt") ?: return@mapNotNull null
            val link = it.selectFirst("div.imagens a")?.attr("href") ?: return@mapNotNull null
            val img = it.selectFirst("div.imagens a img")?.attr("src") ?: ""
            var posterUrl = img
            if (img.startsWith("data:image")) {
               posterUrl = it.selectFirst("div.imagens a img")?.attr("data-lazy-src") ?: ""
            }
            newMovieSearchResponse(title, link, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.result-item, div.item").mapNotNull {
            val title = it.selectFirst("div.imagens a img, div.thumbnail a img")?.attr("alt") ?: return@mapNotNull null
            val link = it.selectFirst("div.imagens a, div.thumbnail a")?.attr("href") ?: return@mapNotNull null
            val img = it.selectFirst("div.imagens a img, div.thumbnail a img")?.attr("src") ?: ""
            var posterUrl = img
            if (img.startsWith("data:image")) {
               posterUrl = it.selectFirst("div.imagens a img, div.thumbnail a img")?.attr("data-lazy-src") ?: ""
            }
            newMovieSearchResponse(title, link, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1[itemprop=name]")?.text() ?: ""
        var poster = document.selectFirst("div.imagen img[itemprop=image]")?.attr("src")
        if (poster?.startsWith("data:image") == true) {
             poster = document.selectFirst("div.imagen img[itemprop=image]")?.attr("data-lazy-src")
        }
        val plot = document.selectFirst("div[itemprop=description] p")?.text()
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        document.select("div[id^=div] a.maxbutton").forEach {
            val link = it.attr("href")
            val text = it.selectFirst("span.mb-text")?.text() ?: ""
            if(!link.isNullOrBlank() && text.contains("WATCH", ignoreCase = true)) {
                loadExtractor(link, "$mainUrl/", subtitleCallback, callback)
            }
        }
        val iframe = document.selectFirst("div.video-player iframe")?.attr("src") ?: document.selectFirst("div.youtube_id iframe")?.attr("data-lazy-src")
        if (iframe != null) {
            var url = iframe
            if (url.startsWith("//")) {
                url = "https:$url"
            }
            loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
        }
        return true
    }
}
