package com.example.phonelock

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.util.Calendar

// مدة القفل عند تجاوز الحد اليومي (3 ساعات)
const val LOCK_MS = 3L * 60 * 60 * 1000

class Store(private val c: Context) {
    private val p = c.getSharedPreferences("s", Context.MODE_PRIVATE)

    // فترات القفل المجدولة: (بداية، نهاية) بالدقائق من منتصف الليل
    private val windows = listOf(
        (15 * 60) to (22 * 60),      // 3:00 م إلى 10:00 م
        (1 * 60) to (7 * 60 + 30)    // 1:00 ص إلى 7:30 ص
    )

    var limitMin: Int
        get() = p.getInt("limit", 180)
        set(v) = p.edit().putInt("limit", v).apply()

    private fun bootCount() = Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, 0)

    fun lockNow() = p.edit()
        .putLong("wall", System.currentTimeMillis())
        .putLong("elapsed", SystemClock.elapsedRealtime())
        .putInt("boot", bootCount())
        .putBoolean("locked", true).apply()

    /** المتبقي من فترة القفل المجدولة، أو 0 إن لم نكن داخل فترة */
    private fun scheduleRemainingMs(): Long {
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)
        for ((s, e) in windows) {
            if (nowMin >= s && nowMin < e) return ((e - nowMin) * 60L - sec) * 1000
        }
        return 0
    }

    /** المتبقي من قفل تجاوز الحد اليومي */
    private fun limitRemainingMs(): Long {
        if (!p.getBoolean("locked", false)) return 0
        val passed = if (p.getInt("boot", -1) == bootCount())
            SystemClock.elapsedRealtime() - p.getLong("elapsed", 0)
        else System.currentTimeMillis() - p.getLong("wall", 0)
        val left = LOCK_MS - passed
        if (left <= 0) { unlock(); return 0 }
        return left
    }

    /** أكبر المدتين: الجدول أو الحد اليومي. 0 = غير مقفل */
    fun remainingMs(): Long = maxOf(scheduleRemainingMs(), limitRemainingMs())

    private fun unlock() {
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
