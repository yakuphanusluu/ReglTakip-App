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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
        val uid = prefs.getInt("user_id", -1)

        lifecycleScope.launch {
            val list = db.dayDao().getAllEntries(uid)

            rvHistory.adapter = HistoryAdapter(list) { entry ->
                lifecycleScope.launch {
                    // 1. Yerel Veritabanından (Room) Sil
                    db.dayDao().delete(entry)

                    // 2. Bulut Veritabanından (MySQL) Sil 🚀
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://yakuphanuslu.com/regl_api/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val api = retrofit.create(ApiService::class.java)

                    api.deleteDay(uid = uid, date = entry.date).enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            // Buluttan başarıyla silindiğinde bir şey yapmaya gerek yok, yerel zaten silindi
                        }
                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            // İnternet hatası olsa bile yerel silindiği için kullanıcıyı etkilemez
                        }
                    })

                    onResume() // Listeyi yenilemek için
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
        h.date.text = item.date
        h.badge.visibility = if (item.isPeriodStart) View.VISIBLE else View.GONE
        h.emotions.text = if(item.emotions.isNotEmpty()) item.emotions else "Belirtilmedi"
        h.pain.text = item.painLevel
        h.energy.text = item.energyLevel
        h.notes.text = if(item.notes.isNotEmpty()) item.notes else "Not eklenmemiş"
        h.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size
}