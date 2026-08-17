package com.example.pizzaconfigurator.customer.data.remote

import com.example.pizzaconfigurator.customer.data.dto.AddBasketItemRequest
import com.example.pizzaconfigurator.customer.data.dto.AuthResponse
import com.example.pizzaconfigurator.customer.data.dto.BasketView
import com.example.pizzaconfigurator.customer.data.dto.ConfigurableOptions
import com.example.pizzaconfigurator.customer.data.dto.ConfigurationInput
import com.example.pizzaconfigurator.customer.data.dto.ConfigurationSessionView
import com.example.pizzaconfigurator.customer.data.dto.CreateOrderRequest
import com.example.pizzaconfigurator.customer.data.dto.LoginRequest
import com.example.pizzaconfigurator.customer.data.dto.OrderCheckoutResponse
import com.example.pizzaconfigurator.customer.data.dto.OrderView
import com.example.pizzaconfigurator.customer.data.dto.PizzaSummary
import com.example.pizzaconfigurator.customer.data.dto.PriceResponse
import com.example.pizzaconfigurator.customer.data.dto.RegisterRequest
import com.example.pizzaconfigurator.customer.data.dto.ReviewOutcome
import com.example.pizzaconfigurator.customer.data.dto.ReviewRequestView
import com.example.pizzaconfigurator.customer.data.dto.ValidationResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Every call takes an explicit nullable Bearer token, mirroring `client.ts` in the web apps
 * (no global auth interceptor there either — each call site decides whether to authenticate).
 * Retrofit omits a `@Header` entirely when the argument is null.
 */
interface ApiService {

    @POST("/api/v1/customers/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("/api/v1/customers/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("/api/v1/catalog/pizzas")
    suspend fun listPizzas(): List<PizzaSummary>

    @GET("/api/v1/catalog/pizzas/{pizzaId}/options")
    suspend fun pizzaOptions(@Path("pizzaId") pizzaId: String): ConfigurableOptions

    @POST("/api/v1/configurations")
    suspend fun createConfiguration(
        @Body request: ConfigurationInput,
        @Header("Authorization") bearer: String?
    ): ConfigurationSessionView

    @PUT("/api/v1/configurations/{configurationId}")
    suspend fun updateConfiguration(
        @Path("configurationId") configurationId: String,
        @Body request: ConfigurationInput
    ): ConfigurationSessionView

    @POST("/api/v1/configurations/{configurationId}/validate")
    suspend fun validateConfiguration(@Path("configurationId") configurationId: String): ValidationResponse

    @POST("/api/v1/configurations/{configurationId}/price")
    suspend fun priceConfiguration(@Path("configurationId") configurationId: String): PriceResponse

    @GET("/api/v1/configurations/{configurationId}")
    suspend fun getConfiguration(@Path("configurationId") configurationId: String): ConfigurationSessionView

    @GET("/api/v1/configurations/{configurationId}/recommendation")
    suspend fun getRecommendation(@Path("configurationId") configurationId: String): ReviewRequestView

    @POST("/api/v1/configurations/{configurationId}/recommendation/accept")
    suspend fun acceptRecommendation(@Path("configurationId") configurationId: String): ReviewOutcome

    @POST("/api/v1/configurations/{configurationId}/recommendation/reject")
    suspend fun rejectRecommendation(@Path("configurationId") configurationId: String): ReviewOutcome

    @POST("/api/v1/baskets")
    suspend fun createBasket(@Header("Authorization") bearer: String?): BasketView

    @GET("/api/v1/baskets/{basketId}")
    suspend fun getBasket(@Path("basketId") basketId: String): BasketView

    @POST("/api/v1/baskets/{basketId}/items")
    suspend fun addBasketItem(
        @Path("basketId") basketId: String,
        @Body request: AddBasketItemRequest
    ): BasketView

    @DELETE("/api/v1/baskets/{basketId}/items/{basketItemId}")
    suspend fun removeBasketItem(
        @Path("basketId") basketId: String,
        @Path("basketItemId") basketItemId: String
    ): BasketView

    @POST("/api/v1/orders")
    suspend fun createOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("Authorization") bearer: String?,
        @Body request: CreateOrderRequest
    ): OrderCheckoutResponse

    @GET("/api/v1/orders/{displayNumber}/status")
    suspend fun getOrderStatus(
        @Path("displayNumber") displayNumber: String,
        @Header("Authorization") bearer: String?,
        @Query("token") guestAccessToken: String?
    ): OrderView
}
