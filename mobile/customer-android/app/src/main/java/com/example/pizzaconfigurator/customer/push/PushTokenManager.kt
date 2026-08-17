package com.example.pizzaconfigurator.customer.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Best-effort FCM token fetch for order creation (agent.md §8.5/§9.1). Any failure (no Google
 * Play Services, misconfigured Firebase project, network error) resolves to null rather than
 * throwing — order creation must succeed with `fcmDeviceToken: null` exactly like a web checkout,
 * per agent.md §7.9's "the app must work correctly even if ... push delivery fails."
 */
object PushTokenManager {
    suspend fun fetchTokenOrNull(): String? = suspendCancellableCoroutine { continuation ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> continuation.resume(token) }
                .addOnFailureListener { continuation.resume(null) }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}
