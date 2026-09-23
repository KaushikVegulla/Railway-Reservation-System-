package com.kaushik.railway.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "railone_session")

data class UserSession(
    val loggedIn: Boolean = false,
    val name: String = "",
    val email: String = "",
    val mobile: String = ""
)

class SessionStore(private val context: Context) {
    private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_MOBILE = stringPreferencesKey("mobile")

    val sessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        UserSession(
            loggedIn = prefs[KEY_LOGGED_IN] ?: false,
            name = prefs[KEY_NAME] ?: "",
            email = prefs[KEY_EMAIL] ?: "",
            mobile = prefs[KEY_MOBILE] ?: ""
        )
    }

    suspend fun saveSession(name: String, email: String, mobile: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_NAME] = name
            prefs[KEY_EMAIL] = email
            prefs[KEY_MOBILE] = mobile
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun updateProfile(name: String, mobile: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NAME] = name
            prefs[KEY_MOBILE] = mobile
        }
    }
}
