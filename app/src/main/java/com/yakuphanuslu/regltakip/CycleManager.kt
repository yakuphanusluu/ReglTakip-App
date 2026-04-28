package com.yakuphanuslu.regltakip

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class PhaseData(val name: String, val description: String, val color: String)

object CycleManager {
    fun getPhaseForDate(dateStr: String, allEntries: List<DayEntry>): PhaseData {
        val lastStart = allEntries.filter { it.isPeriodStart }.maxByOrNull {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.date)?.time ?: 0L
        } ?: return PhaseData("Hoş Geldin!", "Bir başlangıç günü seç kanka.", "NONE")

        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val start = sdf.parse(lastStart.date)
            val current = sdf.parse(dateStr)

            val diffDays = TimeUnit.DAYS.convert(current!!.time - start!!.time, TimeUnit.MILLISECONDS).toInt()

            // KRİTİK: Sadece 0 ile 28 gün arasını boyar, fazlasına dokunmaz
            when {
                diffDays in 0..4 -> PhaseData("Regl Dönemi 🩸", "Vücudun yenileniyor kanka.", "#E91E63")
                diffDays in 5..12 -> PhaseData("Foliküler Faz 🌱", "Enerjin artıyor!", "#9C27B0")
                diffDays in 13..15 -> PhaseData("Yumurtlama ✨", "En enerjik günler!", "#FF9800")
                diffDays in 16..27 -> PhaseData("Luteal Faz 🍂", "Kendine zaman ayır.", "#3F51B5")
                else -> PhaseData("Döngü Tamamlandı", "Bir sonraki kaydı bekle.", "NONE")
            }
        } catch (e: Exception) { PhaseData("Hata", "Hesaplanamadı.", "NONE") }
    }

    fun calculateNextPeriod(allEntries: List<DayEntry>): String {
        val lastStart = allEntries.filter { it.isPeriodStart }.maxByOrNull { it.id } ?: return "Veri bekleniyor..."
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(lastStart.date)!!
        cal.add(Calendar.DAY_OF_YEAR, 28)
        return "Tahmini Gelecek Regl: ${sdf.format(cal.time)} 🌸"
    }
}