package com.yakuphanuslu.regltakip

import retrofit2.Call
import retrofit2.http.*

// Veri yapısını güncelledik: Artık 'data' alanı ile veritabanındaki listeleri de çekebiliyoruz
data class ApiResponse(
    val status: String,
    val user_id: Int? = null,
    val message: String? = null,
    val data: List<Map<String, Any>>? = null // Yeni: Gelen gün veya özet listesi için
)

interface ApiService {

    // --- 1. KAYIT OLMA (E-POSTA İLE) ---
    @FormUrlEncoded
    @POST("api.php")
    fun register(
        @Field("action") action: String = "register",
        @Field("email") email: String,
        @Field("password") p: String
    ): Call<ApiResponse>

    // --- 2. GİRİŞ YAPMA (E-POSTA İLE) ---
    @FormUrlEncoded
    @POST("api.php")
    fun login(
        @Field("action") action: String = "login",
        @Field("email") email: String,
        @Field("password") p: String
    ): Call<ApiResponse>

    // --- 3. GÜNLÜK VERİ SENKRONİZASYONU ---
    @FormUrlEncoded
    @POST("api.php")
    fun syncDay(
        @Field("action") action: String = "sync_day",
        @Field("user_id") uid: Int,
        @Field("date") date: String,
        @Field("emotions") emotions: String,
        @Field("pain") pain: String,
        @Field("energy") energy: String,
        @Field("notes") notes: String,
        @Field("isStart") isStart: Int
    ): Call<ApiResponse>

    // --- 4. DÖNGÜ ÖZETİ SENKRONİZASYONU ---
    @FormUrlEncoded
    @POST("api.php")
    fun syncSummary(
        @Field("action") action: String = "sync_summary",
        @Field("user_id") uid: Int,
        @Field("startDate") sDate: String,
        @Field("endDate") eDate: String,
        @Field("duration") dur: Int,
        @Field("avgPain") aPain: String,
        @Field("avgEnergy") aEng: String
    ): Call<ApiResponse>

    // --- 5. GÜNLÜK VERİ SİLME ---
    @FormUrlEncoded
    @POST("api.php")
    fun deleteDay(
        @Field("action") action: String = "delete_day",
        @Field("user_id") uid: Int,
        @Field("date") date: String
    ): Call<ApiResponse>

    // --- 6. DÖNGÜ ÖZETİ SİLME ---
    @FormUrlEncoded
    @POST("api.php")
    fun deleteSummary(
        @Field("action") action: String = "delete_summary",
        @Field("user_id") uid: Int,
        @Field("startDate") sDate: String,
        @Field("endDate") eDate: String
    ): Call<ApiResponse>

    // --- 7. ŞİFREMİ UNUTTUM ---
    @FormUrlEncoded
    @POST("api.php")
    fun forgotPassword(
        @Field("action") action: String = "forgot_password",
        @Field("email") email: String
    ): Call<ApiResponse>

    // --- 8. MAİL DEĞİŞİMİ İÇİN KOD GÖNDER (ESKİ MAİLE) ---
    @FormUrlEncoded
    @POST("api.php")
    fun requestEmailChange(
        @Field("action") action: String = "request_email_change",
        @Field("user_id") uid: Int,
        @Field("new_email") newEmail: String
    ): Call<ApiResponse>

    // --- 9. KODU DOĞRULA VE MAİLİ GÜNCELLE ---
    @FormUrlEncoded
    @POST("api.php")
    fun updateEmail(
        @Field("action") action: String = "update_email",
        @Field("user_id") uid: Int,
        @Field("new_email") newEmail: String,
        @Field("code") code: String
    ): Call<ApiResponse>

    // --- 10. VERİ ÇEKME: TÜM GÜNLERİ GETİR (YENİ) ---
    @FormUrlEncoded
    @POST("api.php")
    fun getDays(
        @Field("action") action: String = "get_days",
        @Field("user_id") uid: Int
    ): Call<ApiResponse>

    // --- 11. VERİ ÇEKME: TÜM ÖZETLERİ GETİR (YENİ) ---
    @FormUrlEncoded
    @POST("api.php")
    fun getSummaries(
        @Field("action") action: String = "get_summaries",
        @Field("user_id") uid: Int
    ): Call<ApiResponse>
}