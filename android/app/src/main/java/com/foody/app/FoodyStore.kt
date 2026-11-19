package com.foody.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class FoodyRole(val label: String) {
    STAFF("พนักงานเสิร์ฟ"),
    KITCHEN("พนักงานครัว"),
    OWNER("เจ้าของร้าน")
}

data class Food(
    val id:Int,
    val name:String,
    val price:Int,
    val category:String,
    val image:Int,
    val available:Boolean=true
)
data class Seat(val id:Int,val guests:Int=0)
data class Line(
    val foodId:Int,
    val name:String,
    val price:Int,
    val qty:Int,
    val note:String=""
)
data class Order(
    val id:Int,
    val tableId:Int,
    val staffEmail:String,
    val time:Long,
    val lines:List<Line>,
    val status:String="PENDING"
)
data class Sale(
    val id:Int,
    val tableId:Int,
    val time:Long,
    val amount:Int,
    val orderIds:List<Int>
)
data class CartItem(val foodId:Int,val qty:Int=1,val note:String="")

// AndroidViewModel = VM layer. Remote backend is the authoritative data source.
// Repository centralizes Retrofit and the Room offline read-only cache.
class FoodyStore(app:Application):AndroidViewModel(app) {
    private val repository=FoodyRepository(app)
    private val syncLock=Mutex()
    private var pendingRequestKey:String?=null
    private val application=app

    var loaded by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var isOffline by mutableStateOf(false)
        private set
    var email by mutableStateOf("")
        private set
    var role by mutableStateOf<FoodyRole?>(null)
        private set
    var route by mutableStateOf("login")
        private set
    var currentTable by mutableStateOf(1)
        private set
    var guestInput by mutableStateOf("1")
    var message by mutableStateOf("")
    var notifications by mutableStateOf(true)
        private set

    val tables=mutableStateListOf<Seat>()
    val foods=mutableStateListOf<Food>()
    val orders=mutableStateListOf<Order>()
    val sales=mutableStateListOf<Sale>()
    val cart=mutableStateListOf<CartItem>()

    init {
        notifications=repository.notificationsEnabled()
        viewModelScope.launch {
            val remembered=repository.rememberedUser()
            if(remembered!=null) {
                email=remembered.first
                role=remembered.second
                route=homeFor(remembered.second)
                repository.cached()?.let(::applySnapshot)
                try { refreshNow(true) }
                catch(e:Exception) {
                    isOffline=true
                    message=describeError(e)
                }
                repository.startBackgroundEvents()
            }
            loaded=true
        }
        viewModelScope.launch {
            while(isActive) {
                delay(4000)
                if(role!=null && !busy) {
                    try { refreshNow(true) }
                    catch(_ :Exception) { isOffline=true }
                }
            }
        }
    }

    private fun homeFor(role:FoodyRole)=when(role) {
        FoodyRole.STAFF -> "tables"
        FoodyRole.KITCHEN -> "kitchen"
        FoodyRole.OWNER -> "reports"
    }

    fun login(inputEmail:String,password:String):Boolean {
        if(busy || inputEmail.isBlank() || password.isBlank()) {
            message="กรอกอีเมลและรหัสผ่าน"
            return false
        }
        busy=true
        message=""
        viewModelScope.launch {
            try {
                val reply=repository.login(inputEmail.trim().lowercase(Locale.ROOT),password)
                role=FoodyRole.valueOf(reply.role)
                email=reply.email
                cart.clear()
                route=homeFor(role!!)
                refreshNow(true)
                repository.startBackgroundEvents()
                message=""
            } catch(e:Exception) {
                message=if(e is HttpException && e.code()==401)
                    "อีเมลหรือรหัสผ่านไม่ถูกต้อง" else describeError(e)
                if(role==null) route="login"
            } finally { busy=false }
        }
        return true
    }

    fun logout() {
        val previousRole=role
        role=null;email="";route="login";message="";cart.clear()
        tables.clear();foods.clear();orders.clear();sales.clear()
        pendingRequestKey=null
        viewModelScope.launch { repository.logout() }
    }

