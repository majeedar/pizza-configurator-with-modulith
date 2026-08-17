package com.example.pizzaconfigurator.customer.push

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pizzaconfigurator.customer.MainActivity
import com.example.pizzaconfigurator.customer.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

/**
 * Renders incoming order status/ready pushes (agent.md §7.9). Handles both a FCM
 * "notification" payload (title/body already set by the server) and a data-only payload
 * (title/body read from custom keys) defensively, since `FcmPushNotificationProvider`'s exact
 * payload shape isn't fixed by a contract test on the Android side.
 *
 * The app never registers for a *persistent* device token here — per agent.md §8.5/§9.1, the
 * token is only read and submitted once, at order creation time (see CheckoutViewModel), bound
 * to that specific order. `onNewToken` is intentionally a no-op: there is no server-side
 * "update my device token" endpoint to call, since tokens aren't tied to a customer/device
 * registration, only to a single already-placed order.
 */
class PizzaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Intentional no-op — see class doc.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Order update"
        val body = message.notification?.body ?: message.data["body"] ?: "Your order status has changed."
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // User denied the permission — the app must keep working without push (agent.md §8.5).
            return
        }
        val intent = android.content.Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, getString(R.string.order_notification_channel_id))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }
}
