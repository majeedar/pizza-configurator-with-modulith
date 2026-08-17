package com.example.pizzaconfigurator.kitchen.ui.applink

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pizzaconfigurator.kitchen.BuildConfig
import com.example.pizzaconfigurator.kitchen.data.repository.AppLinkRepository

/**
 * The customer Android app's QR code, shown in a corner of the production board (agent.md
 * §8.2/§8.6) so staff can point an in-store customer at it. The `GET
 * /api/v1/kitchen/app-links/android/customer` call (staff-gated) confirms a link exists and is
 * active; the actual PNG is loaded from the *public* `qr.png` endpoint — same one the customer
 * web footer uses — since it needs no auth and is simplest to hand straight to an image loader.
 */
@Composable
fun AppLinkQrCorner(appLinkRepository: AppLinkRepository) {
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        status = try {
            val link = appLinkRepository.customerAppLink()
            if (link.active) "ok" else "inactive"
        } catch (e: Exception) {
            "error"
        }
    }

    if (status == "ok") {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "${BuildConfig.API_BASE_URL}/api/v1/app-links/android/customer/qr.png",
                contentDescription = "Customer app QR code",
                modifier = Modifier.size(72.dp)
            )
            Text("Scan for the customer app", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
