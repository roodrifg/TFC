package com.policar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.policar.data.model.ModoGrabacion
import com.policar.data.model.TipoDeporte
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ═══════════════════════════════════════════════════════════════════════════════
//  POLICAR — DATASTORE PREFERENCES
//  Persiste el deviceId del H10 para reconexión automática entre sesiones.
// ═══════════════════════════════════════════════════════════════════════════════

// Extension property sobre Context para acceder al DataStore de forma singleton
val Context.policarDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "policar_preferences"
)

// Claves de preferencias
object PolicarPreferencesKeys {
    val DEVICE_ID     = stringPreferencesKey("polar_device_id")
    val LAST_SPORT    = stringPreferencesKey("last_sport")
    val LAST_MODE     = stringPreferencesKey("last_recording_mode")
}

// ─── Repository de preferencias ──────────────────────────────────────────────

class PolicarPreferencesRepository(private val context: Context) {

    /** Flow del deviceId guardado. Emite "" si no hay ninguno almacenado. */
    val deviceIdFlow: Flow<String> = context.policarDataStore.data.map { preferences ->
        preferences[PolicarPreferencesKeys.DEVICE_ID] ?: ""
    }

    /** Flow del último deporte seleccionado */
    val lastSportFlow: Flow<String> = context.policarDataStore.data.map { preferences ->
        preferences[PolicarPreferencesKeys.LAST_SPORT] ?: TipoDeporte.FUTBOL.name
    }

    /** Flow del último modo de grabación */
    val lastModeFlow: Flow<String> = context.policarDataStore.data.map { preferences ->
        preferences[PolicarPreferencesKeys.LAST_MODE] ?: ModoGrabacion.EN_VIVO.name
    }

    /** Guarda el deviceId del sensor */
    suspend fun saveDeviceId(deviceId: String) {
        context.policarDataStore.edit { preferences ->
            preferences[PolicarPreferencesKeys.DEVICE_ID] = deviceId.trim().uppercase()
        }
    }

    /** Limpia el deviceId (por ejemplo al desconectar intencionalmente) */
    suspend fun clearDeviceId() {
        context.policarDataStore.edit { preferences ->
            preferences.remove(PolicarPreferencesKeys.DEVICE_ID)
        }
    }

    /** Guarda el deporte seleccionado */
    suspend fun saveLastSport(sport: TipoDeporte) {
        context.policarDataStore.edit { preferences ->
            preferences[PolicarPreferencesKeys.LAST_SPORT] = sport.name
        }
    }

    /** Guarda el modo de grabación */
    suspend fun saveLastMode(modo: ModoGrabacion) {
        context.policarDataStore.edit { preferences ->
            preferences[PolicarPreferencesKeys.LAST_MODE] = modo.name
        }
    }
}