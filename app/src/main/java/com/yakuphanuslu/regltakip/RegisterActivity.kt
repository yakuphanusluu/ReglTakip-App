package com.yakuphanuslu.regltakip

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/regl_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)

        findViewById<Button>(R.id.btnRegSubmit).setOnClickListener {
            // Değişken ismini 'email' yaptık
            val email = findViewById<EditText>(R.id.etRegUsername).text.toString().trim()
            val p = findViewById<EditText>(R.id.etRegPassword).text.toString().trim()

            // Basit e-posta ve şifre kontrolü
            if (!email.contains("@") || email.length < 5) {
                Toast.makeText(this, "Geçerli bir e-posta adresi gir kanka! 📧", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (p.length < 4) {
                Toast.makeText(this, "Şifre en az 4 karakter olmalı! 🔑", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ApiService içindeki yeni 'email' parametresini kullanıyoruz
            api.register(email = email, p = p).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    if (body?.status == "success") {

                        // YENİ EKLENEN KISIM: Onay bekleyen e-postayı hafızaya al (Giriş ekranında uyarı göstermek için)
                        val prefs = getSharedPreferences("regl_prefs", MODE_PRIVATE)
                        prefs.edit().putString("pending_verification", email).apply()

                        Toast.makeText(this@RegisterActivity, "Kayıt Başarılı! E-postanı kontrol et. ✨", Toast.LENGTH_LONG).show()
                        finish() // Giriş ekranına geri döner
                    } else {
                        // PHP tarafındaki hata mesajını göster (örn: "Bu e-posta zaten kayıtlı")
                        val errorMsg = body?.message ?: "Bu e-posta zaten alınmış veya bir hata oluştu!"
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Bağlantı hatası: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        findViewById<TextView>(R.id.btnBackToLogin).setOnClickListener { finish() }
    }
}