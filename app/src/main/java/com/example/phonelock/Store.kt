package com.example.phonelock

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.util.Calendar

const val LOCK_MS = 24L * 60 * 60 * 1000

class Store(private val c: Context) {
    private val p = c.getSharedPreferences("s", Context.MODE_PRIVATE)

    var limitMin: Int
        get() = p.getInt("limit", 180)
        set(v) = p.edit().putInt("limit", v).apply()

    private fun bootCount() = Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, 0)

    fun lockNow() = p.edit()
        .putLong("wall", System.currentTimeMillis())
        .putLong("elapsed", SystemClock.elapsedRealtime())
        .putInt("boot", bootCount())
        .putBoolean("locked", true).apply()

    /** milliseconds left in the lock; 0 = not locked */
    fun remainingMs(): Long {
        if (!p.getBoolean("locked", false)) return 0
        // Same boot: trust elapsedRealtime (immune to clock changes). After reboot: wall clock.
        val passed = if (p.getInt("boot", -1) == bootCount())
            SystemClock.elapsedRealtime() - p.getLong("elapsed", 0)
        else System.currentTimeMillis() - p.getLong("wall", 0)
        val left = LOCK_MS - passed
        if (left <= 0) { unlock(); return 0 }
        return left
    }

    private fun unlock() {
        // new baseline so usage already counted today doesn't re-lock immediately
        p.edit().putBoolean("locked", false)
            .putLong("base", todayUsageRaw()).putLong("baseDay", dayStart()).apply()
    }

    private fun dayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val skip = setOf(c.packageName, "com.android.dialer", "com.google.android.dialer",
        "com.android.phone", "com.android.systemui", "com.android.settings")

    private fun todayUsageRaw(): Long {
        val m = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        return m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, dayStart(), now)
            .filter { it.packageName !in skip }.sumOf { it.totalTimeInForeground }
    }

    fun usageMs(): Long {
        val base = if (p.getLong("baseDay", 0) == dayStart()) p.getLong("base", 0) else 0
        return (todayUsageRaw() - base).coerceAtLeast(0)
    }
}
