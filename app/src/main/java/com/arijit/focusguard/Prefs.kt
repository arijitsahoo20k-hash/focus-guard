package com.arijit.focusguard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)

    /** null = never saved yet (first run) */
    fun blocked(c: Context): Set<String>? = sp(c).getStringSet("blocked", null)

    fun saveBlocked(c: Context, s: Set<String>) {
        sp(c).edit().putStringSet("blocked", HashSet(s)).apply()
    }

    fun endTime(c: Context): Long = sp(c).getLong("end", 0L)

    fun strict(c: Context): Boolean = sp(c).getBoolean("strict", false)

    fun start(c: Context, endMillis: Long, strict: Boolean) {
        sp(c).edit().putLong("end", endMillis).putBoolean("strict", strict).apply()
    }

    fun isActive(c: Context): Boolean = System.currentTimeMillis() < endTime(c)

    /**
     * Apps that must NEVER be blocked: ourselves, system UI, the current home launcher
     * (blocking it would trap you in an endless HOME loop) and phone/dialer (emergency calls).
     */
    fun protectedPackages(c: Context): Set<String> {
        val s = mutableSetOf(
            c.packageName, "android", "com.android.systemui", "com.android.phone",
            "com.android.server.telecom", "com.android.dialer", "com.google.android.dialer",
            "com.samsung.android.dialer", "com.android.emergency"
        )
        try {
            val home = c.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY
            )?.activityInfo?.packageName
            if (home != null) s.add(home)
        } catch (_: Exception) {
        }
        try {
            val d = c.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
            if (d != null) s.add(d)
        } catch (_: Exception) {
        }
        return s
    }
}
