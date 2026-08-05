package com.gabisanchez.carnetperritos

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

class ListaCarnetsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var db: DbHelper
    private var mostrados: List<Carnet> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_carnets)
        db = DbHelper(this)

        listView = findViewById(R.id.listView)
        findViewById<android.widget.ProgressBar>(R.id.progressBar).visibility = android.view.View.GONE

        findViewById<EditText>(R.id.etBuscar).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = cargar(s.toString())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            mostrarOpciones(mostrados[position])
        }

        cargar("")
    }

    override fun onResume() {
        super.onResume()
        cargar(findViewById<EditText>(R.id.etBuscar).text.toString())
    }

    private fun cargar(busqueda: String) {
        mostrados = db.listarCarnets(busqueda)
        val items = mostrados.map { "${it.carnetNumber} - ${it.dogName} (${it.status})" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun mostrarOpciones(carnet: Carnet) {
        val opciones = arrayOf(
            "Ver PDF",
            if (carnet.status == "revocado") "Reactivar" else "Revocar"
        )
        AlertDialog.Builder(this)
            .setTitle("${carnet.carnetNumber} - ${carnet.dogName}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> verPdf(carnet)
                    1 -> cambiarEstado(carnet)
                }
            }
            .show()
    }

    private fun verPdf(carnet: Carnet) {
        val archivo = PdfHelper.generarPdf(this, carnet, "Mi Emprendimiento de Carnets")
        val uri = FileProvider.getUriForFile(this, "com.gabisanchez.carnetperritos.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun cambiarEstado(carnet: Carnet) {
        val nuevo = if (carnet.status == "revocado") "activo" else "revocado"
        db.cambiarEstado(carnet.id, nuevo)
        cargar(findViewById<EditText>(R.id.etBuscar).text.toString())
    }
}
