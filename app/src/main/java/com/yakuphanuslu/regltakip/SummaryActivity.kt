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
                if(it.itemId == R.id.nav_home) startActivity(Intent(this@SummaryActivity, MainActivity::class.java))
                if(it.itemId == R.id.nav_history) startActivity(Intent(this@SummaryActivity, HistoryActivity::class.java))
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val list = db.summaryDao().getAllSummaries()
            rv.adapter = SummaryAdapter(list) { summary ->
                lifecycleScope.launch { db.summaryDao().deleteSummary(summary); onResume() }
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
        val notes: TextView = v.findViewById(R.id.tvHistoryNotes) // Not alanı
        val btnDelete: TextView = v.findViewById(R.id.btnDeleteEntry)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))
    override fun onBindViewHolder(h: VH, p: Int) {
        val item = list[p]
        h.dates.text = "${item.startDate} - ${item.endDate}"
        h.badge.apply { visibility = View.VISIBLE; text = "${item.duration} Gün Sürdü ✨" }
        h.title.text = "Tamamlanan Döngü Özeti"
        h.pain.text = "Genel Ağrı: ${item.avgPain}"
        h.energy.text = "Genel Enerji: ${item.avgEnergy}"
        h.notes.visibility = View.GONE // NOT KISMINI KALDIRDIK
        h.btnDelete.setOnClickListener { onDelete(item) }
    }
    override fun getItemCount() = list.size
}