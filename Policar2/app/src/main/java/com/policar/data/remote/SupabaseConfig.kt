package com.policar.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

// ═══════════════════════════════════════════════════════════════════════
//  POLICAR — SUPABASE CONFIG
//  defaultSerializer con ignoreUnknownKeys = true es CRITICO para que
//  decodeList<T> no falle cuando la tabla tiene columnas extra (ej: "id"
//  auto-generado que Entrenamiento no incluye en el insert).
// ═══════════════════════════════════════════════════════════════════════

object SupabaseConfig {

    private const val SUPABASE_URL  = "https://qrfphdliiiavpoicnmaw.supabase.co"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFyZnBoZGxpaWlhdnBvaWNubWF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcxNDYzNzcsImV4cCI6MjA5MjcyMjM3N30." +
                "eYo0DwaRlOHE7qkBSKgAEtBnS5nS0ehEpNqDzAw8AfQ"

    /**
     * Json compartido para serialización/deserialización manual.
     * Se usa en WorkoutViewModel para convertir Entrenamiento a JsonElement
     * antes del insert, evitando la inferencia de tipo Any en supabase-kt 2.0.0.
     */
    val supabaseJson = Json {
        ignoreUnknownKeys  = true   // Ignora columnas extra devueltas por Supabase (id, created_at…)
        encodeDefaults     = true   // Incluye campos con valor por defecto en el JSON de insert
        coerceInputValues  = true   // Coerciona null → default en tipos no-nullables
    }

    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(supabaseJson)
            install(Postgrest)
        }
    }
}