    fun go(target:String) { route=target; message="" }
    fun home() { go(role?.let(::homeFor)?:"login") }
    fun table(id:Int):Seat = tables.firstOrNull{it.id==id}?:Seat(id)
    fun selectTable(id:Int) {
        if(role!=FoodyRole.STAFF || tables.none{it.id==id})return
        currentTable=id
        guestInput=if(table(id).guests==0)"1" else table(id).guests.toString()
        cart.clear()
        pendingRequestKey=null
        go("table")
    }
    fun setGuests(value:Int) { guestInput=value.coerceIn(1,20).toString() }

    fun openTable():Boolean {
        if(role!=FoodyRole.STAFF || busy)return false
        val guests=guestInput.toIntOrNull() ?:return false
        if(guests !in 1..20 || table(currentTable).guests!=0)return false
        val selected=currentTable
        mutate(
            job={ repository.openTable(selected,guests) },
            onSuccess={cart.clear();pendingRequestKey=null;go("menu")}
        )
        return true
    }

    fun add(foodId:Int) {
        if(role!=FoodyRole.STAFF || busy || table(currentTable).guests==0)return
        if(foods.none{it.id==foodId && it.available})return
        val index=cart.indexOfFirst{it.foodId==foodId}
        if(index>=0)cart[index]=cart[index].copy(qty=cart[index].qty+1)
        else cart.add(CartItem(foodId))
        pendingRequestKey=null
    }
    fun cartQty(foodId:Int)=cart.firstOrNull{it.foodId==foodId}?.qty?:0
    fun qty(foodId:Int,value:Int){
        if(busy)return
        val index=cart.indexOfFirst{it.foodId==foodId}
        if(index>=0){
            if(value<1)cart.removeAt(index)
            else cart[index]=cart[index].copy(qty=value.coerceAtMost(99))
        }
        pendingRequestKey=null
    }
    fun note(foodId:Int,note:String){
        if(busy)return
        val index=cart.indexOfFirst{it.foodId==foodId}
        if(index>=0)cart[index]=cart[index].copy(note=note.take(160))
        pendingRequestKey=null
    }
    fun cartTotal()=cart.sumOf{c->(foods.firstOrNull{it.id==c.foodId}?.price?:0)*c.qty}

    fun sendOrder():Boolean {
        if(role!=FoodyRole.STAFF || busy || table(currentTable).guests==0 || cart.isEmpty()) return false
        val invalid=cart.any{c->foods.none{it.id==c.foodId && it.available}}
        if(invalid){message="มีเมนูหยุดขาย กรุณาตรวจสอบรายการ";return false}
        val selected=currentTable
        val lines=cart.toList().map{NewOrderLine(it.foodId,it.qty,it.note)}
        val key=pendingRequestKey?:UUID.randomUUID().toString().also{pendingRequestKey=it}
        mutate(
            job={repository.createOrder(NewOrderRequest(selected,lines,key))},
            onSuccess={cart.clear();pendingRequestKey=null;go("table")}
        )
        return true
    }

    fun ordersOf(tableId:Int)=orders.filter {
        it.tableId==tableId && it.status!="CHECKED_OUT" && it.status!="CANCELLED"
    }
    fun total(tableId:Int)=ordersOf(tableId).sumOf{o->o.lines.sumOf{it.price*it.qty}}
    fun canCheckout(tableId:Int):Boolean = FoodyRules.canCheckout(ordersOf(tableId))

    fun markReady(orderId:Int) {
        if(role!=FoodyRole.KITCHEN || busy)return
        if(orders.none{it.id==orderId && it.status=="PENDING"})return
        mutate(job={repository.markReady(orderId)})
    }
    fun markServed(orderId:Int) {
        if(role!=FoodyRole.STAFF || busy)return
        if(orders.none{it.id==orderId && it.status=="READY"})return
        mutate(job={repository.markServed(orderId)})
    }
    fun checkout():Boolean {
        if(role!=FoodyRole.STAFF || busy || !canCheckout(currentTable))return false
        val selected=currentTable
        mutate(
            job={repository.checkout(selected)},
            onSuccess={cart.clear();pendingRequestKey=null;go("tables")}
        )
        return true
    }

