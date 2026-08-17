package com.example.pizzaconfigurator.kitchen.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pizzaconfigurator.kitchen.data.dto.StaffLoginResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pizza_kitchen_prefs")

/** Mirrors the staff web app's `localStorage["staff-web.auth"]` employeeId/username/displayName/role/token blob. */
class StaffAuthStore(context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val EMPLOYEE_ID = stringPreferencesKey("auth.employeeId")
        val USERNAME = stringPreferencesKey("auth.username")
        val DISPLAY_NAME = stringPreferencesKey("auth.displayName")
        val ROLE = stringPreferencesKey("auth.role")
        val TOKEN = stringPreferencesKey("auth.token")
    }

    data class StaffAuthState(
        val employeeId: String?,
        val username: String?,
        val displayName: String?,
        val role: String?,
        val token: String?
    ) {
        val isAuthenticated: Boolean get() = token != null
    }

    val authState: Flow<StaffAuthState> = dataStore.data.map { prefs ->
        StaffAuthState(
            employeeId = prefs[Keys.EMPLOYEE_ID],
            username = prefs[Keys.USERNAME],
            displayName = prefs[Keys.DISPLAY_NAME],
            role = prefs[Keys.ROLE],
            token = prefs[Keys.TOKEN]
        )
    }

    suspend fun save(response: StaffLoginResponse) {
        dataStore.edit { prefs ->
            prefs[Keys.EMPLOYEE_ID] = response.employeeId
            prefs[Keys.USERNAME] = response.username
            prefs[Keys.DISPLAY_NAME] = response.displayName
            prefs[Keys.ROLE] = response.role
            prefs[Keys.TOKEN] = response.token
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
