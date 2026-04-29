package com.yakuphanuslu.regltakip

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(val context: Context) {
    private val CHANNEL_ID = "regl_reminder_channel"
    private val NOTIFICATION_ID = 101

    @SuppressLint("MissingPermission")
    fun showReminderNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Android 8.0+ için Bildirim Kanalı Oluşturma
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Regl Hatırlatıcı",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Günlük not ekleme hatırlatıcıları"
            }
            manager.createNotificationChannel(channel)
        }

        // 2. Bildirime Tıklayınca Açılacak Ekran (MainActivity)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Android 13+ (API 33) İzin Kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // İzin yoksa bildirim gönderemeyiz, burada duruyoruz.
                return
            }
        }

        // 4. Bildirimi İnşa Etme
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Bugün Nasıl Hissediyorsun? ✨")
            .setContentText("Döngün devam ediyor, bugünkü notlarını eklemeyi unutma!")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // İstersen buraya kendi ikonunu koyabilirsin
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 5. Bildirimi Gönderme (Kırmızılık Gitti!)
        manager.notify(NOTIFICATION_ID, notification)
    }
}