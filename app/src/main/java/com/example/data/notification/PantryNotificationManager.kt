package com.example.data.notification

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
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.PantryItem
import java.util.concurrent.TimeUnit

object PantryNotificationManager {

    const val CHANNEL_ID = "pantry_expiry_channel"
    private const val CHANNEL_NAME = "Pantry Expiration Alerts"
    private const val NOTIFICATION_ID_URGENT = 1001
    private const val NOTIFICATION_ID_DIGEST = 1002
    private const val NOTIFICATION_ID_TEST = 1003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Alerts when items in your pantry, fridge, or freezer are nearing their expiration date."
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkAndNotifyExpiringItems(context: Context, items: List<PantryItem>) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannel(context)

        val now = System.currentTimeMillis()
        val urgentItems = items.filter { item ->
            val diffMs = item.expiryDate - now
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
            daysRemaining <= 2
        }

        if (urgentItems.isEmpty()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "PANTRY")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (urgentItems.size == 1) {
            val item = urgentItems.first()
            val days = TimeUnit.MILLISECONDS.toDays(item.expiryDate - now).toInt()
            if (days < 0) "⚠️ ${item.name} has expired!"
            else if (days == 0) "⚠️ ${item.name} expires TODAY!"
            else "⚠️ ${item.name} expires in $days day${if (days > 1) "s" else ""}!"
        } else {
            "⚠️ ${urgentItems.size} Pantry Items Nearing Expiration"
        }

        val contentText = urgentItems.take(3).joinToString(", ") { item ->
            val days = TimeUnit.MILLISECONDS.toDays(item.expiryDate - now).toInt()
            val timeDesc = if (days < 0) "expired" else if (days == 0) "today" else "${days}d left"
            "${item.name} ($timeDesc)"
        } + if (urgentItems.size > 3) " and ${urgentItems.size - 3} more" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_URGENT, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }

    fun sendTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "PANTRY")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 FreshCart Pantry Alert")
            .setContentText("Organic Whole Milk expires in 1 day! Check your pantry stock.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Organic Whole Milk (Fridge) expires in 1 day.\nHoney Wheat Bread (Bakery) expires in 2 days.\nTap to review and add to grocery restock list.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
