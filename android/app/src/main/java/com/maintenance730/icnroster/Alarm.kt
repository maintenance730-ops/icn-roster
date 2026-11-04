package com.maintenance730.icnroster

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Wake-up alarm: a full-screen notification on the alarm channel that keeps ringing (FLAG_INSISTENT)
 * until Stop / Snooze is tapped (or 10 min pass).
 */
object Alarm {
    const val NID = 4242
    const val ACTION_STOP = "icn.alarm.STOP"
    const val ACTION_SNOOZE = "icn.alarm.SNOOZE"
    const val ACTION_RING = "icn.alarm.RING"
    const val SNOOZE_MIN = 5

    fun ring(c: Context, title: String, body: String) {
        Notifs.ensureChannels(c)
        if (!Notifs.canPost(c)) return
        val full = Intent(c, AlarmActivity::class.java)
            .putExtra("title", title).putExtra("body", body)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val fullPi = PendingIntent.getActivity(c, 1, full, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopPi = PendingIntent.getBroadcast(c, 2,
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val snoozePi = PendingIntent.getBroadcast(c, 3,
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_SNOOZE).putExtra("title", title).putExtra("body", body),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val n = NotificationCompat.Builder(c, Notifs.wakeChan(c))
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFF1F3864.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullPi, true)
            .setContentIntent(fullPi)
            .addAction(0, "Stop", stopPi)
            .addAction(0, "Snooze $SNOOZE_MIN min", snoozePi)
            .setTimeoutAfter(10 * 60 * 1000L)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        try { NotificationManagerCompat.from(c).notify(NID, n) } catch (e: SecurityException) { }
    }

    fun stop(c: Context) {
        NotificationManagerCompat.from(c).cancel(NID)
    }

    fun snooze(c: Context, title: String, body: String) {
        stop(c)
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + SNOOZE_MIN * 60_000L
        val pi = PendingIntent.getBroadcast(c, 4,
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_RING).putExtra("title", title).putExtra("body", body),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}
