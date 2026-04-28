package com.yakuphanuslu.regltakip

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val auth = FirebaseAuth.getInstance()
        val etEmail = findViewById<TextInputEditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegisterPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegisterSubmit)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.length >= 6) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Hoş geldin Yakup! Hesap açıldı. ✨", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Lütfen geçerli bilgiler gir kanka!", Toast.LENGTH_SHORT).show()
            }
        }

        tvBackToLogin.setOnClickListener { finish() }
    }
}