package com.yakuphanuslu.regltakip

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.prolificinteractive.materialcalendarview.*
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    // Teknik işlemler (DB/API) için format standart kalmalı
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
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

    private lateinit var api: ApiService

    // --- GÜNCEL: Uygulamanın o anki aktif dilini bulan fonksiyon ---
    private fun getAppLocale(): Locale {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        return if (!currentLocales.isEmpty) {
            currentLocales.get(0) ?: Locale("tr")
        } else {
            val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
            val langCode = prefs.getString("app_lang", "tr") ?: "tr"
            Locale(langCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
        val currentUid = prefs.getInt("user_id", -1)
        db = AppDatabase.getDatabase(this)

        // --- VIEW TANIMLAMALARI ---
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
        val btnSettings = findViewById<TextView>(R.id.btnSettings)

        // --- TAKVİM YERELLEŞTİRME ---
        setupCalendarLocalization()

        // Retrofit Setup
        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/regl_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiService::class.java)

        fetchDataFromServer(currentUid)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val today = CalendarDay.today()
        calendarView.setSelectedDate(today)
        calendarView.setCurrentDate(today)

        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = String.format(Locale.US, "%02d/%02d/%d", date.day, date.month, date.year)
            updateUI()
        }

        // --- DÖNGÜ BİTİRME BUTONU ---
        btnEndPeriod.setOnClickListener {
            lifecycleScope.launch {
                val all = db.dayDao().getAllEntries(currentUid)
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

                        val commonPain = cycleDays.groupBy { it.painLevel }.maxByOrNull { it.value.size }?.key ?: getString(R.string.none)
                        val commonEnergy = cycleDays.groupBy { it.energyLevel }.maxByOrNull { it.value.size }?.key ?: getString(R.string.normal)

                        val summary = CycleSummary(
                            userId = currentUid,
                            startDate = lastStart.date,
                            endDate = selectedDate,
                            duration = diff.toInt(),
                            avgPain = commonPain,
                            avgEnergy = commonEnergy
                        )

                        db.summaryDao().insertSummary(summary)
                        syncSummaryWithCloud(summary)

                        Toast.makeText(this@MainActivity, getString(R.string.summary_created), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@MainActivity, SummaryActivity::class.java))
                    }
                }
            }
        }

        // --- KAYDET BUTONU ---
        btnSaveAll.setOnClickListener {
            val emotions = (0 until chipGroup.childCount)
                .map { chipGroup.getChildAt(it) }
                .filterIsInstance<Chip>()
                .filter { it.isChecked }
                .joinToString(", ") { it.text }

            val painValue = findViewById<RadioButton>(rgPain.checkedRadioButtonId)?.text?.toString() ?: getString(R.string.none)
            val energyValue = findViewById<RadioButton>(rgEnergy.checkedRadioButtonId)?.text?.toString() ?: getString(R.string.normal)

            val entry = DayEntry(
                userId = currentUid,
                date = selectedDate,
                emotions = emotions,
                painLevel = painValue,
                energyLevel = energyValue,
                notes = etNotes.text.toString(),
                isPeriodStart = cbPeriodStart.isChecked
            )

            lifecycleScope.launch {
                db.dayDao().insert(entry)
                Toast.makeText(this@MainActivity, getString(R.string.day_saved), Toast.LENGTH_SHORT).show()
                syncWithCloud(entry)

                cbPeriodStart.isChecked = false
                chipGroup.clearCheck()
                rgPain.clearCheck()
                rgEnergy.clearCheck()
                etNotes.text.clear()

                updateUI()
            }
        }

        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_history -> { startActivity(Intent(this, HistoryActivity::class.java)); false }
                R.id.nav_summary -> { startActivity(Intent(this, SummaryActivity::class.java)); false }
                else -> true
            }
        }

        checkNotificationPermission()
        setupDailyReminder()
    }

    // --- GÜNCEL: Takvim yerelleştirmesini yapan fonksiyon ---
    private fun setupCalendarLocalization() {
        val locale = getAppLocale()

        // 1. Üst Başlık (Nisan 2026 / April 2026)
        calendarView.setTitleFormatter { day ->
            val calendar = Calendar.getInstance()
            calendar.set(day.year, day.month - 1, 1)
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", locale)
            monthYearFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
        }

        // 2. Gün İsimleri (Pzt, Sal / Mon, Tue)
        calendarView.setWeekDayFormatter { dayOfWeek ->
            val calendar = Calendar.getInstance()
            val dayInt = (dayOfWeek.value % 7) + 1
            calendar.set(Calendar.DAY_OF_WEEK, dayInt)
            val weekDayFormat = SimpleDateFormat("EEE", locale)
            weekDayFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
        }
    }

    private fun fetchDataFromServer(uid: Int) {
        if (uid == -1) return
        api.getDays(uid = uid).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val body = response.body()
                if (body?.status == "success" && body.data != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.dayDao().deleteByUserId(uid)
                        body.data.forEach { map ->
                            val entry = DayEntry(
                                userId = (map["user_id"] as? Double)?.toInt() ?: uid,
                                date = map["date"] as String,
                                emotions = map["emotions"] as? String ?: "",
                                painLevel = map["painLevel"] as? String ?: getString(R.string.none),
                                energyLevel = map["energyLevel"] as? String ?: getString(R.string.normal),
                                notes = map["notes"] as? String ?: "",
                                isPeriodStart = (map["isPeriodStart"] as? Double)?.toInt() == 1
                            )
                            db.dayDao().insert(entry)
                        }
                        withContext(Dispatchers.Main) { updateUI() }
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })

        api.getSummaries(uid = uid).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val body = response.body()
                if (body?.status == "success" && body.data != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.summaryDao().deleteSummariesByUserId(uid)
                        body.data.forEach { map ->
                            val summary = CycleSummary(
                                userId = (map["user_id"] as? Double)?.toInt() ?: uid,
                                startDate = map["startDate"] as String,
                                endDate = map["endDate"] as String,
                                duration = (map["duration"] as? Double)?.toInt() ?: 0,
                                avgPain = map["avgPain"] as? String ?: getString(R.string.none),
                                avgEnergy = map["avgEnergy"] as? String ?: getString(R.string.normal)
                            )
                            db.summaryDao().insertSummary(summary)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun updateUI() {
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
                val uid = prefs.getInt("user_id", -1)

                val all = db.dayDao().getAllEntries(uid)
                val phase = CycleManager.getPhaseForDate(selectedDate, all)
                val phaseName = phase.name ?: ""

                // --- 1. DÜZELTME: FAZ İSİMLERİ, BOŞ DURUM VE DÖNGÜ TAMAMLANDI KONTROLÜ ---
                when {
                    phaseName.contains("Regl", ignoreCase = true) -> {
                        tvPhaseTitle.text = getString(R.string.period_phase)
                        tvPhaseDesc.text = getString(R.string.phase_desc)
                    }
                    phaseName.contains("Folik", ignoreCase = true) -> {
                        tvPhaseTitle.text = getString(R.string.follicular_phase)
                        tvPhaseDesc.text = getString(R.string.follicular_desc)
                    }
                    phaseName.contains("Yumurt", ignoreCase = true) -> {
                        tvPhaseTitle.text = getString(R.string.ovulation_phase)
                        tvPhaseDesc.text = getString(R.string.ovulation_desc)
                    }
                    phaseName.contains("Luteal", ignoreCase = true) -> {
                        tvPhaseTitle.text = getString(R.string.luteal_phase)
                        tvPhaseDesc.text = getString(R.string.luteal_desc)
                    }
                    phaseName.contains("Hoş Geldin", ignoreCase = true) || phaseName.isEmpty() -> {
                        tvPhaseTitle.text = getString(R.string.welcome_title)
                        tvPhaseDesc.text = getString(R.string.welcome_desc)
                    }
                    // YENİ: DÖNGÜ TAMAMLANDI YAKALAYICISI
                    phaseName.contains("Tamamlandı", ignoreCase = true) -> {
                        tvPhaseTitle.text = getString(R.string.cycle_completed_title)
                        tvPhaseDesc.text = getString(R.string.cycle_completed_desc)
                    }
                    else -> {
                        tvPhaseTitle.text = phaseName
                        tvPhaseDesc.text = phase.description ?: ""
                    }
                }

                try {
                    llPhaseBg.setBackgroundColor(Color.parseColor(if(phase.color.isNullOrEmpty() || phase.color == "NONE") "#9E9E9E" else phase.color))
                } catch (e: Exception) {
                    llPhaseBg.setBackgroundColor(Color.GRAY)
                }

                // --- 2. DÜZELTME: TAHMİN BÖLÜMÜ (VERİ BEKLENİYOR) ÇEVİRİSİ ---
                val rawPrediction = CycleManager.calculateNextPeriod(all)
                val dateRegex = """(\d{2}/\d{2}/\d{4})""".toRegex()
                val match = dateRegex.find(rawPrediction)

                val localizedDate = if (match != null) {
                    try {
                        val dateObj = sdf.parse(match.value)
                        val displayFormat = SimpleDateFormat("dd MMMM yyyy", getAppLocale())
                        displayFormat.format(dateObj!!)
                    } catch (e: Exception) { match.value }
                } else {
                    if (rawPrediction.contains("Veri", ignoreCase = true)) {
                        getString(R.string.waiting_data)
                    } else {
                        rawPrediction.replace("Tahmini Gelecek Regl: ", "").replace("🌸", "").trim()
                    }
                }

                tvPrediction.text = getString(R.string.prediction_text, localizedDate)

                // Takvim Dekorasyonu
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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onResume() {
        super.onResume()
        setupCalendarLocalization()
        updateUI()
    }

    // --- YARDIMCI FONKSİYONLAR ---

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupDailyReminder() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .addTag("ReglReminderWork")
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("ReglReminderWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 21)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis - now
    }

    private fun syncWithCloud(entry: DayEntry) {
        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
        val uid = prefs.getInt("user_id", -1)
        if (uid == -1) return
        api.syncDay(uid = uid, date = entry.date, emotions = entry.emotions, pain = entry.painLevel, energy = entry.energyLevel, notes = entry.notes, isStart = if(entry.isPeriodStart) 1 else 0).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun syncSummaryWithCloud(summary: CycleSummary) {
        val uid = getSharedPreferences("regl_prefs", MODE_PRIVATE).getInt("user_id", -1)
        if (uid == -1) return
        api.syncSummary(uid = uid, sDate = summary.startDate, eDate = summary.endDate, dur = summary.duration, aPain = summary.avgPain, aEng = summary.avgEnergy).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }
}