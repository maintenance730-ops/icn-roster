package com.maintenance730.icnroster

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

/**
 * The roster page (Apps Script, inside the WebView) talks to the app through `icnApp.postMessage(json)`.
 * Same messages as the web shell: {icn:'push', id, cmd:'ping'|'token'|'sounds'|'preview'|'stopPreview'|'fullscreen'}.
 * Only pages from Google's Apps Script hosts get the `icnApp` object.
 */
object Bridge {
    private val ORIGINS = setOf("https://*.googleusercontent.com", "https://script.google.com")

    fun attach(a: MainActivity, web: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(web, "icnApp", ORIGINS) { _, message, _, _, reply ->
            val d = try { JSONObject(message.data ?: "{}") } catch (e: Exception) { JSONObject() }
            handle(a, d, reply)
        }
    }

    private fun handle(a: MainActivity, d: JSONObject, reply: JavaScriptReplyProxy) {
        val id = d.optInt("id")
        fun send(o: JSONObject) {
            o.put("icn", "push"); o.put("id", id)
            a.runOnUiThread { try { reply.postMessage(o.toString()) } catch (e: Exception) { } }
        }
        fun err(code: String, msg: String = "") = send(JSONObject().put("error", code).put("msg", msg))

        when (d.optString("cmd")) {
            "ping" -> send(JSONObject()
                .put("native", true).put("shell", true).put("supported", true)
                .put("perm", if (Notifs.canPost(a)) "granted" else "default")
                .put("fullScreen", canFullScreen(a))
                .put("sounds", Sounds.json())
                .put("prefs", JSONObject().put("wake", Prefs.wakeSound(a)).put("arr", Prefs.arrSound(a)).put("dep", Prefs.depSound(a)))
                .put("version", a.versionName()))

            "token" -> {
                val cfg = d.optJSONObject("fcm")?.optJSONObject("android")
                if (cfg == null) { err("token", "The app is not set up on the server yet (FCM_ANDROID)."); return }
                try {
                    Prefs.setFcm(a, cfg.toString())
                    Push.init(a, cfg)
                } catch (e: Exception) { err("token", "Firebase: " + (e.message ?: "init failed")); return }
                val go = {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
                        if (t.isSuccessful && t.result != null) {
                            val tok = t.result
                            val prev = Prefs.lastToken(a)
                            Prefs.setLastToken(a, tok)
                            send(JSONObject().put("token", tok).put("prev", if (prev != null && prev != tok) prev else ""))
                        } else err("token", t.exception?.message ?: "could not get a token")
                    }
                }
                when {
                    Notifs.canPost(a) -> go()
                    !d.optBoolean("ask") -> err("ask")
                    else -> a.askNotifPermission { ok -> if (ok) go() else err("denied") }
                }
            }

            "sounds" -> {
                Prefs.setSounds(a, d.optString("wake", Prefs.wakeSound(a)), d.optString("arr", Prefs.arrSound(a)), d.optString("dep", Prefs.depSound(a)))
                Notifs.ensureChannels(a)
                send(JSONObject().put("ok", true))
            }

            "preview" -> { a.runOnUiThread { Sounds.preview(a, d.optString("sound"), d.optBoolean("alarm")) }; send(JSONObject().put("ok", true)) }
            "stopPreview" -> { a.runOnUiThread { Sounds.stop() }; send(JSONObject().put("ok", true)) }

            "fullscreen" -> {
                a.runOnUiThread {
                    try {
                        val i = if (Build.VERSION.SDK_INT >= 34)
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + a.packageName))
                        else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.packageName)
                        a.startActivity(i)
                    } catch (e: Exception) { }
                }
                send(JSONObject().put("ok", true))
            }

            "notifSettings" -> {
                a.runOnUiThread {
                    try { a.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.packageName)) }
                    catch (e: Exception) { }
                }
                send(JSONObject().put("ok", true))
            }
        }
    }

    fun canFullScreen(a: MainActivity): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val nm = a.getSystemService(NotificationManager::class.java) ?: return true
        return nm.canUseFullScreenIntent()
    }
}
