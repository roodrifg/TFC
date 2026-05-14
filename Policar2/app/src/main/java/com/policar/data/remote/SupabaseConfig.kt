package com.policar.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {

    private const val SUPABASE_URL = "https://qrfphdliiiavpoicnmaw.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFyZnBoZGxpaWlhdnBvaWNubWF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcxNDYzNzcsImV4cCI6MjA5MjcyMjM3N30.eYo0DwaRlOHE7qkBSKgAEtBnS5nS0ehEpNqDzAw8AfQ"

    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }
    }
}