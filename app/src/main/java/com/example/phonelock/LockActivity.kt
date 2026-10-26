package com.example.phonelock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class LockActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var store: Store
    private lateinit var t: TextView

    private val upd = object : Runnable {
        override fun run() {
            val left = store.remainingMs()
            if (left <= 0) { finish(); return }
            val s = left / 1000
            t.text = "الهاتف مقفل\n\nيفتح بعد\n%02d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
            h.postDelayed(this, 1000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        store = Store(this)
        t = TextView(this).apply { textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val call = Button(this).apply {
            text = "اتصال / طوارئ"
            setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK); setPadding(48, 48, 48, 48)
            addView(t); addView(call)
        })
        h.post(upd)
    }

    @Deprecated("blocked on purpose")
    override fun onBackPressed() {}
    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
}
