package com.yakuphanuslu.regltakip

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var rvHistory: RecyclerView
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        db = AppDatabase.getDatabase(this)
        rvHistory = findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)

        findViewById<BottomNavigationView>(R.id.bottomNavigationHistory).apply {
            selectedItemId = R.id.nav_history
            setOnItemSelectedListener {
                when(it.itemId) {
                    R.id.nav_home -> { startActivity(Intent(this@HistoryActivity, MainActivity::class.java)); false }
                    R.id.nav_summary -> { startActivity(Intent(this@HistoryActivity, SummaryActivity::class.java)); false }
                    else -> true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
        val uid = prefs.getInt("user_id", -1)

        lifecycleScope.launch {
            val allEntries = db.dayDao().getAllEntries(uid)

            // --- YENİ: Tarihe göre en yeniden en eskiye sıralama mantığı ---
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val sortedList = allEntries.sortedByDescending { entry ->
                try {
                    sdf.parse(entry.date)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }

            // Sıralanmış listeyi (sortedList) adaptöre veriyoruz
            rvHistory.adapter = HistoryAdapter(sortedList) { entry ->
                lifecycleScope.launch {
                    db.dayDao().delete(entry)

                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://yakuphanuslu.com/regl_api/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val api = retrofit.create(ApiService::class.java)

                    api.deleteDay(uid = uid, date = entry.date).enqueue(object : retrofit2.Callback<ApiResponse> {
                        override fun onResponse(call: retrofit2.Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {}
                        override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {}
                    })
                    onResume() // Sildikten sonra listeyi yenile
                }
            }
        }
    }
}

class HistoryAdapter(private val list: List<DayEntry>, private val onDelete: (DayEntry) -> Unit) : RecyclerView.Adapter<HistoryAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val date: TextView = v.findViewById(R.id.tvHistoryDate)
        val badge: TextView = v.findViewById(R.id.tvPeriodStartBadge)
        val emotions: TextView = v.findViewById(R.id.cgHistoryEmotions)
        val pain: TextView = v.findViewById(R.id.tvHistoryPain)
        val energy: TextView = v.findViewById(R.id.tvHistoryEnergy)
        val notes: TextView = v.findViewById(R.id.tvHistoryNotes)
        val btnDelete: TextView = v.findViewById(R.id.btnDeleteEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(h: VH, p: Int) {
        val item = list[p]
        val context = h.itemView.context

        h.date.text = item.date
        h.badge.visibility = if (item.isPeriodStart) View.VISIBLE else View.GONE

        // --- 1. DUYGULARI ÇEVİR (contains ile yakalama) ---
        val rawEmotions = item.emotions
        val translatedEmotions = rawEmotions
            .replace("Mutlu 🥰", context.getString(R.string.happy))
            .replace("Hassas \uD83E\uDD7A", context.getString(R.string.sensitive))
            .replace("Sinirli \uD83D\uDE20", context.getString(R.string.angry))

        h.emotions.text = translatedEmotions.ifEmpty { context.getString(R.string.not_specified) }

        // --- 2. AĞRI SEVİYESİNİ ÇEVİR ---
        val rawPain = item.painLevel
        h.pain.text = when {
            rawPain.contains("Şiddetli", true) -> context.getString(R.string.severe)
            rawPain.contains("Hafif", true) -> context.getString(R.string.light_pain)
            rawPain.contains("Yok", true) -> context.getString(R.string.none)
            else -> rawPain
        }

        // --- 3. ENERJİ SEVİYESİNİ ÇEVİR ---
        val rawEnergy = item.energyLevel
        h.energy.text = when {
            rawEnergy.contains("Bitkin", true) -> context.getString(R.string.exhausted)
            rawEnergy.contains("Enerjik", true) -> context.getString(R.string.energetic)
            rawEnergy.contains("Normal", true) -> context.getString(R.string.normal)
            else -> rawEnergy
        }

        // --- 4. NOTLARI VE SİL BUTONUNU ÇEVİR ---
        h.notes.text = item.notes.ifEmpty { context.getString(R.string.no_notes) }
        h.btnDelete.text = context.getString(R.string.delete)

        h.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size
}