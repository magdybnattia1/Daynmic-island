package com.dynamicisland.pro

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class IslandService : Service() {
    companion object {
        const val ACTION_NOTIFICATION="com.dynamicisland.pro.NOTIFICATION"
        const val ACTION_REFRESH="com.dynamicisland.pro.REFRESH"
    }
    private lateinit var wm:WindowManager
    private var capsule:LinearLayout?=null
    private var expanded=false
    private var battery="🔋 البطارية"
    private var alert=""
    private var call=""
    private var music=""
    private var percent=0
    private val receiver=object:BroadcastReceiver(){
        override fun onReceive(context:Context?,intent:Intent?){
            when(intent?.action){
                Intent.ACTION_BATTERY_CHANGED->{
                    val level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0)
                    val scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100).coerceAtLeast(1)
                    percent=(level*100/scale).coerceIn(0,100)
                    val state=intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1)
                    val charging=state==BatteryManager.BATTERY_STATUS_CHARGING || state==BatteryManager.BATTERY_STATUS_FULL
                    battery=if(charging) "⚡ جاري الشحن • $percent%" else "🔋 البطارية • $percent%"
                    render()
                }
                ACTION_NOTIFICATION->{ alert=listOf(intent.getStringExtra("title"),intent.getStringExtra("body")).filterNotNull().filter{it.isNotBlank()}.joinToString("\n"); render() }
                TelephonyManager.ACTION_PHONE_STATE_CHANGED->{
                    call=when(intent.getStringExtra(TelephonyManager.EXTRA_STATE)){
                        TelephonyManager.EXTRA_STATE_RINGING->"☎ مكالمة واردة"
                        TelephonyManager.EXTRA_STATE_OFFHOOK->"☎ المكالمة جارية"
                        TelephonyManager.EXTRA_STATE_IDLE->""
                        else->call
                    }; render()
                }
                ACTION_REFRESH->applyStyle()
            }
        }
    }
    override fun onCreate(){
        super.onCreate()
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val filter=IntentFilter().apply{
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(ACTION_NOTIFICATION)
            addAction(ACTION_REFRESH)
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }
        if(Build.VERSION.SDK_INT>=33) { registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED) } else { registerReceiver(receiver,filter) }
        buildCapsule()
        val sticky=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if(sticky!=null) receiver.onReceive(this,sticky)
    }
    private fun buildCapsule(){
        capsule=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER
            setPadding(16,7,16,7)
            background=GradientDrawable().apply{setColor(Color.BLACK);cornerRadius=90f}
            elevation=18f
            setOnClickListener{expanded=!expanded;render()}
        }
        val params=WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;y=8}
        wm.addView(capsule,params)
        applyStyle()
    }
    private fun applyStyle(){
        val prefs=getSharedPreferences("island",MODE_PRIVATE)
        val color=try{Color.parseColor(prefs.getString("color","#080808"))}catch(_:Exception){Color.BLACK}
        val size=prefs.getFloat("size",1f)
        capsule?.background=GradientDrawable().apply{setColor(color);cornerRadius=90f}
        capsule?.scaleX=size; capsule?.scaleY=size
        render()
    }
    private fun render(){
        val box=capsule?:return
        box.removeAllViews()
        val heading=when{
            call.isNotBlank()->call
            alert.isNotBlank()->"● إشعار جديد"
            else->battery
        }
        box.addView(TextView(this).apply{
            text=heading;setTextColor(Color.WHITE);textSize=13f;typeface=Typeface.DEFAULT_BOLD
            gravity=Gravity.CENTER;setPadding(12,3,12,3)
        })
        if(expanded){
            val content=listOfNotNull(alert.takeIf{it.isNotBlank()},battery,call.takeIf{it.isNotBlank()},music.takeIf{it.isNotBlank()}).joinToString("\n")
            box.addView(TextView(this).apply{text=content;setTextColor(Color.LTGRAY);textSize=11f;gravity=Gravity.CENTER;setPadding(8,5,8,5)})
            val row=LinearLayout(this).apply{gravity=Gravity.CENTER}
            fun control(label:String,fn:()->Unit){row.addView(Button(this).apply{text=label;textSize=10f;setOnClickListener{fn()}})}
            control("⏮"){media("prev")}
            control("▶ / Ⅱ"){media("toggle")}
            control("⏭"){media("next")}
            box.addView(row)
            box.addView(TextView(this).apply{text="اضغط على الكبسولة للإغلاق";setTextColor(Color.GRAY);textSize=9f;gravity=Gravity.CENTER})
        }else{
            val subtitle=if(alert.isNotBlank()) alert.take(64) else if(call.isNotBlank()) call else "اضغط لعرض التفاصيل"
            box.addView(TextView(this).apply{text=subtitle;setTextColor(Color.LTGRAY);textSize=10f;gravity=Gravity.CENTER})
        }
    }
    private fun media(action:String){
        try{
            val manager=getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions=manager.getActiveSessions(ComponentName(this,IslandNotificationListener::class.java))
            val session=sessions.firstOrNull()?:run{music="افتح تطبيق موسيقى وفعّل صلاحية الإشعارات";render();return}
            val controls=session.transportControls
            when(action){
                "prev"->controls.skipToPrevious()
                "next"->controls.skipToNext()
                else->if(session.playbackState?.state==PlaybackState.STATE_PLAYING) controls.pause() else controls.play()
            }
            music="♫ "+(session.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?: "التحكم بالموسيقى جاهز")
        }catch(_:Exception){music="فعّل صلاحية الوصول للإشعارات للتحكم بالموسيقى"}
        render()
    }
    override fun onDestroy(){
        try{unregisterReceiver(receiver)}catch(_:Exception){}
        capsule?.let{try{wm.removeView(it)}catch(_:Exception){}}
        capsule=null
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
