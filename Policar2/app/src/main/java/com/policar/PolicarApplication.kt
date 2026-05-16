package com.policar

import android.app.Application
import android.util.Log
import com.policar.data.remote.SupabaseClientProvider

// ═══════════════════════════════════════════════════════════════════════
//  POLICAR APPLICATION
// ═══════════════════════════════════════════════════════════════════════

class PolicarApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        installBleExceptionHandler()

        Log.d("PolicarApp", "Aplicación iniciada — Supabase URL: ${
            try { SupabaseClientProvider.client.supabaseUrl } catch (e: Exception) { "no disponible" }
        }")
    }

    private fun installBleExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val blePackage = "com.polar.androidcommunications.api.ble"
            val isBleException = throwable.javaClass.name.startsWith(blePackage) ||
                throwable.cause?.javaClass?.name?.startsWith(blePackage) == true
            if (isBleException) {
                Log.w("PolicarApp", "BLE exception ignorada (Polar SDK): ${throwable.javaClass.simpleName} — ${throwable.message}")
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}