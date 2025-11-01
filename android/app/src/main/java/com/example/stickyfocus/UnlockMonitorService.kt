package com.example.stickyfocus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class UnlockMonitorService : Service() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                val i = Intent(this@UnlockMonitorService, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(i)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        startForeground(1001, createNotification())
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // ignore
        }
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun createNotification(): android.app.Notification {
        val channelId = "unlock_monitor"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = nm.getNotificationChannel(channelId)
            if (channel == null) {
                channel = NotificationChannel(channelId, "Unlock Monitor", NotificationManager.IMPORTANCE_LOW)
                nm.createNotificationChannel(channel)
            }
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("StickyFocus")
            .setContentText("Monitoring unlock events")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
