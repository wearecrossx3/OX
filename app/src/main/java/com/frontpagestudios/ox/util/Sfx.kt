package com.frontpagestudios.ox.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.frontpagestudios.ox.R
import com.frontpagestudios.ox.data.Prefs

object Sfx {
    enum class S(val res: Int, val vol: Float) {
        TAP(R.raw.sfx_tap, 0.35f),
        START(R.raw.sfx_start, 0.7f),
        STOP(R.raw.sfx_stop, 0.7f),
        PING(R.raw.sfx_ping, 0.6f),
        SWOOSH(R.raw.sfx_swoosh, 0.45f),
        SUCCESS(R.raw.sfx_success, 0.7f),
    }

    private var pool: SoundPool? = null
    private val ids = HashMap<S, Int>()

    fun init(context: Context) {
        if (pool != null) return
        val p = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        S.entries.forEach { ids[it] = p.load(context, it.res, 1) }
        pool = p
    }

    fun play(s: S) {
        if (!Prefs.sound.value) return
        val id = ids[s] ?: return
        pool?.play(id, s.vol, s.vol, 1, 0, 1f)
    }
}
