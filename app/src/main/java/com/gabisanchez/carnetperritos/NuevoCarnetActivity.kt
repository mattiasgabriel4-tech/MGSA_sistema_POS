package com.gabisanchez.carnetperritos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NuevoCarnetActivity : AppCompatActivity() {

    private lateinit var ivFoto: ImageView
    private lateinit var vacunasContainer: LinearLayout
    private var fotoPathFinal: String? = null
    private lateinit var db: DbHelper

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            ivFoto.setImageURI(uri)
            fotoPathFinal = copiarFotoDefinitiva(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_carnet)
        db = DbHelper(this)

        ivFoto = findViewById(R.id.ivFoto)
        vacunasContainer = findViewById(R.id.vacunasContainer)

        findViewById<Button>(R.id.btnElegirFoto).setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<Button>(R.id.btnAgregarVacuna).setOnClickListener {
            agregarFilaVacuna()
        }

        findViewById<Button>(R.id.btnGuardarCarnet).setOnClickListener {
            guardarCarnet()
        }
    }

    private fun agregarFilaVacuna() {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_vacuna, vacunasContainer, false)
        fila.findViewById<Button>(R.id.btnQuitar).setOnClickListener {
            vacunasContainer.removeView(fila)
        }
        vacunasContainer.addView(fila)
    }

    private fun copiarFotoDefinitiva(uri: Uri): String? {
        return try {
            var nombre = "foto_${System.currentTimeMillis()}.jpg"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) nombre = "${System.currentTimeMillis()}_${cursor.getString(idx)}"
            }
            val carpeta = File(getExternalFilesDir(null), "fotos")
            if (!carpeta.exists()) carpeta.mkdirs()
            val archivo = File(carpeta, nombre)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(archivo).use { output -> input.copyTo(output) }
            }
            archivo.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun textoDe(id: Int): String = findViewById<EditText>(id).text.toString().trim()

    private fun guardarCarnet() {
        val dogName = textoDe(R.id.etDogName)
        val ownerName = textoDe(R.id.etOwnerName)

        if (dogName.isBlank() || ownerName.isBlank()) {
            Toast.makeText(this, "Faltan el nombre del perro o del dueño", Toast.LENGTH_LONG).show()
            return
        }

        val vacunas = mutableListOf<Vacuna>()
        for (i in 0 until vacunasContainer.childCount) {
            val fila = vacunasContainer.getChildAt(i)
            val nombre = fila.findViewById<EditText>(R.id.etVacNombre).text.toString().trim()
            if (nombre.isNotBlank()) {
                vacunas.add(Vacuna(
                    nombre,
                    fila.findViewById<EditText>(R.id.etVacFecha).text.toString().trim(),
                    fila.findViewById<EditText>(R.id.etVacProxima).text.toString().trim()
                ))
            }
        }

        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 365)
        val vencimiento = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val carnet = db.crearCarnet(
            dogName = dogName,
            breed = textoDe(R.id.etBreed),
            sex = textoDe(R.id.etSex),
            birthDate = textoDe(R.id.etBirthDate),
            color = textoDe(R.id.etColor),
            photoPath = fotoPathFinal,
            ownerName = ownerName,
            ownerPhone = textoDe(R.id.etOwnerPhone),
            ownerAddress = textoDe(R.id.etOwnerAddress),
            ownerEmail = textoDe(R.id.etOwnerEmail),
            issueDate = hoy,
            expiryDate = vencimiento,
            vacunas = vacunas
        )

        val archivoPdf = PdfHelper.generarPdf(this, carnet, "Mi Emprendimiento de Carnets")

        // Intento de sincronizacion liviana (no bloquea ni molesta si falla o no esta configurada)
        ApiClient.enviarVerificacion(this, carnet, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) { /* se reintenta despues */ }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) db.marcarSincronizado(carnet.id)
                response.close()
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Carnet creado")
            .setMessage("Se generó el carnet ${carnet.carnetNumber} para ${carnet.dogName}.")
            .setPositiveButton("Ver PDF") { _, _ -> abrirPdf(archivoPdf) }
            .setNegativeButton("Cerrar") { _, _ -> finish() }
            .setOnDismissListener { finish() }
            .show()
    }

    private fun abrirPdf(archivo: File) {
        val uri = FileProvider.getUriForFile(this, "com.gabisanchez.carnetperritos.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Instalá una app para ver PDF (ej. Google Drive o un lector de PDF)", Toast.LENGTH_LONG).show()
        }
    }
}
