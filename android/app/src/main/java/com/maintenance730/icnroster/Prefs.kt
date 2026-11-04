package com.maintenance730.icnroster

import android.content.Context

/** Small settings stored on the phone. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("icn", Context.MODE_PRIVATE)

    fun url(c: Context): String = sp(c).getString("url", "") ?: ""
    fun setUrl(c: Context, v: String) = sp(c).edit().putString("url", v).apply()

    fun fcm(c: Context): String? = sp(c).getString("fcm", null)
    fun setFcm(c: Context, v: String) = sp(c).edit().putString("fcm", v).apply()

    fun lastToken(c: Context): String? = sp(c).getString("token", null)
    fun setLastToken(c: Context, v: String) = sp(c).edit().putString("token", v).apply()

    fun wakeSound(c: Context): String = sp(c).getString("snd_wake", "sys_alarm") ?: "sys_alarm"
    fun arrSound(c: Context): String = sp(c).getString("snd_arr", "sys_notif") ?: "sys_notif"
    fun depSound(c: Context): String = sp(c).getString("snd_dep", "sys_notif") ?: "sys_notif"
    fun setSounds(c: Context, wake: String, arr: String, dep: String) =
        sp(c).edit().putString("snd_wake", wake).putString("snd_arr", arr).putString("snd_dep", dep).apply()
}
