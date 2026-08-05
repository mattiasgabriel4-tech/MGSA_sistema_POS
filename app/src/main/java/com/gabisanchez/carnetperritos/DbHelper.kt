package com.gabisanchez.carnetperritos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Vacuna(
    val nombre: String,
    val fechaAplicacion: String,
    val proximaDosis: String
)

data class Carnet(
    val id: Long,
    val carnetNumber: String,
    val dogName: String,
    val breed: String,
    val sex: String,
    val birthDate: String,
    val color: String,
    val photoPath: String?,
    val ownerName: String,
    val ownerPhone: String,
    val ownerAddress: String,
    val ownerEmail: String,
    val issueDate: String,
    val expiryDate: String,
    val status: String,
    val vacunas: List<Vacuna> = emptyList()
)

class DbHelper(context: Context) : SQLiteOpenHelper(context, "carnets.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE carnets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                carnet_number TEXT UNIQUE NOT NULL,
                dog_name TEXT NOT NULL,
                breed TEXT, sex TEXT, birth_date TEXT, color TEXT,
                photo_path TEXT,
                owner_name TEXT NOT NULL, owner_phone TEXT, owner_address TEXT, owner_email TEXT,
                issue_date TEXT NOT NULL, expiry_date TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'activo',
                sincronizado INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE vacunas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                carnet_id INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                fecha_aplicacion TEXT,
                proxima_dosis TEXT
            )
        """)
        db.execSQL("CREATE TABLE contador (nombre TEXT PRIMARY KEY, valor INTEGER NOT NULL)")
        db.execSQL("INSERT INTO contador (nombre, valor) VALUES ('carnet', 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // futuras migraciones acá
    }

    fun siguienteNumeroCarnet(prefijo: String = "CP"): String {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT valor FROM contador WHERE nombre = 'carnet'", null)
            var valor = 0
            if (cursor.moveToFirst()) valor = cursor.getInt(0)
            cursor.close()
            valor += 1
            db.execSQL("UPDATE contador SET valor = ? WHERE nombre = 'carnet'", arrayOf(valor))
            db.setTransactionSuccessful()
            return "$prefijo-${valor.toString().padStart(6, '0')}"
        } finally {
            db.endTransaction()
        }
    }

    fun crearCarnet(
        dogName: String, breed: String, sex: String, birthDate: String, color: String,
        photoPath: String?, ownerName: String, ownerPhone: String, ownerAddress: String, ownerEmail: String,
        issueDate: String, expiryDate: String, vacunas: List<Vacuna>, prefijo: String = "CP"
    ): Carnet {
        val db = writableDatabase
        val carnetNumber = siguienteNumeroCarnet(prefijo)

        val values = ContentValues().apply {
            put("carnet_number", carnetNumber)
            put("dog_name", dogName)
            put("breed", breed)
            put("sex", sex)
            put("birth_date", birthDate)
            put("color", color)
            put("photo_path", photoPath)
            put("owner_name", ownerName)
            put("owner_phone", ownerPhone)
            put("owner_address", ownerAddress)
            put("owner_email", ownerEmail)
            put("issue_date", issueDate)
            put("expiry_date", expiryDate)
            put("status", "activo")
        }
        val id = db.insert("carnets", null, values)

        for (v in vacunas) {
            val vv = ContentValues().apply {
                put("carnet_id", id)
                put("nombre", v.nombre)
                put("fecha_aplicacion", v.fechaAplicacion)
                put("proxima_dosis", v.proximaDosis)
            }
            db.insert("vacunas", null, vv)
        }

        return obtenerCarnet(id)!!
    }

    fun marcarSincronizado(id: Long) {
        writableDatabase.execSQL("UPDATE carnets SET sincronizado = 1 WHERE id = ?", arrayOf(id))
    }

    fun cambiarEstado(id: Long, status: String) {
        writableDatabase.execSQL(
            "UPDATE carnets SET status = ?, sincronizado = 0 WHERE id = ?",
            arrayOf(status, id)
        )
    }

    fun obtenerCarnet(id: Long): Carnet? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM carnets WHERE id = ?", arrayOf(id.toString()))
        if (!cursor.moveToFirst()) { cursor.close(); return null }
        val carnet = carnetDesdeCursor(cursor)
        cursor.close()

        val vCursor = db.rawQuery("SELECT nombre, fecha_aplicacion, proxima_dosis FROM vacunas WHERE carnet_id = ?", arrayOf(id.toString()))
        val vacunas = mutableListOf<Vacuna>()
        while (vCursor.moveToNext()) {
            vacunas.add(Vacuna(vCursor.getString(0), vCursor.getString(1) ?: "", vCursor.getString(2) ?: ""))
        }
        vCursor.close()

        return carnet.copy(vacunas = vacunas)
    }

    fun obtenerCarnetPorNumero(numero: String): Carnet? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM carnets WHERE carnet_number = ?", arrayOf(numero))
        if (!cursor.moveToFirst()) { cursor.close(); return null }
        val carnet = carnetDesdeCursor(cursor)
        cursor.close()
        return obtenerCarnet(carnet.id)
    }

    fun listarCarnets(busqueda: String = ""): List<Carnet> {
        val db = readableDatabase
        val cursor = if (busqueda.isBlank()) {
            db.rawQuery("SELECT * FROM carnets ORDER BY id DESC", null)
        } else {
            val q = "%$busqueda%"
            db.rawQuery(
                "SELECT * FROM carnets WHERE dog_name LIKE ? OR owner_name LIKE ? OR carnet_number LIKE ? ORDER BY id DESC",
                arrayOf(q, q, q)
            )
        }
        val lista = mutableListOf<Carnet>()
        while (cursor.moveToNext()) lista.add(carnetDesdeCursor(cursor))
        cursor.close()
        return lista
    }

    fun carnetsSinSincronizar(): List<Carnet> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM carnets WHERE sincronizado = 0", null)
        val lista = mutableListOf<Carnet>()
        while (cursor.moveToNext()) lista.add(carnetDesdeCursor(cursor))
        cursor.close()
        return lista
    }

    private fun carnetDesdeCursor(c: android.database.Cursor): Carnet {
        return Carnet(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            carnetNumber = c.getString(c.getColumnIndexOrThrow("carnet_number")),
            dogName = c.getString(c.getColumnIndexOrThrow("dog_name")),
            breed = c.getString(c.getColumnIndexOrThrow("breed")) ?: "",
            sex = c.getString(c.getColumnIndexOrThrow("sex")) ?: "",
            birthDate = c.getString(c.getColumnIndexOrThrow("birth_date")) ?: "",
            color = c.getString(c.getColumnIndexOrThrow("color")) ?: "",
            photoPath = c.getString(c.getColumnIndexOrThrow("photo_path")),
            ownerName = c.getString(c.getColumnIndexOrThrow("owner_name")),
            ownerPhone = c.getString(c.getColumnIndexOrThrow("owner_phone")) ?: "",
            ownerAddress = c.getString(c.getColumnIndexOrThrow("owner_address")) ?: "",
            ownerEmail = c.getString(c.getColumnIndexOrThrow("owner_email")) ?: "",
            issueDate = c.getString(c.getColumnIndexOrThrow("issue_date")),
            expiryDate = c.getString(c.getColumnIndexOrThrow("expiry_date")),
            status = c.getString(c.getColumnIndexOrThrow("status"))
        )
    }
}
