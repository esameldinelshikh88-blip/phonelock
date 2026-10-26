package com.example.phonelock

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class MonitorService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var store: Store
    private var warned = false

    private val tick = object : Runnable {
        override fun run() {
            if (store.remainingMs() > 0) {
                startActivity(Intent(this@MonitorService, LockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                h.postDelayed(this, 3000)
                return
            }
            val used = store.usageMs(); val limit = store.limitMin * 60_000L
            if (used >= limit) { store.lockNow(); warned = false; h.post(this); return }
            if (!warned && used >= limit * 0.8) {
                warned = true
                notify(2, "اقترب وقت القفل", "تبقّى أقل من 20% من وقتك اليوم")
            }
            h.postDelayed(this, 15_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("m", "المراقبة", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "m")
            .setContentTitle("قفل الهاتف يعمل")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock).build())
        h.post(tick)
    }

    private fun notify(id: Int, t: String, b: String) =
        getSystemService(NotificationManager::class.java).notify(id,
            Notification.Builder(this, "m").setContentTitle(t).setContentText(b)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock).build())

    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
}
