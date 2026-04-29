package com.yakuphanuslu.regltakip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class UpdateInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_info)

        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
        val uid = prefs.getInt("user_id", -1)
        val currentEmail = prefs.getString("email", "") ?: ""

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/regl_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        // Geri Butonu
        findViewById<TextView>(R.id.btnBackUpdate).setOnClickListener { finish() }

        // Görünümleri (Views) tanımlıyoruz
        val etNewEmail = findViewById<EditText>(R.id.etNewEmail)
        val btnRequestCode = findViewById<Button>(R.id.btnRequestCode)
        val tilSecurityCode = findViewById<View>(R.id.tilSecurityCode)
        val etSecurityCode = findViewById<EditText>(R.id.etSecurityCode)
        val btnVerifyAndUpdate = findViewById<Button>(R.id.btnVerifyAndUpdate)

        // --- 1. AŞAMA: MAİL DEĞİŞİMİ İÇİN KOD İSTEME ---
        btnRequestCode.setOnClickListener {
            val newEmail = etNewEmail.text.toString().trim()

            if (!newEmail.contains("@") || newEmail.length < 5) {
                // Dile duyarlı hata mesajı
                Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.requestEmailChange(uid = uid, newEmail = newEmail).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    if (body?.status == "success") {
                        // Sunucudan gelen başarı mesajı (Zaten API tarafında yönetiliyor olabilir)
                        Toast.makeText(this@UpdateInfoActivity, body.message, Toast.LENGTH_LONG).show()

                        tilSecurityCode.visibility = View.VISIBLE
                        btnVerifyAndUpdate.visibility = View.VISIBLE
                        btnRequestCode.visibility = View.GONE
                        etNewEmail.isEnabled = false
                    } else {
                        Toast.makeText(this@UpdateInfoActivity, body?.message ?: getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@UpdateInfoActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                }
            })
        }

        // --- 2. AŞAMA: KODU DOĞRULAMA VE GÜNCELLEME ---
        btnVerifyAndUpdate.setOnClickListener {
            val newEmail = etNewEmail.text.toString().trim()
            val code = etSecurityCode.text.toString().trim()

            if (code.length != 6) {
                // Dile duyarlı güvenlik kodu uyarısı
                Toast.makeText(this, getString(R.string.error_invalid_code), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            api.updateEmail(uid = uid, newEmail = newEmail, code = code).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@UpdateInfoActivity, body.message, Toast.LENGTH_LONG).show()

                        // Hafızayı temizle ve AuthActivity'ye yönlendir
                        prefs.edit().clear().putString("pending_verification", newEmail).apply()

                        val intent = Intent(this@UpdateInfoActivity, AuthActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@UpdateInfoActivity, body?.message ?: getString(R.string.wrong_code), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@UpdateInfoActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                }
            })
        }

        // --- 3. ŞİFRE SIFIRLAMA MANTIĞI ---
        findViewById<Button>(R.id.btnSendPasswordReset).setOnClickListener {
            api.forgotPassword(email = currentEmail).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val msg = response.body()?.message ?: getString(R.string.process_completed)
                    Toast.makeText(this@UpdateInfoActivity, msg, Toast.LENGTH_LONG).show()
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@UpdateInfoActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}