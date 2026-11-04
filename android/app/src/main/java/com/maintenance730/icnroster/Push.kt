package com.maintenance730.icnroster

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

/**
 * Firebase is started from the config the server sends (Script property FCM_ANDROID = google-services.json),
 * so no Firebase file / key lives in the GitHub repo. The config is saved so pushes work after a reboot.
 */
object Push {
    fun initFromPrefs(c: Context) {
        val s = Prefs.fcm(c) ?: return
        try { init(c, JSONObject(s)) } catch (e: Exception) { }
    }

    fun init(c: Context, o: JSONObject): Boolean {
        if (FirebaseApp.getApps(c).isNotEmpty()) return true
        val opts = FirebaseOptions.Builder()
            .setApiKey(o.getString("apiKey"))
            .setApplicationId(o.getString("appId"))
            .setProjectId(o.getString("projectId"))
            .setGcmSenderId(o.getString("senderId"))
            .build()
        FirebaseApp.initializeApp(c, opts)
        return true
    }
}
