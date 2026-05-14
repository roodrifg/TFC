package com.policar.data.remote

import com.policar.data.model.Entrenamiento

private const val TABLE_ENTRENAMIENTOS = "entrenamientos"

object SupabaseClientProvider {
    val client = SupabaseConfig.supabase
}

@Suppress("UNUSED")
suspend fun insertEntrenamiento(entrenamiento: Entrenamiento): Result<String?> {
    return Result.success(null)
}

@Suppress("UNUSED")
suspend fun fetchEntrenamientos(deviceId: String): Result<List<Entrenamiento>> {
    return Result.success(emptyList())
}

@Suppress("UNUSED")
suspend fun updateRPE(trainingId: String, rpe: Int): Result<Unit> {
    return Result.success(Unit)
}