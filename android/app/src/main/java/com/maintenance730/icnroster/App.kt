package com.maintenance730.icnroster

import android.app.Application

/** Starts Firebase from the saved config (so pushes work even when the app was closed) and creates the channels. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Push.initFromPrefs(this)
        Notifs.ensureChannels(this)
    }
}
