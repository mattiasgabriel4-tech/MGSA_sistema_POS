package com.gabisanchez.carnetperritos

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Prefs {
    private const val NAME = "carnet_prefs"

    fun serverUrl(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("server_url", "") ?: ""

    fun apiKey(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("api_key", "") ?: ""

    fun save(ctx: Context, serverUrl: String, apiKey: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("server_url", serverUrl.trimEnd('/'))
            .putString("api_key", apiKey)
            .apply()
    }
}

// Cliente para el mini-servidor de VERIFICACION unicamente.
// No sube fotos ni PDF: solo un registro chico para que la verificacion publica funcione.
object ApiClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun sincronizarConfigurado(ctx: Context): Boolean =
        Prefs.serverUrl(ctx).isNotBlank() && Prefs.apiKey(ctx).isNotBlank()

    fun enviarVerificacion(ctx: Context, carnet: Carnet, callback: Callback): Call? {
        if (!sincronizarConfigurado(ctx)) return null

        val json = JSONObject().apply {
            put("carnet_number", carnet.carnetNumber)
            put("dog_name", carnet.dogName)
            put("issue_date", carnet.issueDate)
            put("expiry_date", carnet.expiryDate)
            put("status", carnet.status)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("${Prefs.serverUrl(ctx)}/api/registrar")
            .addHeader("X-API-Key", Prefs.apiKey(ctx))
            .post(body)
            .build()

        val call = client.newCall(req)
        call.enqueue(callback)
        return call
    }
}
