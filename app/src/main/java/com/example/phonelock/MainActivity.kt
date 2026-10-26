package com.example.phonelock

import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private fun hasUsage(): Boolean {
        val a = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return a.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        val store = Store(this)
        val limit = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(store.limitMin.toString()); hint = "الحد اليومي بالدقائق"
        }
        fun btn(txt: String, f: () -> Unit) = Button(this).apply { text = txt; setOnClickListener { f() } }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48)
            addView(TextView(context).apply { text = "الحد اليومي (دقائق)، وبعده يُقفل الهاتف 24 ساعة"; textSize = 18f })
            addView(limit)
            addView(btn("1) إذن بيانات الاستخدام") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
            addView(btn("2) إذن العرض فوق التطبيقات") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            })
            addView(btn("3) حفظ وتشغيل") {
                if (!hasUsage() || !Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(context, "فعّل الإذنين أولاً", Toast.LENGTH_LONG).show()
                } else {
                    store.limitMin = limit.text.toString().toIntOrNull() ?: 180
                    startForegroundService(Intent(context, MonitorService::class.java))
                    Toast.makeText(context, "تم التشغيل", Toast.LENGTH_SHORT).show()
                }
            })
        })
    }
}
