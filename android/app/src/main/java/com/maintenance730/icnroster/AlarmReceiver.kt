package com.maintenance730.icnroster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Stop / Snooze buttons on the alarm notification, and the snoozed alarm itself. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val title = i.getStringExtra("title") ?: "⏰ Wake-up call"
        val body = i.getStringExtra("body") ?: ""
        when (i.action) {
            Alarm.ACTION_STOP -> Alarm.stop(c)
            Alarm.ACTION_SNOOZE -> Alarm.snooze(c, title, body)
            Alarm.ACTION_RING -> Alarm.ring(c, title, body)
        }
    }
}
