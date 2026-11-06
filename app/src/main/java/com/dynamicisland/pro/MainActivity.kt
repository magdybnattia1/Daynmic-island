package com.dynamicisland.pro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var info: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,32,24,24); setBackgroundColor(Color.rgb(15,18,27)) }
        root.addView(TextView(this).apply { text="Dynamic Island Pro"; textSize=28f; gravity=Gravity.CENTER; setTextColor(Color.WHITE) })
        info=TextView(this).apply { text="فعّل الصلاحيات ثم شغّل الكبسولة"; textSize=15f; setTextColor(Color.LTGRAY); setPadding(0,16,0,16) }
        root.addView(info)
        button(root,"السماح بالظهور فوق التطبيقات") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        button(root,"تفعيل إشعارات التطبيقات وواتساب") { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
        button(root,"السماح بقراءة حالة المكالمات") {
            if (Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE),2)
            else toast("الصلاحية مفعّلة")
        }
        button(root,"تشغيل Dynamic Island") {
            if (!Settings.canDrawOverlays(this)) toast("فعّل صلاحية الظهور فوق التطبيقات أولاً")
            else { startService(Intent(this,IslandService::class.java)); info.text="الكبسولة تعمل الآن"; toast("تم التشغيل") }
        }
        button(root,"إيقاف Dynamic Island") { stopService(Intent(this,IslandService::class.java)); info.text="تم إيقاف الكبسولة" }
        button(root,"اللون: بنفسجي") { appearance("#8B5CF6",1f) }
        button(root,"اللون: أزرق") { appearance("#2563EB",1f) }
        button(root,"اللون: أخضر") { appearance("#16A34A",1f) }
        button(root,"اللون: أسود") { appearance("#080808",1f) }
        button(root,"الحجم: صغير") { appearance(null,0.85f) }
        button(root,"الحجم: عادي") { appearance(null,1f) }
        button(root,"الحجم: كبير") { appearance(null,1.2f) }
        root.addView(TextView(this).apply {
            text="المميزات: إشعارات التطبيقات وواتساب، الموسيقى والتحكم، الشحن ونسبة البطارية، حالة المكالمات، وتخصيص اللون والحجم. افتح الكبسولة لعرض أدوات الموسيقى."
            textSize=13f; setTextColor(Color.LTGRAY); setPadding(0,16,0,0)
        })
        val timerTitle=TextView(this).apply { text="المؤقت / ساعة الإيقاف"; textSize=18f; setTextColor(Color.WHITE); setPadding(0,18,0,4) }
        val clock=Chronometer(this).apply { textSize=24f; setTextColor(Color.WHITE); base=SystemClock.elapsedRealtime() }
        root.addView(timerTitle)
        root.addView(clock)
        button(root,"بدء المؤقت") { clock.base=SystemClock.elapsedRealtime(); clock.start() }
        button(root,"إيقاف المؤقت") { clock.stop() }
        button(root,"إعادة ضبط المؤقت") { clock.stop(); clock.base=SystemClock.elapsedRealtime() }
        setContentView(root)
        if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),3)
    }
    private fun button(root:LinearLayout,label:String,action:()->Unit) { root.addView(Button(this).apply { text=label; setOnClickListener { action() } }) }
    private fun appearance(color:String?,size:Float) {
        val p=getSharedPreferences("island",MODE_PRIVATE).edit().putFloat("size",size)
        if(color!=null) p.putString("color",color)
        p.apply()
        sendBroadcast(Intent(IslandService.ACTION_REFRESH).setPackage(packageName))
        toast("تم حفظ المظهر")
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
