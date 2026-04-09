package com.androidpyhole

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pyholex_settings", Context.MODE_PRIVATE)

    fun saveExcludedApps(apps: List<String>) {
        prefs.edit().putStringSet("excluded_apps", apps.toSet()).apply()
    }

    fun getExcludedApps(): List<String> {
        return prefs.getStringSet("excluded_apps", emptySet())?.toList() ?: emptyList()
    }

    fun saveUpstreamDns(dns: String) {
        prefs.edit().putString("upstream_dns", dns).apply()
    }

    fun getUpstreamDns(): String {
        return prefs.getString("upstream_dns", "1.1.1.1:53") ?: "1.1.1.1:53"
    }

    fun saveThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt("theme_mode", 2)
    }

    fun setAppLockPin(pin: String?) {
        prefs.edit().putString("app_lock_pin", pin).apply()
    }

    fun getAppLockPin(): String? {
        return prefs.getString("app_lock_pin", null)
    }
}
