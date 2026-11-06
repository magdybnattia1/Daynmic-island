package com.example.dynamicisland

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 60, 36, 36)
        }
        val title = TextView(this).apply {
            text = "Dynamic Island"
            textSize = 28f
        }
        val info = TextView(this).apply {
            text = "فعّل صلاحية الظهور فوق التطبيقات، ثم شغّل الكبسولة."
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        val permission = Button(this).apply {
            text = "السماح بالظهور فوق التطبيقات"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                    startActivity(i)
                }
            }
        }
        val start = Button(this).apply {
            text = "تشغيل الكبسولة"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, IslandService::class.java))
                } else {
                    info.text = "امنح الصلاحية أولاً ثم اضغط تشغيل."
                }
            }
        }
        val stop = Button(this).apply {
            text = "إيقاف الكبسولة"
            setOnClickListener {
                stopService(Intent(this@MainActivity, IslandService::class.java))
            }
        }
        layout.addView(title); layout.addView(info)
        layout.addView(permission); layout.addView(start); layout.addView(stop)
        setContentView(layout)
    }
}
