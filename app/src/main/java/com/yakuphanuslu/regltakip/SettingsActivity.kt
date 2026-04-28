package com.yakuphanuslu.regltakip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val auth = FirebaseAuth.getInstance()

        val btnLogin = findViewById<MaterialButton>(R.id.btnSettingsLogin)
        val cardAccountInfo = findViewById<MaterialCardView>(R.id.cardAccountInfo)
        val tvStatus = findViewById<TextView>(R.id.tvUserStatus)
        val btnLogout = findViewById<MaterialButton>(R.id.btnSettingsLogout)

        if (auth.currentUser == null) {
            btnLogin.visibility = View.VISIBLE
            cardAccountInfo.visibility = View.GONE

            btnLogin.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } else {
            btnLogin.visibility = View.GONE
            cardAccountInfo.visibility = View.VISIBLE

            tvStatus.text = auth.currentUser?.email

            btnLogout.setOnClickListener {
                auth.signOut()
                // Çıkış yapınca sayfayı yenile ki giriş butonu geri gelsin
                recreate()
            }
        }
    }
}