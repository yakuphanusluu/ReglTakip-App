package com.yakuphanuslu.regltakip

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

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
                if(it.itemId == R.id.nav_home) startActivity(Intent(this@HistoryActivity, MainActivity::class.java))
                if(it.itemId == R.id.nav_summary) startActivity(Intent(this@HistoryActivity, SummaryActivity::class.java))
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val list = db.dayDao().getAllEntries()
            rvHistory.adapter = HistoryAdapter(list) { entry ->
                lifecycleScope.launch { db.dayDao().delete(entry); onResume() }
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
        val notes: TextView = v.findViewById(R.id.tvHistoryNotes) // BU SATIR EKLENDİ
        val btnDelete: TextView = v.findViewById(R.id.btnDeleteEntry)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))
    override fun onBindViewHolder(h: VH, p: Int) {
        val item = list[p]
        h.date.text = item.date
        h.badge.visibility = if (item.isPeriodStart) View.VISIBLE else View.GONE
        h.emotions.text = if(item.emotions.isNotEmpty()) item.emotions else "Belirtilmedi"
        h.pain.text = item.painLevel
        h.energy.text = item.energyLevel
        h.notes.text = if(item.notes.isNotEmpty()) item.notes else "Not eklenmemiş" // NOT BURADA BASILIYOR
        h.btnDelete.setOnClickListener { onDelete(item) }
    }
    override fun getItemCount() = list.size
}