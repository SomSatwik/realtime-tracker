package com.ghosttrack.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class GhostTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configure osmdroid
        Configuration.getInstance().userAgentValue = packageName
    }
}
