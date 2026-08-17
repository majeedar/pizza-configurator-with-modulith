package com.example.pizzaconfigurator.customer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Mirrors the web app's `localStorage["pizza-configurator.basketId"]` plain string entry. */
class BasketIdStore(context: Context) {
    private val dataStore = context.dataStore
    private val key = stringPreferencesKey("basket.id")

    val basketId: Flow<String?> = dataStore.data.map { it[key] }

    suspend fun currentOrNull(): String? = basketId.first()

    suspend fun save(basketId: String) {
        dataStore.edit { it[key] = basketId }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
