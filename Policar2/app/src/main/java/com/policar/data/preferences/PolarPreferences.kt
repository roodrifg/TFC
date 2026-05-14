package com.policar.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "polar_prefs")

class PolarPreferences(private val context: Context) {

    companion object {
        private val SAVED_DEVICE_ID = stringPreferencesKey("saved_device_id")
        private val SAVED_DEVICE_NAME = stringPreferencesKey("saved_device_name")
        private val LAST_HEART_RATE = intPreferencesKey("last_heart_rate")
    }

    val savedDeviceId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SAVED_DEVICE_ID]
    }

    val savedDeviceName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SAVED_DEVICE_NAME]
    }

    val lastHeartRate: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_HEART_RATE] ?: 0
    }

    suspend fun saveDevice(deviceId: String, deviceName: String = "Polar H10") {
        context.dataStore.edit { prefs ->
            prefs[SAVED_DEVICE_ID] = deviceId
            prefs[SAVED_DEVICE_NAME] = deviceName
        }
    }

    suspend fun clearDevice() {
        context.dataStore.edit { prefs ->
            prefs.remove(SAVED_DEVICE_ID)
            prefs.remove(SAVED_DEVICE_NAME)
        }
    }

    suspend fun updateHeartRate(hr: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_HEART_RATE] = hr
        }
    }

    suspend fun getSavedDeviceIdOnce(): String? {
        return context.dataStore.data.first()[SAVED_DEVICE_ID]
    }
}