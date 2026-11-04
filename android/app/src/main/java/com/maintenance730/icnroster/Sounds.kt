package com.maintenance730.icnroster

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

/** Built-in sounds (res/raw) + the phone's own alarm / notification sound. */
object Sounds {
    data class Snd(val id: String, val name: String, val res: Int?)

    val WAKE = listOf(
        Snd("sys_alarm", "Phone alarm sound", null),
        Snd("alarm_classic", "Classic beep", R.raw.alarm_classic),
        Snd("alarm_digital", "Digital", R.raw.alarm_digital),
        Snd("alarm_rise", "Rising melody", R.raw.alarm_rise),
        Snd("alarm_chime", "Chime", R.raw.alarm_chime),
    )
    val FLIGHT = listOf(
        Snd("sys_notif", "Phone notification sound", null),
        Snd("ping", "Ping", R.raw.ping),
        Snd("double_beep", "Double beep", R.raw.double_beep),
        Snd("chime_short", "Chime", R.raw.chime_short),
        Snd("radar", "Radar", R.raw.radar),
        Snd("silent", "Silent (pop-up only)", null),
    )

    fun uri(c: Context, id: String, alarm: Boolean): Uri? {
        if (id == "silent") return null
        val s = (WAKE + FLIGHT).firstOrNull { it.id == id }
        if (s?.res != null) return Uri.parse("android.resource://${c.packageName}/${s.res}")
        return if (alarm) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: Settings.System.DEFAULT_NOTIFICATION_URI
               else Settings.System.DEFAULT_NOTIFICATION_URI
    }

    fun json(): JSONObject {
        fun arr(l: List<Snd>) = JSONArray().apply { l.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) } }
        return JSONObject().put("wake", arr(WAKE)).put("flight", arr(FLIGHT))
    }

    // ---------- preview (▶ button in the settings) ----------
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun preview(c: Context, id: String, alarm: Boolean) {
        stop()
        val u = uri(c, id, alarm) ?: return
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(if (alarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            p.setDataSource(c, u)
            p.setOnPreparedListener { it.start() }
            p.setOnCompletionListener { stop() }
            p.prepareAsync()
            player = p
            handler.postDelayed({ stop() }, 4000)
        } catch (e: Exception) { stop() }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        try { player?.stop() } catch (e: Exception) { }
        try { player?.release() } catch (e: Exception) { }
        player = null
    }
}
