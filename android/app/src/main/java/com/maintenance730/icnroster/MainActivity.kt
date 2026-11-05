package com.maintenance730.icnroster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * The roster web app (Apps Script) in a WebView. First start: paste the web app link (or the team link).
 * Native parts (notifications, alarm, sounds) are reached from the page through Bridge.
 */
class MainActivity : AppCompatActivity() {
    private var web: WebView? = null
    private var pendingPerm: ((Boolean) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        pendingPerm?.invoke(ok); pendingPerm = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val w = web
                if (w != null && w.canGoBack()) w.goBack() else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        takeLink(intent)
        start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (takeLink(intent)) start()
    }

    /** team link …/icn-roster/?u=<web app url>  or  ?reset=1 */
    private fun takeLink(i: Intent?): Boolean {
        val data = i?.data ?: return false
        if (data.getQueryParameter("reset") != null) { Prefs.setUrl(this, ""); return true }
        val u = parse(data.toString()) ?: return false
        Prefs.setUrl(this, u); return true
    }

    private fun start() {
        val u = Prefs.url(this)
        if (u.isEmpty()) showSetup("") else showWeb(u)
    }

    fun versionName(): String = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (e: Exception) { "" }

    // ---------- first start: web app link ----------
    private fun parse(s: String): String? {
        var v = s.trim()
        if (v.contains("u=")) v = try { Uri.parse(v).getQueryParameter("u") ?: v } catch (e: Exception) { v }
        val ok = Regex("^https://script\\.google\\.com/(a/macros/[^/]+|macros)/s/[\\w-]+/(exec|dev)(\\?.*)?$")
        return if (ok.matches(v.trim())) v.trim() else null
    }

    private fun showSetup(msg: String) {
        web?.destroy(); web = null
        val dp = resources.displayMetrics.density
        val pad = (22 * dp).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (48 * dp).toInt(), pad, pad)
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }
        fun tv(s: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
            text = s; textSize = size; setTextColor(Color.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
        }
        box.addView(tv("ICN Line Roster — setup", 21f, "#0F172A", true))
        box.addView(tv("Paste the team link (or the web app link) once. It is saved on this phone.", 14f, "#475569"))
        box.addView(tv("카톡에 공유된 팀 링크(또는 …/exec 웹앱 주소)를 한 번만 붙여넣으면 돼.", 13f, "#64748B"))
        val input = EditText(this).apply {
            hint = "https://…"
            setSingleLine(true)
            textSize = 14f
            background = GradientDrawable().apply { cornerRadius = 10 * dp; setColor(Color.WHITE); setStroke((1 * dp).toInt(), Color.parseColor("#CBD5E1")) }
            setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
        }
        box.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (10 * dp).toInt() })
        val err = tv(msg, 13f, "#B91C1C")
        box.addView(err)
        val btn = Button(this).apply {
            text = "Save & open"; isAllCaps = false; textSize = 16f; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { cornerRadius = 12 * dp; setColor(Color.parseColor("#1F3864")) }
            setOnClickListener {
                val u = parse(input.text.toString())
                if (u == null) err.text = "That does not look like the team link or a web app link (…/macros/s/…/exec)."
                else { Prefs.setUrl(this@MainActivity, u); showWeb(u) }
            }
        }
        box.addView(btn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (52 * dp).toInt()).apply { topMargin = (6 * dp).toInt() })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#F1F5F9")); addView(box) })
    }

    // ---------- the roster ----------
    @SuppressLint("SetJavaScriptEnabled")
    private fun showWeb(url: String) {
        web?.destroy()
        val w = WebView(this)
        web = w
        w.setBackgroundColor(Color.parseColor("#F1F5F9"))
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.setSupportZoom(false)
        w.settings.userAgentString = w.settings.userAgentString + " ICNRosterApp/" + versionName()
        w.webChromeClient = WebChromeClient()
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val host = req.url.host ?: return false
                if (host.endsWith("google.com") || host.endsWith("googleusercontent.com")) return false
                try { startActivity(Intent(Intent.ACTION_VIEW, req.url)) } catch (e: Exception) { }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (Uri.parse(url).host == "script.google.com") view.evaluateJavascript(HIDE_BANNER, null)
            }
        }
        Bridge.attach(this, w)
        setContentView(w)
        w.loadUrl(url)
    }

    companion object {
        /**
         * Google shows a "created by a Google Apps Script user" bar above every Apps Script web app.
         * In the app we hide it: keep only the frame that holds the roster and stretch it to the full screen.
         */
        private const val HIDE_BANNER = """
(function () {
  function fix() {
    var f = document.getElementById('sandboxFrame'), all = document.querySelectorAll('iframe');
    if (!f) for (var i = 0; i < all.length; i++) if (!f || all[i].offsetHeight > f.offsetHeight) f = all[i];
    if (!f || !document.body) return false;
    var box = f;
    while (box.parentElement && box.parentElement !== document.body) box = box.parentElement;
    Array.prototype.forEach.call(document.body.children, function (c) {
      if (c !== box && c.tagName !== 'SCRIPT' && c.tagName !== 'STYLE') c.style.setProperty('display', 'none', 'important');
    });
    box.style.cssText += ';position:fixed!important;top:0!important;left:0!important;right:0!important;bottom:0!important;width:100%!important;height:100%!important;margin:0!important;';
    f.style.cssText += ';width:100%!important;height:100%!important;border:0!important;';
    document.documentElement.style.overflow = 'hidden';
    document.body.style.cssText += ';margin:0!important;padding:0!important;overflow:hidden!important;';
    return true;
  }
  var n = 0, t = setInterval(function () { fix(); if (++n > 20) clearInterval(t); }, 250);
  fix();
})();
"""
    }

    // ---------- notification permission (Android 13+) ----------
    fun askNotifPermission(cb: (Boolean) -> Unit) {
        runOnUiThread {
            if (Build.VERSION.SDK_INT < 33) { cb(Notifs.canPost(this)); return@runOnUiThread }
            pendingPerm = cb
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        Sounds.stop()
        web?.destroy(); web = null
        super.onDestroy()
    }
}
