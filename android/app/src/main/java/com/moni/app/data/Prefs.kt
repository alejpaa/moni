package com.moni.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persisted app-level user preferences backed by SharedPreferences. */
object Prefs {
    private const val FILE = "moni_prefs"
    private const val KEY_HIDE_AMOUNTS = "hide_amounts"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_CARD_NOTIFS_ENABLED = "card_notifs_enabled"
    private const val KEY_CARD_NOTIFS_DAYS_BEFORE = "card_notifs_days_before"
    private const val KEY_CARD_NOTIF_LAST_PREFIX = "card_notif_last_"
    const val DEFAULT_CARD_NOTIFY_DAYS = 3

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _hideAmounts = MutableStateFlow(false)
    val hideAmounts: StateFlow<Boolean> = _hideAmounts.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(false)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _cardNotificationsEnabled = MutableStateFlow(false)
    val cardNotificationsEnabled: StateFlow<Boolean> = _cardNotificationsEnabled.asStateFlow()

    private val _cardNotifyDaysBefore = MutableStateFlow(DEFAULT_CARD_NOTIFY_DAYS)
    val cardNotifyDaysBefore: StateFlow<Int> = _cardNotifyDaysBefore.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            prefs = sp
            _hideAmounts.value = sp.getBoolean(KEY_HIDE_AMOUNTS, false)
            _appLockEnabled.value = sp.getBoolean(KEY_APP_LOCK_ENABLED, false)
            _cardNotificationsEnabled.value = sp.getBoolean(KEY_CARD_NOTIFS_ENABLED, false)
            _cardNotifyDaysBefore.value = sp.getInt(KEY_CARD_NOTIFS_DAYS_BEFORE, DEFAULT_CARD_NOTIFY_DAYS)
        }
    }

    fun setHideAmounts(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_HIDE_AMOUNTS, value)?.apply()
        _hideAmounts.value = value
    }

    fun setAppLockEnabled(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_APP_LOCK_ENABLED, value)?.apply()
        _appLockEnabled.value = value
    }

    fun setCardNotificationsEnabled(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_CARD_NOTIFS_ENABLED, value)?.apply()
        _cardNotificationsEnabled.value = value
    }

    fun setCardNotifyDaysBefore(days: Int) {
        val clamped = days.coerceIn(1, 30)
        prefs?.edit()?.putInt(KEY_CARD_NOTIFS_DAYS_BEFORE, clamped)?.apply()
        _cardNotifyDaysBefore.value = clamped
    }

    fun wasCardNotifSent(accountId: String, milestoneKey: String): Boolean =
        prefs?.getString(KEY_CARD_NOTIF_LAST_PREFIX + accountId, null) == milestoneKey

    fun markCardNotifSent(accountId: String, milestoneKey: String) {
        prefs?.edit()?.putString(KEY_CARD_NOTIF_LAST_PREFIX + accountId, milestoneKey)?.apply()
    }
}
