package com.foody.app

import android.app.Application
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Entity(tableName = "foody_state")
data class SavedState(
    @PrimaryKey val id: Int = 1,
    val json: String
)

@Dao
interface SavedStateDao {
    @Query("SELECT * FROM foody_state WHERE id=1 LIMIT 1")
    suspend fun read(): SavedState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(state: SavedState)

    @Query("DELETE FROM foody_state")
    suspend fun clear()
}

@Database(entities=[SavedState::class], version=1, exportSchema=false)
abstract class FoodyDatabase : RoomDatabase() {
    abstract fun stateDao(): SavedStateDao
}

// MVVM: FoodyStore (ViewModel) -> FoodyRepository -> Retrofit (remote source of truth)
//                                      |
//                                      +-> Room (offline read-only snapshot cache)
class FoodyRepository(private val app: Application) {
    private val prefs = app.getSharedPreferences("foody_session", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val room = Room.databaseBuilder(app, FoodyDatabase::class.java, "foody_local.db").build()
    private val dao = room.stateDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = prefs.getString("token", null)
            val builder = chain.request().newBuilder()
            if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
            chain.proceed(builder.build())
        }.build()

    private val api: FoodyApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(FoodyApi::class.java)

    fun rememberedUser(): Pair<String, FoodyRole>? {
        val email = prefs.getString("email", "") ?: ""
        val role = prefs.getString("role", "") ?: ""
        if (prefs.getString("token", "").isNullOrBlank() || email.isBlank()) return null
        return runCatching { email to FoodyRole.valueOf(role) }.getOrNull()
    }

    suspend fun login(email: String, password: String): AuthReply {
        val response = api.login(AuthRequest(email, password))
        prefs.edit().putString("token", response.token)
            .putString("email", response.email)
            .putString("role", response.role)
            .putLong("event_cursor", -1L)
            .apply()
        return response
    }

    suspend fun logout() {
        runCatching { api.logout() }
        WorkManager.getInstance(app).cancelUniqueWork("foody_events")
        prefs.edit().remove("token").remove("email").remove("role")
            .remove("event_cursor").apply()
        dao.clear()
    }

    suspend fun snapshot(): ServerSnapshot {
        val result = api.snapshot()
        // Store only the latest authenticated server snapshot.
        dao.write(SavedState(json=gson.toJson(result)))
        return result
    }

    suspend fun cached(): ServerSnapshot? =
        runCatching { dao.read()?.json?.let { gson.fromJson(it, ServerSnapshot::class.java) } }.getOrNull()

    suspend fun events(): EventsReply {
        val since = prefs.getLong("event_cursor", -1L)
        val data = api.events(if(since < 0L) 0L else since)
        if (since < 0L) {
            // Do not notify on historical events from before initial login.
            prefs.edit().putLong("event_cursor", data.latestEventId).apply()
            return data.copy(events=emptyList())
        }
        // Avoid duplicate notifications between foreground and WorkManager.
        val unseen = data.events.filter { it.id > since }
        prefs.edit().putLong("event_cursor", data.latestEventId).apply()
        return data.copy(events=unseen)
    }

    suspend fun openTable(tableId: Int, guests: Int) =
        api.openTable(tableId, OpenTableRequest(guests))

    suspend fun createOrder(request: NewOrderRequest) =
        api.createOrder(request)

    suspend fun markReady(orderId: Int) = api.ready(orderId)

    suspend fun markServed(orderId: Int) = api.served(orderId)

    suspend fun checkout(tableId: Int) = api.checkout(tableId, CheckoutRequest())

    suspend fun toggleFood(foodId: Int, available: Boolean) =
        api.availability(foodId, AvailabilityRequest(available))

    suspend fun saveFood(foodId: Int?, request: FoodRequest) =
        if (foodId == null) api.createFood(request)
        else api.updateFood(foodId, request)

    suspend fun deleteFood(foodId: Int) = api.deleteFood(foodId)

    fun notificationsEnabled(): Boolean = prefs.getBoolean("notifications", true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications", enabled).apply()
    }

    fun startBackgroundEvents() {
        val constraints=Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request=PeriodicWorkRequestBuilder<FoodySyncWorker>(15,TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            "foody_events",ExistingPeriodicWorkPolicy.KEEP,request
        )
    }

    fun tokenPresent(): Boolean = !prefs.getString("token",null).isNullOrBlank()
}
