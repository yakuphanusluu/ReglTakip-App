package com.yakuphanuslu.regltakip

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate // EKLENDİ
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDate = sdf.format(Date())

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvPrediction: TextView
    private lateinit var tvPhaseTitle: TextView
    private lateinit var tvPhaseDesc: TextView
    private lateinit var llPhaseBg: LinearLayout
    private lateinit var etNotes: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var rgPain: RadioGroup
    private lateinit var rgEnergy: RadioGroup
    private lateinit var cbPeriodStart: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 İŞTE O SATIR: Karanlık modu devre dışı bırakır, tasarımın bozulmasını önler
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View Bağlantıları
        db = AppDatabase.getDatabase(this)
        calendarView = findViewById(R.id.calendarView)
        tvPrediction = findViewById(R.id.tvPrediction)
        tvPhaseTitle = findViewById(R.id.tvPhaseTitle)
        tvPhaseDesc = findViewById(R.id.tvPhaseDesc)
        llPhaseBg = findViewById(R.id.llPhaseBg)
        cbPeriodStart = findViewById(R.id.cbPeriodStart)
        etNotes = findViewById(R.id.etNotes)
        chipGroup = findViewById(R.id.chipGroupEmotions)
        rgPain = findViewById(R.id.rgPain)
        rgEnergy = findViewById(R.id.rgEnergy)
        val btnSaveAll = findViewById<MaterialButton>(R.id.btnSaveAll)
        val btnEndPeriod = findViewById<MaterialButton>(R.id.btnEndPeriod)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // BUGÜNÜ OTOMATİK SEÇ
        val today = CalendarDay.today()
        calendarView.setSelectedDate(today)
        calendarView.setCurrentDate(today)

        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = String.format("%02d/%02d/%d", date.day, date.month, date.year)
            updateUI()
        }

        // --- REGL BİTTİ (ÖZET ÇIKARMA) MANTIĞI ---
        btnEndPeriod.setOnClickListener {
            lifecycleScope.launch {
                val all = db.dayDao().getAllEntries()
                val lastStart = all.filter { it.isPeriodStart }.maxByOrNull {
                    sdf.parse(it.date)?.time ?: 0L
                }

                if (lastStart != null) {
                    val startDT = sdf.parse(lastStart.date)!!
                    val endDT = sdf.parse(selectedDate)!!
                    val diff = (endDT.time - startDT.time) / (1000 * 60 * 60 * 24) + 1

                    if (diff > 0) {
                        val cycleDays = all.filter {
                            val d = sdf.parse(it.date)
                            d != null && !d.before(startDT) && !d.after(endDT)
                        }

                        val commonPain = cycleDays.groupBy { it.painLevel }.maxByOrNull { it.value.size }?.key ?: "Yok"
                        val commonEnergy = cycleDays.groupBy { it.energyLevel }.maxByOrNull { it.value.size }?.key ?: "Normal"

                        db.summaryDao().insertSummary(CycleSummary(
                            startDate = lastStart.date,
                            endDate = selectedDate,
                            duration = diff.toInt(),
                            avgPain = commonPain,
                            avgEnergy = commonEnergy
                        ))

                        Toast.makeText(this@MainActivity, "Döngü Tamamlandı: $diff Gün ✨", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@MainActivity, SummaryActivity::class.java))
                    } else {
                        Toast.makeText(this@MainActivity, "Bitiş tarihi başlangıçtan önce olamaz!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Önce başlangıç seçmelisin!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- GÜNÜ KAYDET VE EKRANI SIFIRLA MANTIĞI ---
        btnSaveAll.setOnClickListener {
            val emotions = (0 until chipGroup.childCount).map { chipGroup.getChildAt(it) as Chip }.filter { it.isChecked }.joinToString(", ") { it.text }
            val pain = findViewById<RadioButton>(rgPain.checkedRadioButtonId)?.text?.toString() ?: "Yok"
            val energy = findViewById<RadioButton>(rgEnergy.checkedRadioButtonId)?.text?.toString() ?: "Normal"

            lifecycleScope.launch {
                db.dayDao().insert(DayEntry(
                    date = selectedDate,
                    emotions = emotions,
                    painLevel = pain,
                    energyLevel = energy,
                    notes = etNotes.text.toString(),
                    isPeriodStart = cbPeriodStart.isChecked
                ))
                Toast.makeText(this@MainActivity, "Gün Kaydedildi! 🩸", Toast.LENGTH_SHORT).show()

                // KAYDETTİKTEN SONRA EKRANI SIFIRLAMA
                cbPeriodStart.isChecked = false
                chipGroup.clearCheck()
                rgPain.clearCheck()
                rgEnergy.clearCheck()
                etNotes.text.clear()

                updateUI()
            }
        }

        // NAVİGASYON
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_history -> { startActivity(Intent(this, HistoryActivity::class.java)); false }
                R.id.nav_summary -> { startActivity(Intent(this, SummaryActivity::class.java)); false }
                else -> true
            }
        }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            val all = db.dayDao().getAllEntries()
            val phase = CycleManager.getPhaseForDate(selectedDate, all)

            tvPhaseTitle.text = phase.name
            tvPhaseDesc.text = phase.description
            llPhaseBg.setBackgroundColor(Color.parseColor(if(phase.color=="NONE") "#9E9E9E" else phase.color))
            tvPrediction.text = CycleManager.calculateNextPeriod(all)

            calendarView.removeDecorators()
            val lastStart = all.filter { it.isPeriodStart }.maxByOrNull { sdf.parse(it.date)?.time ?: 0L }
            if (lastStart != null) {
                val startCal = Calendar.getInstance().apply { time = sdf.parse(lastStart.date)!! }
                for (i in 0..27) {
                    val tempCal = startCal.clone() as Calendar
                    tempCal.add(Calendar.DAY_OF_YEAR, i)
                    val dateStr = sdf.format(tempCal.time)
                    val dayPhase = CycleManager.getPhaseForDate(dateStr, all)
                    if (dayPhase.color != "NONE") {
                        val day = CalendarDay.from(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH) + 1, tempCal.get(Calendar.DAY_OF_MONTH))
                        calendarView.addDecorator(object : DayViewDecorator {
                            override fun shouldDecorate(d: CalendarDay?) = d == day
                            override fun decorate(v: DayViewFacade?) {
                                v?.addSpan(DotSpan(10f, Color.parseColor(dayPhase.color)))
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}