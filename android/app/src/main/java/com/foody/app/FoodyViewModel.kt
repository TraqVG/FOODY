package com.foody.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID

// The repository owns network and Room; ViewModel owns navigation and transient cart state.
data class CartSelection(val foodId: Int, val qty: Int, val note: String = "")

class FoodyViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FoodyRepository(application)
    var role by mutableStateOf<Role?>(null)
        private set
    var email by mutableStateOf("")
        private set
    var route by mutableStateOf("login")
        private set
    var snapshot by mutableStateOf(SnapshotDto())
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf("")
        private set
    var offline by mutableStateOf(false)
        private set
    var currentTable by mutableStateOf(1)
        private set
    var guestText by mutableStateOf("1")
    var search by mutableStateOf("")
    var category by mutableStateOf("ทั้งหมด")
    val cart = mutableStateListOf<CartSelection>()
    var eventCursor: Long = 0
        private set

    // Keep one idempotency key while the same draft is being retried.
    private var draftKey = UUID.randomUUID().toString()

    fun login(emailInput: String, password: String) = perform {
        val login = repo.login(emailInput, password)
        role = Role.valueOf(login.role)
        email = login.email
        snapshot = repo.refresh()
        eventCursor = snapshot.latestEventId
        route = when(role) { Role.STAFF -> "tables"; Role.KITCHEN -> "kitchen"; Role.OWNER -> "reports"; null -> "login" }
        offline = false
    }
    fun logout() {
        viewModelScope.launch { repo.logout() }
        role = null
        email = ""
        route = "login"
        cart.clear()
        snapshot = SnapshotDto()
        eventCursor = 0
        error = ""
        offline = false
    }
    fun goto(page: String) { route = page; error = "" }
    fun home() = goto(when(role){ Role.STAFF->"tables"; Role.KITCHEN->"kitchen"; Role.OWNER->"reports"; null->"login" })
    fun back() = goto(when(route){
        "table" -> "tables"; "menu" -> "table"; "cart" -> "menu"
        "checkout" -> "table"; "editor" -> "manage"
        "settings" -> when(role) { Role.STAFF->"tables";Role.KITCHEN->"kitchen";Role.OWNER->"reports";null->"login" }
        else -> when(role){Role.STAFF->"tables";Role.KITCHEN->"kitchen";Role.OWNER->"reports";null->"login"}
    })
    fun table(id: Int) = snapshot.tables.firstOrNull { it.id == id } ?: TableDto(id)
    fun tableOrders(tableId: Int) = snapshot.orders.filter {
        it.tableId == tableId && it.status !in setOf("CHECKED_OUT", "CANCELLED")
    }
    fun tableTotal(tableId: Int) = tableOrders(tableId).sumOf { it.total }
    fun canCheckout(tableId: Int): Boolean {
        val orders = tableOrders(tableId)
        return orders.isNotEmpty() && orders.all { it.status == "SERVED" }
    }
    fun selectTable(id: Int) {
        currentTable = id
        guestText = (table(id).guests.takeIf{it>0} ?: 1).toString()
        cart.clear()
        draftKey = UUID.randomUUID().toString()
        goto("table")
    }
    fun setGuests(n: Int) { guestText = n.coerceIn(1, 20).toString() }
    fun openTable() = perform {
        check(role == Role.STAFF)
        val guests = guestText.toIntOrNull() ?: error("กรุณาระบุจำนวนคน")
        require(guests in 1..20) { "จำนวนลูกค้าต้องอยู่ระหว่าง 1–20 คน" }
        repo.openTable(currentTable, guests)
        snapshot = repo.refresh()
        goto("menu")
    }
    fun cartQty(id: Int) = cart.firstOrNull { it.foodId == id }?.qty ?: 0
    fun addToCart(foodId: Int) {
        val food = snapshot.foods.firstOrNull { it.id == foodId && it.available } ?: return
        val index = cart.indexOfFirst { it.foodId == food.id }
        if(index < 0) cart.add(CartSelection(food.id)) else cart[index] = cart[index].copy(qty = (cart[index].qty+1).coerceAtMost(99))
        draftKey = UUID.randomUUID().toString()
    }
    fun updateQty(foodId: Int, qty: Int) {
        val index = cart.indexOfFirst { it.foodId == foodId }
        if(index < 0) return
        if(qty <= 0) cart.removeAt(index) else cart[index] = cart[index].copy(qty=qty.coerceAtMost(99))
        draftKey = UUID.randomUUID().toString()
    }
    fun updateNote(foodId: Int, note: String) {
        val index=cart.indexOfFirst{it.foodId==foodId}
        if(index>=0) cart[index]=cart[index].copy(note=note.take(160))
        draftKey=UUID.randomUUID().toString()
    }
    fun cartTotal(): Int = cart.sumOf { c ->
        (snapshot.foods.firstOrNull { it.id == c.foodId }?.price ?: 0) * c.qty
    }
    fun sendOrder() = perform {
        check(role == Role.STAFF)
        require(cart.isNotEmpty()) { "กรุณาเลือกเมนูก่อนส่งครัว" }
        val lines=cart.map { NewLineInput(it.foodId,it.qty,it.note) }
        repo.order(currentTable,lines,draftKey)
        cart.clear()
        draftKey=UUID.randomUUID().toString()
        snapshot=repo.refresh()
        goto("table")
    }
    fun finishCooking(id:Int) = perform {
        check(role == Role.KITCHEN)
        repo.ready(id)
        snapshot=repo.refresh()
    }
    fun served(id:Int) = perform {
        check(role == Role.STAFF)
        repo.served(id)
        snapshot=repo.refresh()
    }
    fun checkout() = perform {
        check(role == Role.STAFF)
        check(canCheckout(currentTable)) { "ต้องเสิร์ฟทุกรายการก่อนเช็กบิล" }
        repo.checkout(currentTable)
        snapshot=repo.refresh()
        goto("tables")
    }
    fun saveFood(id:Int?,name:String,price:Int,category:String) = perform {
        check(role == Role.OWNER)
        require(name.isNotBlank() && price>0) { "กรุณากรอกชื่ออาหารและราคาให้ถูกต้อง" }
        if(id==null) repo.addMenu(name,price,category)
        else repo.editMenu(id,name,price,category)
        snapshot=repo.refresh()
        goto("manage")
    }
    fun toggleFood(food:FoodDto) = perform {
        check(role == Role.OWNER)
        repo.toggleMenu(food)
        snapshot=repo.refresh()
    }
    fun deleteFood(id:Int) = perform {
        check(role == Role.OWNER)
        repo.deleteMenu(id)
        snapshot=repo.refresh()
    }
    suspend fun refresh(silent:Boolean=true) {
        if(role==null || busy) return
        try { snapshot=repo.refresh();offline=false;if(!silent)error="" }
        catch(ex:Exception) {
            offline=true
            val cached=repo.cachedSnapshot()
            if(cached!=null) snapshot=cached
            if(!silent) error=readableError(ex)
        }
    }
    suspend fun fetchEvents(): List<EventDto> {
        if(role==null) return emptyList()
        return try {
            val response=repo.events(eventCursor)
            eventCursor=response.latestEventId
            response.events
        }catch(_:Exception){emptyList()}
    }
    fun clearError() { error="" }
    private fun perform(block:suspend ()->Unit) {
        if(busy) return
        viewModelScope.launch {
            busy=true;error=""
            try { block() }
            catch(ex:Exception){error=readableError(ex)}
            finally{busy=false}
        }
    }
    private fun readableError(ex:Throwable):String = when(ex) {
        is HttpException -> when(ex.code()) {
            401 -> "บัญชีหมดอายุหรือเข้าสู่ระบบไม่ถูกต้อง"
            403 -> "บัญชีนี้ไม่มีสิทธิ์ทำรายการ"
            404 -> "ไม่พบข้อมูลที่ต้องการ"
            409 -> "ข้อมูลเปลี่ยนไปแล้ว กรุณารีเฟรชและลองใหม่"
            else -> "เซิร์ฟเวอร์ตอบกลับผิดพลาด (${ex.code()})"
        }
        is IllegalArgumentException -> ex.message ?: "กรุณาตรวจสอบข้อมูล"
        is IllegalStateException -> ex.message ?: "ไม่สามารถทำรายการได้"
        else -> "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ กรุณาตรวจสอบ FastAPI และเครือข่าย"
    }
}
