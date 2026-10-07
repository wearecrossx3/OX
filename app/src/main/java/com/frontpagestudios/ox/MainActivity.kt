package com.frontpagestudios.ox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.tracking.AutoDetect
import com.frontpagestudios.ox.ui.OxRoot
import com.frontpagestudios.ox.ui.theme.OxTheme
import com.frontpagestudios.ox.util.Perms
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface DeepLink {
    data object Live : DeepLink
    data class OpenTrip(val id: String) : DeepLink
    data object StartTrip : DeepLink
}

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_LIVE = "open_live"
        const val EXTRA_OPEN_TRIP = "open_trip"
        const val EXTRA_START_TRIP = "start_trip"
    }

    private val deepLink = MutableStateFlow<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            OxTheme {
                OxRoot(deepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.autoDetect.value && Perms.canAuto(this)) AutoDetect.enable(this)
    }

    private fun handle(intent: Intent?) {
        intent ?: return
        when {
            intent.getBooleanExtra(EXTRA_START_TRIP, false) -> deepLink.value = DeepLink.StartTrip
            intent.getBooleanExtra(EXTRA_OPEN_LIVE, false) -> deepLink.value = DeepLink.Live
            intent.getStringExtra(EXTRA_OPEN_TRIP) != null ->
                deepLink.value = DeepLink.OpenTrip(intent.getStringExtra(EXTRA_OPEN_TRIP)!!)
        }
        intent.removeExtra(EXTRA_OPEN_LIVE)
        intent.removeExtra(EXTRA_OPEN_TRIP)
        intent.removeExtra(EXTRA_START_TRIP)
    }
}
