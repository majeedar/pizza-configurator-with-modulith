package com.example.pizzaconfigurator.kitchen.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

private const val RECONNECT_DELAY_MS = 3000L

/**
 * Ported 1:1 from the staff web app's `kitchenStream.ts`: `EventSource` cannot send an
 * `Authorization` header, and `/api/v1/kitchen/stream` is gated by the same staff bearer JWT as
 * every other staff-gated kitchen endpoint, so a hand-rolled streaming client is required here
 * too (OkHttp's `BufferedSource` standing in for the web version's `ReadableStream` reader).
 *
 * Only the `event:` line of each SSE frame is read — the `data:` field is never parsed. Per
 * agent.md §17, SSE is "an optimization, not the only source of truth": every event, regardless
 * of name, just means "something changed, refetch `GET /api/v1/kitchen/orders`." On any
 * disconnect (stream end or I/O error) this loops with a fixed 3s delay and reconnects, exactly
 * matching the web client's simple fixed backoff.
 */
class KitchenStream(private val bearerTokenProvider: suspend () -> String?) {

    suspend fun listen(onEvent: () -> Unit) {
        while (true) {
            try {
                connectOnce(onEvent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // fall through to reconnect delay
            }
            delay(RECONNECT_DELAY_MS)
        }
    }

    private suspend fun connectOnce(onEvent: () -> Unit) = withContext(Dispatchers.IO) {
        val token = bearerTokenProvider() ?: return@withContext
        val request = Request.Builder()
            .url("${com.example.pizzaconfigurator.kitchen.BuildConfig.API_BASE_URL}/api/v1/kitchen/stream")
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .build()

        NetworkFactory.okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use
            val source = response.body?.source() ?: return@use
            val frame = StringBuilder()
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isEmpty()) {
                    val eventLine = frame.lineSequence().firstOrNull { it.startsWith("event:") }
                    if (eventLine != null) onEvent()
                    frame.clear()
                } else {
                    frame.appendLine(line)
                }
            }
        }
    }
}
