package com.frontpagestudios.ox.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.frontpagestudios.ox.ui.components.pressable
import com.frontpagestudios.ox.ui.theme.Ink
import com.frontpagestudios.ox.ui.theme.Lime
import com.frontpagestudios.ox.ui.theme.Mist
import com.frontpagestudios.ox.ui.theme.Paper
import com.frontpagestudios.ox.ui.theme.Snow

@Composable
fun MainScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(Paper)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(tween(320)) { 60 }) togetherWith fadeOut(tween(140))
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeTab(nav, openTab = { tab = it })
                1 -> HistoryTab(nav)
                2 -> RoutesTab(nav, openSettings = { tab = 4 })
                3 -> StatsTab(nav)
                else -> SettingsTab()
            }
        }
        // soft fade so content slides under the bar
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(130.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Paper.copy(alpha = 0.95f))))
        )
        BottomBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val icons = listOf(Icons.Rounded.Home, Icons.Rounded.History, Icons.Rounded.Directions, Icons.Rounded.BarChart, Icons.Rounded.Tune)
    Row(
        modifier.navigationBarsPadding().padding(bottom = 14.dp)
            .shadow(18.dp, RoundedCornerShape(44.dp), ambientColor = Ink.copy(alpha = 0.2f), spotColor = Ink.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(44.dp)).background(Snow).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icons.forEachIndexed { i, icon ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Lime else Mist.copy(alpha = 0.6f), tween(260), label = "nav")
            Box(
                Modifier.pressable { onSelect(i) }.size(56.dp).clip(CircleShape).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}
