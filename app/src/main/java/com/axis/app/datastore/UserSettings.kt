package com.axis.app.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettings(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LAST_PROCESSED_TIMESTAMP = longPreferencesKey("last_processed_timestamp")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_SECURITY_ENABLED = booleanPreferencesKey("is_security_enabled")
        val ENC_PIN = stringPreferencesKey("enc_pin")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val CURRENCY = stringPreferencesKey("currency")
    }

    val userName: Flow<String> = dataStore.data.map {
        it[USER_NAME] ?: ""
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME] = name }
    }

    val userEmail: Flow<String> = dataStore.data.map { it[USER_EMAIL] ?: "" }
    suspend fun setUserEmail(email: String) {
        dataStore.edit { it[USER_EMAIL] = email }
    }

    val isSecurityEnabled: Flow<Boolean> = dataStore.data.map { it[IS_SECURITY_ENABLED] ?: false }
    suspend fun setSecurityEnabled(enabled: Boolean) {
        dataStore.edit { it[IS_SECURITY_ENABLED] = enabled }
    }

    val encPin: Flow<String> = dataStore.data.map { it[ENC_PIN] ?: "" }
    suspend fun setEncPin(pin: String) {
        dataStore.edit { it[ENC_PIN] = pin }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { it[IS_BIOMETRIC_ENABLED] ?: false }
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    val currency: Flow<String> = dataStore.data.map { it[CURRENCY] ?: "Ksh" }
    suspend fun setCurrency(currency: String) {
        dataStore.edit { it[CURRENCY] = currency }
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map {
        it[IS_DARK_MODE] ?: false // Default to light mode
    }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit {
            it[IS_DARK_MODE] = isDark
        }
    }

    val lastProcessedTimestamp: Flow<Long> = dataStore.data.map {
        it[LAST_PROCESSED_TIMESTAMP] ?: 0L
    }

    suspend fun setLastProcessedTimestamp(timestamp: Long) {
        dataStore.edit {
            it[LAST_PROCESSED_TIMESTAMP] = timestamp
        }
    }
}
