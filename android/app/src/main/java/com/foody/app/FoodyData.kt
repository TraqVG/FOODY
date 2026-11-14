package com.foody.app

import android.app.Application
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.gson.Gson
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Field names intentionally match backend/main.py JSON. Never trust client-side prices.
data class TableDto(val id: Int = 0, val guests: Int = 0)
data class FoodDto(
    val id: Int = 0, val name: String = "", val price: Int = 0,
    val category: String = "", val image: Int = 8, val available: Boolean = true
)
data class OrderLineDto(
    val foodId: Int = 0, val name: String = "", val price: Int = 0,
    val qty: Int = 0, val note: String = ""
)
data class OrderDto(
    val id: Int = 0, val tableId: Int = 0, val staffEmail: String = "",
    val time: Long = 0, val status: String = "PENDING", val lines: List<OrderLineDto> = emptyList()
) {
    val total: Int get() = lines.sumOf { it.price * it.qty }
}
data class SaleDto(
    val id: Int = 0, val tableId: Int = 0, val time: Long = 0,
    val amount: Int = 0, val orderIds: List<Int> = emptyList()
)
data class SnapshotDto(
    val tables: List<TableDto> = emptyList(),
    val foods: List<FoodDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val sales: List<SaleDto> = emptyList(),
    val latestEventId: Long = 0,
    val serverTime: Long = 0
)
data class LoginInput(val email: String, val password: String)
data class LoginDto(val token: String, val email: String, val role: String)
data class GuestsInput(val guests: Int)
data class NewLineInput(val foodId: Int, val qty: Int, val note: String = "")
data class OrderInput(val tableId: Int, val lines: List<NewLineInput>, val clientKey: String)
data class FoodInput(val name: String, val price: Int, val category: String)
data class AvailabilityInput(val available: Boolean)
data class CheckoutInput(val method: String = "CASH_SIMULATED")
data class EventDto(val id: Long, val kind: String, val tableId: Int, val orderId: Int?, val time: Long)
data class EventsDto(val events: List<EventDto>, val latestEventId: Long)

enum class Role(val label: String) {
    STAFF("พนักงานเสิร์ฟ"), KITCHEN("พนักงานครัว"), OWNER("เจ้าของร้าน")
}

interface FoodyApi {
    @POST("v1/auth/login") suspend fun login(@Body body: LoginInput): LoginDto
    @POST("v1/auth/logout") suspend fun logout(@Header("Authorization") auth: String)
    @GET("v1/snapshot") suspend fun snapshot(@Header("Authorization") auth: String): SnapshotDto
    @POST("v1/tables/{id}/open") suspend fun openTable(
        @Header("Authorization") auth: String, @Path("id") id: Int, @Body body: GuestsInput
    )
    @POST("v1/orders") suspend fun newOrder(@Header("Authorization") auth: String, @Body body: OrderInput)
    @POST("v1/orders/{id}/ready") suspend fun ready(@Header("Authorization") auth: String, @Path("id") id: Int)
    @POST("v1/orders/{id}/served") suspend fun served(@Header("Authorization") auth: String, @Path("id") id: Int)
    @POST("v1/tables/{id}/checkout") suspend fun checkout(
        @Header("Authorization") auth: String, @Path("id") id: Int, @Body body: CheckoutInput
    )
    @POST("v1/foods") suspend fun addFood(@Header("Authorization") auth: String, @Body body: FoodInput)
    @PUT("v1/foods/{id}") suspend fun editFood(@Header("Authorization") auth: String, @Path("id") id: Int, @Body body: FoodInput)
    @PATCH("v1/foods/{id}/availability") suspend fun available(
        @Header("Authorization") auth: String, @Path("id") id: Int, @Body body: AvailabilityInput
    )
    @DELETE("v1/foods/{id}") suspend fun deleteFood(@Header("Authorization") auth: String, @Path("id") id: Int)
    @GET("v1/events") suspend fun events(@Header("Authorization") auth: String, @Query("after") after: Long): EventsDto
}

@Entity(tableName = "cached_snapshot")
data class SnapshotCache(@PrimaryKey val id: Int = 1, val json: String)

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM cached_snapshot WHERE id=1") suspend fun read(): SnapshotCache?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun write(row: SnapshotCache)
}

@Database(entities = [SnapshotCache::class], version = 1, exportSchema = false)
abstract class FoodyDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
}

class FoodyRepository(app: Application) {
    private val gson = Gson()
    private val db = Room.databaseBuilder(app, FoodyDatabase::class.java, "foody_cache.db").build()
    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(FoodyApi::class.java)
    private var bearer: String? = null

    suspend fun login(email: String, password: String): LoginDto {
        val result = api.login(LoginInput(email.trim().lowercase(), password))
        bearer = "Bearer ${result.token}"
        return result
    }
    private fun auth() = bearer ?: error("ยังไม่ได้เข้าสู่ระบบ")

    suspend fun logout() {
        runCatching { api.logout(auth()) }
        bearer = null
    }
    suspend fun refresh(): SnapshotDto {
        val result = api.snapshot(auth())
        db.snapshotDao().write(SnapshotCache(json = gson.toJson(result)))
        return result
    }
    suspend fun cachedSnapshot(): SnapshotDto? = db.snapshotDao().read()?.json?.let {
        runCatching { gson.fromJson(it, SnapshotDto::class.java) }.getOrNull()
    }
    suspend fun openTable(id: Int, guests: Int) { api.openTable(auth(), id, GuestsInput(guests)) }
    suspend fun order(tableId: Int, lines: List<NewLineInput>, key: String) {
        api.newOrder(auth(), OrderInput(tableId, lines, key))
    }
    suspend fun ready(id: Int) { api.ready(auth(), id) }
    suspend fun served(id: Int) { api.served(auth(), id) }
    suspend fun checkout(id: Int) { api.checkout(auth(), id, CheckoutInput()) }
    suspend fun addMenu(name: String, price: Int, category: String) {
        api.addFood(auth(), FoodInput(name, price, category))
    }
    suspend fun editMenu(id: Int, name: String, price: Int, category: String) {
        api.editFood(auth(), id, FoodInput(name, price, category))
    }
    suspend fun toggleMenu(food: FoodDto) { api.available(auth(), food.id, AvailabilityInput(!food.available)) }
    suspend fun deleteMenu(id: Int) { api.deleteFood(auth(), id) }
    suspend fun events(after: Long) = api.events(auth(), after)
}
