package com.shield.app

import android.app.Application
import com.shield.app.blocklist.BlocklistManager

class ShieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Warm up the in-memory blocklist as early as possible so the
        // Accessibility Service has compiled patterns ready immediately.
        BlocklistManager.get(this).initialLoad()
    }
}
