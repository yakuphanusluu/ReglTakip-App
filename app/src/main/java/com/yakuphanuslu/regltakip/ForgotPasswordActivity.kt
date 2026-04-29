package com.yakuphanuslu.regltakip

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // --- DİLİ BAŞLATMA ---
        val prefs = getSharedPreferences("regl_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_lang", "tr") ?: "tr"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<EditText>(R.id.etForgotEmail)
        val btnSubmit = findViewById<Button>(R.id.btnForgotSubmit)
        val btnBack = findViewById<TextView>(R.id.btnBackToLoginFromForgot) // YENİ: Geri butonu

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/regl_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        // --- YENİ: Geri Tuşuna Basılınca Sayfayı Kapat ---
        btnBack.setOnClickListener {
            finish()
        }

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty() || !email.contains("@")) {
                Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.forgotPassword(email = email).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val msg = response.body()?.message ?: getString(R.string.process_completed)
                    Toast.makeText(this@ForgotPasswordActivity, msg, Toast.LENGTH_LONG).show()

                    if (response.body()?.status == "success") {
                        finish() // İşlem başarılıysa giriş ekranına geri dön
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@ForgotPasswordActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}