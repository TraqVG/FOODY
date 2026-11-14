package com.foody.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

// Real Android local notifications generated from role-specific server events.
// Foreground polling is intentionally used for this classroom prototype; closed-app
// real-time delivery would require FCM or a background delivery service.
class FoodyNotifier(private val context: Context) {
    private val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId="foody_orders"
    init {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                channelId, "ออเดอร์ร้านอาหาร", NotificationManager.IMPORTANCE_DEFAULT
            ).apply{description="แจ้งออเดอร์ใหม่และอาหารพร้อมเสิร์ฟ"})
        }
    }
    fun notify(event: EventDto) {
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        val (title,message) = when(event.kind) {
            "ORDER_NEW" -> "มีออเดอร์ใหม่" to "โต๊ะ ${event.tableId} · ออเดอร์ #${event.orderId}"
            "ORDER_READY" -> "อาหารพร้อมเสิร์ฟ" to "โต๊ะ ${event.tableId} · ออเดอร์ #${event.orderId}"
            "SALE_COMPLETE" -> "มีรายการขายใหม่" to "โต๊ะ ${event.tableId} เช็กบิลแล้ว"
            else -> return
        }
        val launch=Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending=PendingIntent.getActivity(context,event.id.toInt(),launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=NotificationCompat.Builder(context,channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(event.id.toInt(),notification)
    }
}
