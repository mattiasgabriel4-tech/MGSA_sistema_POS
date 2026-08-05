package com.gabisanchez.carnetperritos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.content.Context
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object PdfHelper {

    // Tamaño tipo tarjeta de credito en puntos (85.6mm x 54mm), igual que la version del servidor
    private const val CARD_W = 243
    private const val CARD_H = 153

    fun generarPdf(context: Context, carnet: Carnet, nombreEmprendimiento: String): File {
        val doc = PdfDocument()

        // ---------- FRENTE ----------
        val pageInfoFrente = PdfDocument.PageInfo.Builder(CARD_W, CARD_H, 1).create()
        val pageFrente = doc.startPage(pageInfoFrente)
        dibujarFrente(pageFrente.canvas, carnet, nombreEmprendimiento)
        doc.finishPage(pageFrente)

        // ---------- DORSO ----------
        val pageInfoDorso = PdfDocument.PageInfo.Builder(CARD_W, CARD_H, 2).create()
        val pageDorso = doc.startPage(pageInfoDorso)
        dibujarDorso(pageDorso.canvas, carnet)
        doc.finishPage(pageDorso)

        val carpeta = File(context.getExternalFilesDir(null), "carnets")
        if (!carpeta.exists()) carpeta.mkdirs()
        val archivo = File(carpeta, "${carnet.carnetNumber}.pdf")
        FileOutputStream(archivo).use { doc.writeTo(it) }
        doc.close()
        return archivo
    }

    private fun dibujarFrente(canvas: android.graphics.Canvas, carnet: Carnet, nombreEmprendimiento: String) {
        val verde = Color.parseColor("#1f6f43")
        val gris = Color.parseColor("#cccccc")

        val paintHeader = Paint().apply { color = verde; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), 26f, paintHeader)

        val paintHeaderText = Paint().apply {
            color = Color.WHITE; textSize = 8f; isFakeBoldText = true; isAntiAlias = true
        }
        canvas.drawText(nombreEmprendimiento.uppercase(), 8f, 16f, paintHeaderText)

        val photoX = 8f; val photoY = 32f; val photoW = 62f; val photoH = 70f
        val paintBorde = Paint().apply { color = gris; style = Paint.Style.STROKE; strokeWidth = 1f }

        if (!carnet.photoPath.isNullOrBlank() && File(carnet.photoPath).exists()) {
            val bmp = BitmapFactory.decodeFile(carnet.photoPath)
            if (bmp != null) {
                val escalado = Bitmap.createScaledBitmap(bmp, photoW.toInt(), photoH.toInt(), true)
                canvas.drawBitmap(escalado, photoX, photoY, null)
            }
        }
        canvas.drawRect(photoX, photoY, photoX + photoW, photoY + photoH, paintBorde)

        val infoX = photoX + photoW + 8
        var y = 40f
        val paintTitulo = Paint().apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText(carnet.dogName, infoX, y, paintTitulo)
        y += 13

        val paintTexto = Paint().apply { color = Color.BLACK; textSize = 6.5f; isAntiAlias = true }
        canvas.drawText("Raza: ${carnet.breed.ifBlank { "-" }}", infoX, y, paintTexto); y += 8
        canvas.drawText("Sexo: ${carnet.sex.ifBlank { "-" }}  Color: ${carnet.color.ifBlank { "-" }}", infoX, y, paintTexto); y += 8
        canvas.drawText("Nac.: ${carnet.birthDate.ifBlank { "-" }}", infoX, y, paintTexto); y += 10

        val paintNegrita = Paint().apply { color = Color.BLACK; textSize = 6.5f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("Dueño/a:", infoX, y, paintNegrita); y += 8
        canvas.drawText(carnet.ownerName, infoX, y, paintTexto); y += 8
        if (carnet.ownerPhone.isNotBlank()) {
            canvas.drawText("Tel: ${carnet.ownerPhone}", infoX, y, paintTexto)
        }

        val footY = CARD_H - 40f
        val paintLinea = Paint().apply { color = gris; strokeWidth = 1f }
        canvas.drawLine(8f, footY, CARD_W - 8f, footY, paintLinea)

        val paintNumero = Paint().apply { color = Color.BLACK; textSize = 7f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText(carnet.carnetNumber, 8f, footY + 12, paintNumero)
        canvas.drawText("Emisión: ${carnet.issueDate}", 8f, footY + 22, paintTexto)
        canvas.drawText("Vencimiento: ${carnet.expiryDate}", 8f, footY + 31, paintTexto)

        // QR: por ahora codifica solo el numero de carnet (se verifica dentro de la misma app,
        // o contra el mini-servidor de verificacion cuando este disponible)
        val qrBitmap = generarQr(carnet.carnetNumber, 130)
        canvas.drawBitmap(qrBitmap, null, android.graphics.RectF(CARD_W - 40f, footY + 2, CARD_W - 6f, footY + 36), null)
    }

    private fun dibujarDorso(canvas: android.graphics.Canvas, carnet: Carnet) {
        val verde = Color.parseColor("#1f6f43")
        val paintHeader = Paint().apply { color = verde; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), 18f, paintHeader)

        val paintHeaderText = Paint().apply { color = Color.WHITE; textSize = 8f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("CARTILLA DE VACUNACIÓN", 8f, 12f, paintHeaderText)

        val paintNegrita = Paint().apply { color = Color.BLACK; textSize = 6.5f; isFakeBoldText = true; isAntiAlias = true }
        var y = 30f
        canvas.drawText("Vacuna", 8f, y, paintNegrita)
        canvas.drawText("Aplicación", 110f, y, paintNegrita)
        canvas.drawText("Próx. dosis", 175f, y, paintNegrita)
        y += 6

        val paintLinea = Paint().apply { color = Color.parseColor("#999999"); strokeWidth = 1f }
        canvas.drawLine(8f, y, CARD_W - 8f, y, paintLinea)
        y += 10

        val paintTexto = Paint().apply { color = Color.BLACK; textSize = 6.5f; isAntiAlias = true }
        if (carnet.vacunas.isEmpty()) {
            canvas.drawText("Sin vacunas registradas", 8f, y, paintTexto)
        } else {
            for (v in carnet.vacunas) {
                if (y > CARD_H - 14) break
                canvas.drawText(v.nombre, 8f, y, paintTexto)
                canvas.drawText(v.fechaAplicacion.ifBlank { "-" }, 110f, y, paintTexto)
                canvas.drawText(v.proximaDosis.ifBlank { "-" }, 175f, y, paintTexto)
                y += 12
            }
        }
    }

    private fun generarQr(texto: String, tamano: Int): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(texto, BarcodeFormat.QR_CODE, tamano, tamano)
        val bmp = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.RGB_565)
        for (x in 0 until tamano) {
            for (yPix in 0 until tamano) {
                bmp.setPixel(x, yPix, if (matrix[x, yPix]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
