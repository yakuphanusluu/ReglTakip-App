package com.yakuphanuslu.regltakip

import android.content.Context
import androidx.room.*

// --- ENTITIES (Değişmedi) ---

@Entity(tableName = "days")
data class DayEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
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
    val userId: Int,
    val startDate: String,
    val endDate: String,
    val duration: Int,
    val avgPain: String,
    val avgEnergy: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAOs (YENİ SORGULAR EKLENDİ) ---

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE userId = :userId ORDER BY id DESC")
    suspend fun getAllEntries(userId: Int): List<DayEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dayEntry: DayEntry)

    // YENİ: Senkronizasyon öncesi mükerrer kayıtları önlemek için kullanıcı verilerini temizler
    @Query("DELETE FROM days WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int)

    @Delete
    suspend fun delete(dayEntry: DayEntry)
}

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllSummaries(userId: Int): List<CycleSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: CycleSummary)

    // YENİ: Özetlerin üst üste binmemesi için temizlik yapar
    @Query("DELETE FROM summaries WHERE userId = :userId")
    suspend fun deleteSummariesByUserId(userId: Int)

    @Delete
    suspend fun deleteSummary(summary: CycleSummary)
}

// --- DATABASE (Değişmedi) ---

@Database(entities = [DayEntry::class, CycleSummary::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "regl_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}