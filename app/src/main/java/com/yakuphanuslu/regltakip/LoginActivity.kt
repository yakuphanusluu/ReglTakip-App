package com.yakuphanuslu.regltakip

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Eğer kullanıcı zaten giriş yapmışsa direkt ana sayfaya gönder
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etLoginEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLoginSubmit)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        // 2. Giriş Yap Butonu Mantığı
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Giriş başarılı! Hoş geldin Yakup 🌸", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Giriş başarısız: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldur kanka!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Kayıt Ol Ekranına Yönlendirme
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}