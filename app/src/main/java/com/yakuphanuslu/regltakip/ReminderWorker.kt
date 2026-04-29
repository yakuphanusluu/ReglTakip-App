package com.yakuphanuslu.regltakip

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val prefs = applicationContext.getSharedPreferences("regl_prefs", Context.MODE_PRIVATE)
        val uid = prefs.getInt("user_id", -1)

        if (uid == -1) return Result.success()

        // Veritabanı kontrolünü "blocking" olarak yapıyoruz çünkü arka plandayız
        runBlocking {
            val allEntries = db.dayDao().getAllEntries(uid)
            val allSummaries = db.summaryDao().getAllSummaries(uid)

            // En son regl başlangıcını bul
            val lastStart = allEntries.filter { it.isPeriodStart }.maxByOrNull { it.date }

            if (lastStart != null) {
                // Bu başlangıç tarihi için bir özet çıkarılmış mı?
                val isFinished = allSummaries.any { it.startDate == lastStart.date }

                // Eğer özet yoksa, hala regl dönemindedir
                if (!isFinished) {
                    NotificationHelper(applicationContext).showReminderNotification()
                }
            }
        }

        return Result.success()
    }
}