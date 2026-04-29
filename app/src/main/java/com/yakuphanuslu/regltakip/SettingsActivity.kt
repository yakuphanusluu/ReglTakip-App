package com.yakuphanuslu.regltakip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("regl_prefs", Context.MODE_PRIVATE)
        val userEmail = prefs.getString("email", "E-posta bulunamadı")

        // Görünümleri tanımlıyoruz
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val btnUpdateInfo = findViewById<Button>(R.id.btnUpdateInfo)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        // RadioGroup ve RadioButton Tanımlamaları
        val rgLanguage = findViewById<RadioGroup>(R.id.rgLanguage)
        val rbTr = findViewById<RadioButton>(R.id.rbTr)
        val rbEn = findViewById<RadioButton>(R.id.rbEn)
        val rbDe = findViewById<RadioButton>(R.id.rbDe)
        val rbRu = findViewById<RadioButton>(R.id.rbRu)
        val rbPl = findViewById<RadioButton>(R.id.rbPl)

        tvUserEmail?.text = userEmail

        btnBack?.setOnClickListener {
            finish()
        }

        btnUpdateInfo?.setOnClickListener {
            val intent = Intent(this, UpdateInfoActivity::class.java)
            startActivity(intent)
        }

        btnLogout?.setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // --- DİL SEÇİM MANTIĞI ---

        // 1. Uygulama şu an hangi dildeyse, listede o dili işaretle
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = currentLocales.toLanguageTags()

        when {
            currentTag.contains("en") -> rbEn.isChecked = true
            currentTag.contains("de") -> rbDe.isChecked = true
            currentTag.contains("ru") -> rbRu.isChecked = true
            currentTag.contains("pl") -> rbPl.isChecked = true
            else -> rbTr.isChecked = true
        }

        // 2. Kullanıcı listeden bir dile tıkladığında dili değiştir
        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val langCode = when (checkedId) {
                R.id.rbEn -> "en"
                R.id.rbDe -> "de"
                R.id.rbRu -> "ru"
                R.id.rbPl -> "pl"
                else -> "tr"
            }

            // --- KRİTİK GÜNCELLEME: Seçilen dili hafızaya alıyoruz ---
            // MainActivity'deki takvim bu "app_lang" anahtarına bakarak dilleri çizer.
            prefs.edit().putString("app_lang", langCode).apply()

            // Uygulama genelinde UI dilini değiştir
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}