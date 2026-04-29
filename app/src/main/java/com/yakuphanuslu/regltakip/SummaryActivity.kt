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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        db = AppDatabase.getDatabase(this)
        rv = findViewById(R.id.rvSummaries)
        rv.layoutManager = LinearLayoutManager(this)

        findViewById<BottomNavigationView>(R.id.bottomNavigationSummary).apply {
            selectedItemId = R.id.nav_summary
            setOnItemSelectedListener {
                when(it.itemId) {
                    R.id.nav_home -> { startActivity(Intent(this@SummaryActivity, MainActivity::class.java)); false }
                    R.id.nav_history -> { startActivity(Intent(this@SummaryActivity, HistoryActivity::class.java)); false }
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
            val list = db.summaryDao().getAllSummaries(uid)

            // --- YENİ: Başlangıç tarihine göre en yeniden en eskiye sıralama mantığı ---
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val sortedList = list.sortedByDescending { summary ->
                try {
                    sdf.parse(summary.startDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }

            // Sıralanmış listeyi (sortedList) adaptöre veriyoruz
            rv.adapter = SummaryAdapter(sortedList) { summary ->
                lifecycleScope.launch {
                    db.summaryDao().deleteSummary(summary)
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://yakuphanuslu.com/regl_api/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val api = retrofit.create(ApiService::class.java)

                    api.deleteSummary(uid = uid, sDate = summary.startDate, eDate = summary.endDate)
                        .enqueue(object : Callback<ApiResponse> {
                            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
                            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                        })
                    onResume() // Sildikten sonra listeyi yenile
                }
            }
        }
    }
}

class SummaryAdapter(private val list: List<CycleSummary>, private val onDelete: (CycleSummary) -> Unit) : RecyclerView.Adapter<SummaryAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val dates: TextView = v.findViewById(R.id.tvHistoryDate)
        val badge: TextView = v.findViewById(R.id.tvPeriodStartBadge)
        val title: TextView = v.findViewById(R.id.cgHistoryEmotions)
        val pain: TextView = v.findViewById(R.id.tvHistoryPain)
        val energy: TextView = v.findViewById(R.id.tvHistoryEnergy)
        val notes: TextView = v.findViewById(R.id.tvHistoryNotes)
        val btnDelete: TextView = v.findViewById(R.id.btnDeleteEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(h: VH, p: Int) {
        val item = list[p]
        val context = h.itemView.context

        h.dates.text = "${item.startDate} - ${item.endDate}"

        // 1. SÜRE (Dinamik Formatlı: Lasted 5 Days ✨)
        h.badge.apply {
            visibility = View.VISIBLE
            text = context.getString(R.string.summary_duration_format, item.duration)
        }

        // 2. BAŞLIK
        h.title.text = context.getString(R.string.completed_cycle_summary)

        // 3. AĞRI VE ENERJİ (Veritabanındaki metinleri dile göre çeviriyoruz)
        val rawPain = item.avgPain
        val translatedPain = when {
            rawPain.contains("Şiddetli", true) -> context.getString(R.string.severe)
            rawPain.contains("Hafif", true) -> context.getString(R.string.light_pain)
            else -> context.getString(R.string.none)
        }
        h.pain.text = context.getString(R.string.avg_pain_format, translatedPain)

        val rawEnergy = item.avgEnergy
        val translatedEnergy = when {
            rawEnergy.contains("Bitkin", true) -> context.getString(R.string.exhausted)
            rawEnergy.contains("Enerjik", true) -> context.getString(R.string.energetic)
            else -> context.getString(R.string.normal)
        }
        h.energy.text = context.getString(R.string.avg_energy_format, translatedEnergy)

        h.notes.visibility = View.GONE
        h.btnDelete.text = context.getString(R.string.delete)
        h.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size
}