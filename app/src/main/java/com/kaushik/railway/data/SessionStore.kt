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
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "railone_session")

data class UserSession(
    val loggedIn: Boolean = false,
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val userId: String = ""
)

/** Last search/book route for one-tap rebook */
data class LastJourney(
    val fromCode: String = "",
    val toCode: String = "",
    val classCode: String = "",
    val quota: String = "GN - General",
    val trainNumber: String = "",
    val trainName: String = ""
)

class SessionStore(private val context: Context) {
    private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_MOBILE = stringPreferencesKey("mobile")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_LAST_JOURNEY = stringPreferencesKey("last_journey_json")
    private val KEY_SAVED_PASSENGERS = stringPreferencesKey("saved_passengers_json")

    val sessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        UserSession(
            loggedIn = prefs[KEY_LOGGED_IN] ?: false,
            name = prefs[KEY_NAME] ?: "",
            email = prefs[KEY_EMAIL] ?: "",
            mobile = prefs[KEY_MOBILE] ?: "",
            userId = prefs[KEY_USER_ID] ?: ""
        )
    }

    val lastJourneyFlow: Flow<LastJourney?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_JOURNEY]?.let { parseLastJourney(it) }
    }

    val savedPassengersFlow: Flow<List<Passenger>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_PASSENGERS]?.let { parsePassengers(it) } ?: emptyList()
    }

    suspend fun saveSession(name: String, email: String, mobile: String = "", userId: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_NAME] = name
            prefs[KEY_EMAIL] = email
            if (mobile.isNotBlank()) prefs[KEY_MOBILE] = mobile
            if (userId.isNotBlank()) prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun clearSession() {
        // Keep last journey + saved passengers across logout for convenience
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = false
            prefs.remove(KEY_NAME)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_USER_ID)
            // mobile / last journey / passengers kept
        }
    }

    suspend fun updateProfile(name: String, mobile: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NAME] = name
            prefs[KEY_MOBILE] = mobile
        }
    }

    suspend fun saveLastJourney(j: LastJourney) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_JOURNEY] = JSONObject()
                .put("fromCode", j.fromCode)
                .put("toCode", j.toCode)
                .put("classCode", j.classCode)
                .put("quota", j.quota)
                .put("trainNumber", j.trainNumber)
                .put("trainName", j.trainName)
                .toString()
        }
    }

    suspend fun savePassengers(list: List<Passenger>) {
        val arr = JSONArray()
        list.filter { it.name.isNotBlank() }.forEach { p ->
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("age", p.age)
                    .put("gender", p.gender)
                    .put("berth", p.berth)
                    .put("concession", p.concession)
            )
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_PASSENGERS] = arr.toString()
        }
    }

    private fun parseLastJourney(raw: String): LastJourney? = try {
        val o = JSONObject(raw)
        LastJourney(
            fromCode = o.optString("fromCode"),
            toCode = o.optString("toCode"),
            classCode = o.optString("classCode"),
            quota = o.optString("quota", "GN - General"),
            trainNumber = o.optString("trainNumber"),
            trainName = o.optString("trainName")
        )
    } catch (_: Exception) {
        null
    }

    private fun parsePassengers(raw: String): List<Passenger> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Passenger(
                name = o.optString("name"),
                age = o.optString("age"),
                gender = o.optString("gender", "Male"),
                berth = o.optString("berth", "No Preference"),
                concession = o.optString("concession", "None")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
