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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object FoodyNotifier {
    private const val CHANNEL = "foody_orders"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "ออเดอร์และสถานะอาหาร",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description="แจ้งเตือนออเดอร์ใหม่และอาหารพร้อมเสิร์ฟ" }
        )
    }

    fun show(context: Context, event: RestaurantEvent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        createChannel(context)
        val title = when (event.kind) {
            "ORDER_NEW" -> "มีออเดอร์เข้าครัว"
            "ORDER_READY" -> "อาหารพร้อมเสิร์ฟ"
            "SALE_COMPLETE" -> "มีรายการขายใหม่"
            else -> "Foody"
        }
        val body = "โต๊ะ ${event.tableId}" + (event.orderId?.let {" • ออเดอร์ #$it"} ?: "")
        val intent=Intent(context,MainActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending=PendingIntent.getActivity(
            context,0,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notice=NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(event.id.toInt(),notice)
    }
}
