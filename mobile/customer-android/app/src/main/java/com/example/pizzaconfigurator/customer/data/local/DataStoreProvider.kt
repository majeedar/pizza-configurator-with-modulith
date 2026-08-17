package com.example.pizzaconfigurator.customer.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * A single shared DataStore instance for this app's small amount of local state (auth + basket
 * id). DataStore throws at runtime if two delegates are created against the same file name, so
 * every local store (AuthStore, BasketIdStore) must reuse this one property rather than each
 * declaring their own `preferencesDataStore(...)` delegate.
 */
val Context.dataStore by preferencesDataStore(name = "pizza_configurator_prefs")
