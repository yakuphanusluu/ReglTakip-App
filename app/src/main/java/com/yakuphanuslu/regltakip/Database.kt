package com.yakuphanuslu.regltakip

import android.content.Context
import androidx.room.*

@Entity(tableName = "days")
data class DayEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val emotions: String,
    val painLevel: String,
    val energyLevel: String,
    val notes: String,
    val isPeriodStart: Boolean = false
)

@Entity(tableName = "summaries")
data class CycleSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: String,
    val endDate: String,
    val duration: Int,
    val avgPain: String,    // YENİ
    val avgEnergy: String,  // YENİ
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DayDao {
    @Query("SELECT * FROM days ORDER BY id DESC")
    suspend fun getAllEntries(): List<DayEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dayEntry: DayEntry)
    @Delete
    suspend fun delete(dayEntry: DayEntry)
}

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    suspend fun getAllSummaries(): List<CycleSummary>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: CycleSummary)
    @Delete
    suspend fun deleteSummary(summary: CycleSummary)
}

@Database(entities = [DayEntry::class, CycleSummary::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun summaryDao(): SummaryDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "regl_ultra_db")
                    .fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}