package com.example.pizzaconfigurator.kitchen.data.remote

import com.example.pizzaconfigurator.kitchen.data.dto.AppLinkView
import com.example.pizzaconfigurator.kitchen.data.dto.ConfigurationPatchRequest
import com.example.pizzaconfigurator.kitchen.data.dto.OrderView
import com.example.pizzaconfigurator.kitchen.data.dto.RejectRequest
import com.example.pizzaconfigurator.kitchen.data.dto.ReviewOutcome
import com.example.pizzaconfigurator.kitchen.data.dto.ReviewRequestView
import com.example.pizzaconfigurator.kitchen.data.dto.StaffLoginRequest
import com.example.pizzaconfigurator.kitchen.data.dto.StaffLoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("/api/v1/staff/login")
    suspend fun login(@Body request: StaffLoginRequest): StaffLoginResponse

    @GET("/api/v1/kitchen/orders")
    suspend fun listOrders(@Header("Authorization") bearer: String): List<OrderView>

    @POST("/api/v1/kitchen/orders/{orderId}/approve")
    suspend fun approve(@Path("orderId") orderId: String, @Header("Authorization") bearer: String): OrderView

    @POST("/api/v1/kitchen/orders/{orderId}/start")
    suspend fun start(@Path("orderId") orderId: String, @Header("Authorization") bearer: String): OrderView

    @POST("/api/v1/kitchen/orders/{orderId}/ready")
    suspend fun ready(@Path("orderId") orderId: String, @Header("Authorization") bearer: String): OrderView

    @POST("/api/v1/kitchen/orders/{orderId}/complete")
    suspend fun complete(@Path("orderId") orderId: String, @Header("Authorization") bearer: String): OrderView

    @GET("/api/v1/kitchen/reviews")
    suspend fun listReviews(@Header("Authorization") bearer: String): List<ReviewRequestView>

    @POST("/api/v1/kitchen/reviews/{reviewId}/accept")
    suspend fun acceptReview(@Path("reviewId") reviewId: String, @Header("Authorization") bearer: String): ReviewOutcome

    @POST("/api/v1/kitchen/reviews/{reviewId}/recommend")
    suspend fun recommendReview(
        @Path("reviewId") reviewId: String,
        @Header("Authorization") bearer: String,
        @Body request: ConfigurationPatchRequest
    ): ReviewOutcome

    @POST("/api/v1/kitchen/reviews/{reviewId}/reject")
    suspend fun rejectReview(
        @Path("reviewId") reviewId: String,
        @Header("Authorization") bearer: String,
        @Body request: RejectRequest
    ): ReviewOutcome

    @GET("/api/v1/kitchen/app-links/android/customer")
    suspend fun customerAppLink(@Header("Authorization") bearer: String): AppLinkView
}
