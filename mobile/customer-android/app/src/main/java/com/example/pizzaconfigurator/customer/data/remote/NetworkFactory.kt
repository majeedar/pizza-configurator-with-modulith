package com.example.pizzaconfigurator.customer.data.remote

import com.example.pizzaconfigurator.customer.BuildConfig
import com.example.pizzaconfigurator.customer.data.dto.ApiException
import com.example.pizzaconfigurator.customer.data.dto.ProblemDetail
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

object NetworkFactory {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val apiService: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        retrofit.create()
    }
}

/**
 * Mirrors `client.ts`'s error handling: on a non-2xx response, try to parse an RFC 7807
 * ProblemDetail body and surface its `detail`/`title`, falling back to a generic message.
 */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: HttpException) {
        val status = e.code()
        val errorBody = e.response()?.errorBody()?.string()
        val message = errorBody
            ?.let { runCatching { NetworkFactory.json.decodeFromString<ProblemDetail>(it) }.getOrNull() }
            ?.let { it.detail ?: it.title }
            ?: "Request failed with status $status"
        throw ApiException(status, message)
    }
