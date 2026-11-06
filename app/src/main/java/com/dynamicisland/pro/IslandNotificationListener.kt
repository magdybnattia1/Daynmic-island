package com.dynamicisland.pro

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras=sbn.notification.extras
        val title=extras.getCharSequence("android.title")?.toString().orEmpty()
        val body=extras.getCharSequence("android.text")?.toString().orEmpty()
        if(title.isBlank() && body.isBlank()) return
        val app=try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName,0)).toString() } catch (_:Exception) { sbn.packageName }
        sendBroadcast(Intent(IslandService.ACTION_NOTIFICATION).setPackage(packageName).putExtra("title","$app • $title").putExtra("body",body))
    }
}
