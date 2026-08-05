package com.gabisanchez.carnetperritos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etServerUrl = findViewById<EditText>(R.id.etServerUrl)
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        etServerUrl.setText(Prefs.serverUrl(this))
        etApiKey.setText(Prefs.apiKey(this))

        // Config de sincronizacion con el mini-servidor de verificacion (opcional por ahora)
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            Prefs.save(this, etServerUrl.text.toString(), etApiKey.text.toString())
            Toast.makeText(this, "Configuración de sincronización guardada", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnNuevoCarnet).setOnClickListener {
            startActivity(Intent(this, NuevoCarnetActivity::class.java))
        }

        findViewById<Button>(R.id.btnVerCarnets).setOnClickListener {
            startActivity(Intent(this, ListaCarnetsActivity::class.java))
        }
    }
}
