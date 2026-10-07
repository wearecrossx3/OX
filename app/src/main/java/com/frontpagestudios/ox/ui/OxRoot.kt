package com.frontpagestudios.ox.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.frontpagestudios.ox.DeepLink
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.tracking.TrackingService
import com.frontpagestudios.ox.tracking.TrackingState
import com.frontpagestudios.ox.ui.screens.LiveScreen
import com.frontpagestudios.ox.ui.screens.MainScreen
import com.frontpagestudios.ox.ui.screens.OnboardingScreen
import com.frontpagestudios.ox.ui.screens.TripDetailScreen
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.util.Perms
import com.frontpagestudios.ox.util.Sfx
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val ONBOARD = "onboard"
    const val MAIN = "main"
    const val LIVE = "live"
    const val TRIP = "trip/{id}"
    fun trip(id: String) = "trip/$id"
}

/** Starts a manual trip and opens the live screen. */
fun startTrip(ctx: Context, nav: NavHostController, mode: TripMode) {
    if (!Perms.location(ctx)) {
        Toast.makeText(ctx, "Allow location in Settings → Permissions to track trips", Toast.LENGTH_LONG).show()
        return
    }
    if (TrackingState.live.value == null) {
        Sfx.play(Sfx.S.START)
        TrackingService.start(ctx, mode, auto = false)
    }
    nav.navigate(Routes.LIVE) { launchSingleTop = true }
}

@Composable
fun OxRoot(deepLink: MutableStateFlow<DeepLink?>) {
    val nav = rememberNavController()
    val onboarded by Prefs.onboarded.collectAsState()
    val link by deepLink.collectAsState()
    val start = remember { if (Prefs.onboarded.value) Routes.MAIN else Routes.ONBOARD }

    LaunchedEffect(link, onboarded) {
        val l = link ?: return@LaunchedEffect
        if (!onboarded) return@LaunchedEffect
        when (l) {
            DeepLink.Live -> if (TrackingState.live.value != null) nav.navigate(Routes.LIVE) { launchSingleTop = true }
            is DeepLink.OpenTrip -> nav.navigate(Routes.trip(l.id)) { launchSingleTop = true }
        }
        deepLink.value = null
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.fillMaxSize().background(Paper),
        enterTransition = { fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f) },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 0.96f) },
    ) {
        composable(Routes.ONBOARD) {
            OnboardingScreen(onDone = {
                Prefs.setOnboarded(true)
                nav.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARD) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) {
            MainScreen(nav)
        }
        composable(
            Routes.LIVE,
            enterTransition = { slideInVertically(tween(420)) { it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutVertically(tween(320)) { it / 3 } + fadeOut(tween(260)) },
        ) {
            LiveScreen(
                onClose = { nav.popBackStack() },
                onSaved = { id ->
                    nav.navigate(Routes.trip(id)) { popUpTo(Routes.LIVE) { inclusive = true } }
                },
            )
        }
        composable("route/{key}") { entry ->
            val key = android.net.Uri.decode(entry.arguments?.getString("key") ?: "")
            com.frontpagestudios.ox.ui.screens.RouteDetailScreen(routeKey = key, nav = nav, onBack = { nav.popBackStack() })
        }
        composable(Routes.TRIP) { entry ->
            val id = entry.arguments?.getString("id") ?: ""
            TripDetailScreen(id = id, onBack = { nav.popBackStack() })
        }
    }
}
