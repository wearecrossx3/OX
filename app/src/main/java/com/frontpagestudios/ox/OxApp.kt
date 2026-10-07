package com.frontpagestudios.ox

import android.app.Application
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.tracking.Notifs
import com.frontpagestudios.ox.util.Sfx
import org.osmdroid.config.Configuration
import java.io.File

class OxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        TripRepo.init(this)
        Sfx.init(this)
        Notifs.createChannels(this)
        Configuration.getInstance().apply {
            userAgentValue = "OX-Android/${BuildConfig.VERSION_NAME} ($packageName)"
            osmdroidBasePath = File(cacheDir, "osm")
            osmdroidTileCache = File(cacheDir, "osm/tiles")
        }
    }
}
