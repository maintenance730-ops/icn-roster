package com.maintenance730.icnroster

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Full-screen alarm (also over the lock screen): big clock, shift text, Stop / Snooze. */
class AlarmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val title = intent.getStringExtra("title") ?: "⏰ Wake-up call"
        val body = intent.getStringExtra("body") ?: ""
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1F3864"))
            setPadding((28 * dp).toInt(), (40 * dp).toInt(), (28 * dp).toInt(), (40 * dp).toInt())
        }
        fun text(s: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
            text = s; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
        }
        root.addView(text(SimpleDateFormat("HH:mm", Locale.US).format(Date()), 72f, Color.WHITE, true))
        root.addView(text(title, 22f, Color.WHITE, true))
        root.addView(text(body, 16f, Color.parseColor("#B8C4DA")))

        fun button(label: String, bg: String, fg: String, onClick: () -> Unit) = Button(this).apply {
            text = label; textSize = 18f; isAllCaps = false; setTextColor(Color.parseColor(fg))
            background = GradientDrawable().apply { cornerRadius = 16 * dp; setColor(Color.parseColor(bg)) }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (60 * dp).toInt()).apply {
                topMargin = (14 * dp).toInt()
            }
        }
        root.addView(LinearLayout(this).apply { minimumHeight = (30 * dp).toInt() })
        root.addView(button("Stop", "#FFFFFF", "#1F3864") { Alarm.stop(this); finish() })
        root.addView(button("Snooze ${Alarm.SNOOZE_MIN} min", "#33FFFFFF", "#FFFFFF") { Alarm.snooze(this, title, body); finish() })
        setContentView(root)
    }
}
