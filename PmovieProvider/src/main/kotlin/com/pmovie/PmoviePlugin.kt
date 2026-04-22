package com.pmovie

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PmoviePlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PmovieProvider())
    }
}
