package com.example.pizzaconfigurator.customer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.pizzaconfigurator.customer.data.dto.AuthResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Mirrors the web app's `localStorage["pizza-configurator.auth"]` (customerId/name/email/token
 * JSON blob): a null token means guest, matching `isGuest = state.token === null` in
 * `AuthContext.tsx`. Browsing/configuring/basket all work in guest mode.
 */
class AuthStore(context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val CUSTOMER_ID = stringPreferencesKey("auth.customerId")
        val NAME = stringPreferencesKey("auth.name")
        val EMAIL = stringPreferencesKey("auth.email")
        val TOKEN = stringPreferencesKey("auth.token")
    }

    data class AuthState(val customerId: String?, val name: String?, val email: String?, val token: String?) {
        val isGuest: Boolean get() = token == null
    }

    val authState: Flow<AuthState> = dataStore.data.map { prefs ->
        AuthState(
            customerId = prefs[Keys.CUSTOMER_ID],
            name = prefs[Keys.NAME],
            email = prefs[Keys.EMAIL],
            token = prefs[Keys.TOKEN]
        )
    }

    suspend fun save(response: AuthResponse) {
        dataStore.edit { prefs ->
            prefs[Keys.CUSTOMER_ID] = response.customerId
            prefs[Keys.NAME] = response.name
            prefs[Keys.EMAIL] = response.email
            prefs[Keys.TOKEN] = response.token
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.CUSTOMER_ID)
            prefs.remove(Keys.NAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.TOKEN)
        }
    }
}
