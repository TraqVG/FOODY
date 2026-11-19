package com.foody.app

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// This interface is the single API contract for the Android app.
data class AuthRequest(val email: String, val password: String)
data class AuthReply(val token: String, val email: String, val role: String)
data class OpenTableRequest(val guests: Int)
data class NewOrderLine(val foodId: Int, val qty: Int, val note: String)
data class NewOrderRequest(val tableId: Int, val lines: List<NewOrderLine>, val clientKey: String)
data class FoodRequest(val name: String, val price: Int, val category: String)
data class AvailabilityRequest(val available: Boolean)
data class CheckoutRequest(val method: String = "CASH_SIMULATED")
data class ServerSnapshot(
    val tables: List<Seat>,
    val foods: List<Food>,
    val orders: List<Order>,
    val sales: List<Sale>,
    val latestEventId: Long = 0L
)
data class RestaurantEvent(
    val id: Long, val kind: String, val tableId: Int,
    val orderId: Int? = null, val time: Long = 0L
)
data class EventsReply(
    val events: List<RestaurantEvent>,
    val latestEventId: Long
)

interface FoodyApi {
    @POST("v1/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthReply

    @POST("v1/auth/logout")
    suspend fun logout(): JsonObject

    @GET("v1/me")
    suspend fun me(): AuthReply

    @GET("v1/snapshot")
    suspend fun snapshot(): ServerSnapshot

    @GET("v1/events")
    suspend fun events(@Query("after") after: Long): EventsReply

    @POST("v1/tables/{tableId}/open")
    suspend fun openTable(@Path("tableId") tableId: Int, @Body body: OpenTableRequest): JsonObject

    @POST("v1/orders")
    suspend fun createOrder(@Body body: NewOrderRequest): JsonObject

    @POST("v1/orders/{orderId}/ready")
    suspend fun ready(@Path("orderId") orderId: Int): JsonObject

    @POST("v1/orders/{orderId}/served")
    suspend fun served(@Path("orderId") orderId: Int): JsonObject

    @POST("v1/tables/{tableId}/checkout")
    suspend fun checkout(@Path("tableId") tableId: Int, @Body body: CheckoutRequest): JsonObject

    @POST("v1/foods")
    suspend fun createFood(@Body body: FoodRequest): JsonObject

    @PUT("v1/foods/{foodId}")
    suspend fun updateFood(@Path("foodId") foodId: Int, @Body body: FoodRequest): JsonObject

    @PATCH("v1/foods/{foodId}/availability")
    suspend fun availability(@Path("foodId") foodId: Int, @Body body: AvailabilityRequest): JsonObject

    @DELETE("v1/foods/{foodId}")
    suspend fun deleteFood(@Path("foodId") foodId: Int): JsonObject
}
