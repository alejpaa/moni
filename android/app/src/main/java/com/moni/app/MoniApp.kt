package com.moni.app

import android.app.Application
import com.moni.app.data.CardDueNotifications
import com.moni.app.data.CardDueScheduler
import com.moni.app.data.Prefs

class MoniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        CardDueNotifications.ensureChannel(this)
        CardDueScheduler.sync(this, Prefs.cardNotificationsEnabled.value)
    }
}
