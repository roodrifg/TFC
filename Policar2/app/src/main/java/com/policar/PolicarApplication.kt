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
        // Inicializar el cliente Supabase de forma lazy (primera llamada)
        Log.d("PolicarApp", "Aplicación iniciada — Supabase URL: ${
            try { SupabaseClientProvider.client.supabaseUrl } catch (e: Exception) { "no disponible" }
        }")
    }
}