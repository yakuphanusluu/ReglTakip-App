package com.yakuphanuslu.regltakip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog // YENİ EKLENDİ
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class AuthActivity : AppCompatActivity() {
    private lateinit var api: ApiService
    private lateinit var tvNotice: TextView
    private lateinit var etEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("regl_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_lang", "tr") ?: "tr"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (prefs.getInt("user_id", -1) != -1) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/regl_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiService::class.java)

        etEmail = findViewById(R.id.etUsername)
        tvNotice = findViewById(R.id.tvVerificationNotice)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)
        val btnForgot = findViewById<TextView>(R.id.btnForgotPassword)
        val btnLanguageLogin = findViewById<TextView>(R.id.btnLanguageLogin) // YENİ: DİL BUTONU

        // Seçili dili butona yazdır (Örn: 🌍 EN)
        btnLanguageLogin.text = "🌍 ${langCode.uppercase()}"

        // --- YENİ: DİL SEÇİM PENCERESİ ---
        btnLanguageLogin.setOnClickListener {
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

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val p = etPass.text.toString().trim()

            if (email.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.login(email = email, p = p).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    if (body?.status == "success" && body.user_id != null) {
                        prefs.edit()
                            .putInt("user_id", body.user_id)
                            .putString("email", email)
                            .remove("pending_verification")
                            .apply()

                        Toast.makeText(this@AuthActivity, getString(R.string.process_completed), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    } else {
                        val errorMsg = body?.message ?: getString(R.string.error_auth_failed)
                        Toast.makeText(this@AuthActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@AuthActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
        val pendingEmail = prefs.getString("pending_verification", "")

        if (!pendingEmail.isNullOrEmpty()) {
            tvNotice.visibility = View.VISIBLE
            tvNotice.text = getString(R.string.verification_notice_format, pendingEmail)
            etEmail.setText(pendingEmail)
        } else {
            tvNotice.visibility = View.GONE
        }
    }
}