package com.maintenance730.icnroster

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notification channels — one per sound (Android fixes a channel's sound when it is created,
 * so changing the sound = a new channel id).
 *   wake_<sound>  alarm (full screen, repeats until stopped)
 *   arr_<sound>   landing   (pop-up)
 *   dep_<sound>   take-off  (pop-up)
 *   general       test / other
 */
object Notifs {
    const val GENERAL = "general"

    fun wakeChan(c: Context) = "wake_" + Prefs.wakeSound(c)
    fun arrChan(c: Context) = "arr_" + Prefs.arrSound(c)
    fun depChan(c: Context) = "dep_" + Prefs.depSound(c)

    fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        val want = setOf(GENERAL, wakeChan(c), arrChan(c), depChan(c))
        nm.notificationChannels.filter { it.id !in want }.forEach { nm.deleteNotificationChannel(it.id) }

        nm.createNotificationChannel(NotificationChannel(GENERAL, "General", NotificationManager.IMPORTANCE_HIGH))

        val alarmAttr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        nm.createNotificationChannel(NotificationChannel(wakeChan(c), "Wake-up alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(Sounds.uri(c, Prefs.wakeSound(c), true), alarmAttr)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })

        val notifAttr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        fun flight(id: String, name: String, snd: String) =
            nm.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(Sounds.uri(c, snd, false), if (snd == "silent") null else notifAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 120, 250)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            })
        flight(arrChan(c), "Landing", Prefs.arrSound(c))
        flight(depChan(c), "Take-off", Prefs.depSound(c))
    }

    fun canPost(c: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(c).areNotificationsEnabled()

    /** a push arrived — data = {title, body, tag, kind} */
    fun show(c: Context, d: Map<String, String>) {
        ensureChannels(c)
        val kind = d["kind"] ?: ""
        val title = d["title"] ?: "ICN Roster"
        val body = d["body"] ?: ""
        if (kind == "wake") { Alarm.ring(c, title, body); return }
        if (!canPost(c)) return
        val chan = when (kind) { "arr" -> arrChan(c); "dep" -> depChan(c); else -> GENERAL }
        val open = PendingIntent.getActivity(c, 0,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, chan)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFF1F3864.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        val tag = d["tag"].orEmpty().ifEmpty { title }
        try { NotificationManagerCompat.from(c).notify(tag.hashCode(), n) } catch (e: SecurityException) { }
    }
}
