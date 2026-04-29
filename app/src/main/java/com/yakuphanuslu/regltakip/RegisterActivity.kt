package com.yakuphanuslu.regltakip

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog // YENİ EKLENDİ
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("regl_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_lang", "tr") ?: "tr"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/regl_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        val btnLanguageRegister = findViewById<TextView>(R.id.btnLanguageRegister) // YENİ DİL BUTONU
        btnLanguageRegister.text = "🌍 ${langCode.uppercase()}"

        // --- YENİ: DİL SEÇİM PENCERESİ ---
        btnLanguageRegister.setOnClickListener {
            val languages = arrayOf("Türkçe (TR)", "English (EN)", "Deutsch (DE)", "Русский (RU)", "Polski (PL)")
            val codes = arrayOf("tr", "en", "de", "ru", "pl")

            AlertDialog.Builder(this)
                .setTitle("🌍 " + getString(R.string.app_language))
                .setItems(languages) { _, which ->
                    val selectedCode = codes[which]
                    prefs.edit().putString("app_lang", selectedCode).apply()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedCode))
                }
                .show()
        }

        findViewById<Button>(R.id.btnRegSubmit).setOnClickListener {
            val email = findViewById<EditText>(R.id.etRegUsername).text.toString().trim()
            val p = findViewById<EditText>(R.id.etRegPassword).text.toString().trim()

            if (!email.contains("@") || email.length < 5) {
                Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (p.length < 4) {
                Toast.makeText(this, getString(R.string.error_invalid_code), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.register(email = email, p = p).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    if (body?.status == "success") {
                        prefs.edit().putString("pending_verification", email).apply()
                        Toast.makeText(this@RegisterActivity, getString(R.string.process_completed), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = body?.message ?: getString(R.string.connection_error)
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                }
            })
        }

        findViewById<TextView>(R.id.btnBackToLogin).setOnClickListener { finish() }
    }
}