    fun toggleMenu(foodId:Int){
        if(role!=FoodyRole.OWNER || busy)return
        val food=foods.firstOrNull{it.id==foodId} ?:return
        mutate(job={repository.toggleFood(foodId,!food.available)})
    }
    fun saveMenu(id:Int?,name:String,price:Int,category:String){
        if(role!=FoodyRole.OWNER || busy || name.isBlank() || price<=0)return
        val payload=FoodRequest(name.trim(),price,category)
        mutate(
            job={repository.saveFood(id,payload)},
            onSuccess={go("manage")}
        )
    }
    fun deleteMenu(id:Int){
        if(role!=FoodyRole.OWNER || busy)return
        mutate(job={repository.deleteFood(id)})
    }
    fun changeNotifications(value:Boolean) {
        notifications=value
        repository.setNotificationsEnabled(value)
    }
    fun retryRefresh() {
        if(role==null || busy)return
        busy=true
        viewModelScope.launch {
            try { refreshNow(true);message="" }
            catch(e:Exception){message=describeError(e)}
            finally { busy=false }
        }
    }

    private fun mutate(job:suspend ()->Any,onSuccess:()->Unit={}) {
        if(busy || role==null)return
        busy=true
        message=""
        viewModelScope.launch {
            try {
                job()
                refreshNow(true)
                onSuccess()
                message=""
            }catch(e:Exception) {
                isOffline=e is IOException
                message=describeError(e)
            }finally {busy=false}
        }
    }

    private suspend fun refreshNow(withEvents:Boolean) {
        syncLock.withLock {
            val latest=repository.snapshot()
            applySnapshot(latest)
            if(withEvents) {
                val events=repository.events()
                if(notifications)events.events.forEach{FoodyNotifier.show(application,it)}
            }
            isOffline=false
        }
    }

    private fun applySnapshot(snapshot:ServerSnapshot) {
        tables.clear();tables.addAll(snapshot.tables)
        foods.clear();foods.addAll(snapshot.foods)
        orders.clear();orders.addAll(snapshot.orders)
        sales.clear();sales.addAll(snapshot.sales)
    }

    private fun describeError(e:Exception):String = when(e) {
        is IOException -> "เชื่อมต่อ Server ไม่ได้ ตรวจสอบว่า FastAPI เปิดอยู่"
        is HttpException -> when(e.code()) {
            401 -> "เซสชันหมดอายุ กรุณาออกจากระบบและเข้าสู่ระบบใหม่"
            403 -> "บัญชีนี้ไม่มีสิทธิ์ทำรายการ"
            404 -> "ไม่พบข้อมูลที่ต้องการ"
            409 -> "ข้อมูลเปลี่ยนแล้ว กรุณาตรวจสอบโต๊ะหรือออเดอร์อีกครั้ง"
            422 -> "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบก่อนบันทึก"
            else -> "Server ตอบกลับ HTTP ${e.code()}"
        }
        else -> "ทำรายการไม่สำเร็จ: ${e.message.orEmpty().take(80)}"
    }

    fun timeIsToday(time:Long)=calendarMatch(time,0)
    fun timeIsMonth(time:Long)=calendarMatch(time,1)
    fun timeIsYear(time:Long)=calendarMatch(time,2)
    private fun calendarMatch(time:Long,precision:Int):Boolean {
        val tz=TimeZone.getTimeZone("Asia/Bangkok")
        val a=Calendar.getInstance(tz).apply{timeInMillis=time}
        val b=Calendar.getInstance(tz)
        if(a.get(Calendar.YEAR)!=b.get(Calendar.YEAR))return false
        if(precision==2)return true
        if(a.get(Calendar.MONTH)!=b.get(Calendar.MONTH))return false
        return precision==1 || a.get(Calendar.DAY_OF_MONTH)==b.get(Calendar.DAY_OF_MONTH)
    }
    fun salesTotal(precision:Int)=sales.filter{s->
        when(precision){0->timeIsToday(s.time);1->timeIsMonth(s.time);else->timeIsYear(s.time)}
    }.sumOf{it.amount}
}
