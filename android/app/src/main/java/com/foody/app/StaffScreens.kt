package com.foody.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(vm:FoodyViewModel){
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState())) {
        Surface(color=Orange) {
            Column(Modifier.fillMaxWidth().padding(top=55.dp,bottom=46.dp,start=24.dp)) {
                Text("Foody",fontSize=38.sp,fontWeight=FontWeight.ExtraBold,color=Color.White)
                Text("ระบบรับออเดอร์ภายในร้านอาหาร",fontSize=15.sp,color=Color.White)
            }
        }
        Column(Modifier.fillMaxWidth().widthIn(max=480.dp).padding(22.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("เข้าสู่ระบบ",fontSize=28.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value=username,onValueChange={username=it},singleLine=true,
                label={Text("อีเมลร้าน")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value=password,onValueChange={password=it},singleLine=true,
                label={Text("รหัสผ่าน")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),
                visualTransformation=if(show) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon={ TextButton(onClick={show=!show}) { Text(if(show)"ซ่อน" else "แสดง") } }
            )
            if(vm.error.isNotBlank()) Text(vm.error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=9.dp))
            Spacer(Modifier.height(18.dp))
            Button(onClick={vm.login(username,password)},enabled=!vm.busy,
                modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(15.dp)) {
                Text(if(vm.busy)"กำลังเข้าสู่ระบบ..." else "เข้าสู่ระบบ",fontSize=17.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Pane {
                HeaderText("บัญชีทดลอง")
                Spacer(Modifier.height(6.dp))
                listOf("staff@foody.shop" to "พนักงานเสิร์ฟ",
                    "kitchen@foody.shop" to "พนักงานครัว", "owner@foody.shop" to "เจ้าของร้าน")
                    .forEach { (account,role) ->
                        Row(Modifier.fillMaxWidth().clickable { username=account;password="foody123" }
                            .padding(vertical=11.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(role,fontWeight=FontWeight.Bold)
                                Text(account,fontSize=13.sp,color=SecondaryInk)
                            }
                            Text("เลือก",color=Orange,fontWeight=FontWeight.Bold)
                        }
                        if(account!="owner@foody.shop") HorizontalDivider()
                    }
            }
        }
    }
}

@Composable
fun TablesScreen(vm:FoodyViewModel){
    val occupied=vm.snapshot.tables.count{it.guests>0}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            StatusCount("ว่าง",vm.snapshot.tables.count{it.guests==0},true,Modifier.weight(1f))
            StatusCount("มีลูกค้า",occupied,false,Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns=GridCells.Fixed(2),
            horizontalArrangement=Arrangement.spacedBy(12.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(bottom=16.dp)
        ) {
            items(vm.snapshot.tables,key={it.id}) { seat ->
                val busy=seat.guests>0
                Pane(Modifier.fillMaxWidth().clickable{vm.selectTable(seat.id)}) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.TableRestaurant,null,tint=Orange,modifier=Modifier.size(23.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("โต๊ะ ${seat.id}",fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1)
                    }
                    Spacer(Modifier.height(10.dp))
                    Tag(if(busy)"มีลูกค้า" else "ว่าง",!busy)
                    Spacer(Modifier.height(11.dp))
                    Text(if(busy)"${seat.guests} คน" else "พร้อมเปิดโต๊ะ",color=SecondaryInk,fontSize=14.sp)
                    if(busy) Text("${vm.tableOrders(seat.id).sumOf{it.lines.sumOf{l->l.qty}}} รายการ",fontSize=13.sp,color=SecondaryInk)
                }
            }
        }
    }
}
@Composable
private fun StatusCount(label:String,count:Int,good:Boolean,modifier:Modifier=Modifier){
    Surface(modifier=modifier,color=if(good)PaleGreen else PaleOrange,shape=RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            Text(label,fontSize=14.sp,color=if(good)Success else OrangeDark)
            Spacer(Modifier.width(7.dp))
            Text("$count",fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(good)Success else OrangeDark)
        }
    }
}

@Composable
fun GuestCountScreen(vm:FoodyViewModel){
    var first by remember(vm.currentTable){mutableStateOf(true)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Pane {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                HeaderText("โต๊ะ ${vm.currentTable}")
                Tag("ว่าง",true)
            }
        }
        Spacer(Modifier.height(23.dp))
        HeaderText("จำนวนลูกค้า")
        Spacer(Modifier.height(12.dp))
        Pane {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                OutlinedIconButton(onClick={vm.setGuests((vm.guestText.toIntOrNull()?:1)-1)}) {
                    Icon(Icons.Default.Remove,"ลดจำนวนลูกค้า")
                }
                Text(vm.guestText,fontSize=42.sp,fontWeight=FontWeight.Bold)
                OutlinedIconButton(onClick={vm.setGuests((vm.guestText.toIntOrNull()?:1)+1)}) {
                    Icon(Icons.Default.Add,"เพิ่มจำนวนลูกค้า")
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("","0","⌫")).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom=9.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                row.forEach { digit ->
                    if(digit.isEmpty()) Spacer(Modifier.weight(1f).height(54.dp))
                    else Surface(
                        modifier=Modifier.weight(1f).height(54.dp).clickable {
                            val prefix=if(first) "" else vm.guestText
                            first=false
                            val value=if(digit=="⌫")prefix.dropLast(1).ifBlank{"1"} else (prefix+digit).take(2)
                            vm.setGuests(value.toIntOrNull()?:1)
                        },
                        shape=RoundedCornerShape(14.dp),color=Color.White,shadowElevation=1.dp
                    ) { Box(contentAlignment=Alignment.Center) { Text(digit,fontSize=23.sp,fontWeight=FontWeight.Bold) } }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("ระบุ 1–20 คน จากนั้นกดเปิดโต๊ะ",fontSize=13.sp,color=SecondaryInk)
    }
}

@Composable
fun TableDetailScreen(vm:FoodyViewModel){
    val seat=vm.table(vm.currentTable)
    val orders=vm.tableOrders(seat.id).sortedBy{it.time}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),
        contentPadding=PaddingValues(vertical=16.dp)) {
        item { Pane {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                HeaderText("โต๊ะ ${seat.id}")
                Tag("มีลูกค้า",false)
            }
            Spacer(Modifier.height(8.dp))
            Text("${seat.guests} คน",fontSize=16.sp,color=SecondaryInk)
        } }
        item { HeaderText("รายการอาหาร") }
        if(orders.isEmpty()) item { Pane {Text("ยังไม่มีออเดอร์",fontWeight=FontWeight.Bold)} }
        items(orders,key={it.id}) { order ->
            Pane {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text("ออเดอร์ #${order.id}",fontWeight=FontWeight.Bold)
                    Tag(when(order.status){"PENDING"->"รอครัว";"READY"->"พร้อมเสิร์ฟ";else->"เสิร์ฟแล้ว"},order.status!="PENDING")
                }
                Spacer(Modifier.height(9.dp))
                order.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("${line.name} ×${line.qty}",fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                            if(line.note.isNotBlank()) Text(line.note,fontSize=12.sp,color=SecondaryInk)
                        }
                        Text(money(line.qty*line.price),fontWeight=FontWeight.Bold)
                    }
                }
                if(order.status=="READY") {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick={vm.served(order.id)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=Success)) {
                        Icon(Icons.Default.Check,null);Spacer(Modifier.width(7.dp));Text("ยืนยันเสิร์ฟแล้ว")
                    }
                }
            }
        }
        item { Pane {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                HeaderText("ยอดรวม")
                PriceText(vm.tableTotal(seat.id))
            }
            if(!vm.canCheckout(seat.id)) Text("เช็กบิลได้เมื่อทุกออเดอร์เสิร์ฟครบ",fontSize=13.sp,color=SecondaryInk)
        } }
    }
}
@Composable
fun OccupiedBottom(vm:FoodyViewModel){
    Surface(color=Color.White,shadowElevation=5.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick={vm.goto("menu")},modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(14.dp)) {
                Text("สั่งอาหารเพิ่ม",fontSize=14.sp,fontWeight=FontWeight.Bold)
            }
            Button(onClick={vm.goto("checkout")},modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(14.dp)) {
                Text("เช็กบิล",fontSize=15.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MenuScreen(vm:FoodyViewModel){
    val categories=listOf("ทั้งหมด","อาหารจานเดียว","กับข้าว","เครื่องดื่ม","ของหวาน")
    val foods=vm.snapshot.foods.filter{
        it.available && (vm.category=="ทั้งหมด" || it.category==vm.category) && it.name.contains(vm.search,ignoreCase=true)
    }
    Column(Modifier.fillMaxSize()) {
        Text("โต๊ะ ${vm.currentTable}  ·  ${vm.table(vm.currentTable).guests} คน",fontSize=16.sp,fontWeight=FontWeight.Bold,color=OrangeDark,
            modifier=Modifier.padding(start=16.dp,top=13.dp,bottom=7.dp))
        OutlinedTextField(value=vm.search,onValueChange={vm.search=it},singleLine=true,
            leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("ค้นหาเมนู")},
            modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),shape=RoundedCornerShape(15.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(selected=vm.category==category,onClick={vm.category=category},label={Text(category,fontSize=13.sp)})
            }
        }
        if(foods.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {Text("ไม่พบเมนู",color=SecondaryInk)}
        else LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.fillMaxSize(),
            contentPadding=PaddingValues(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(foods,key={it.id}) { food ->
                Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),
                    elevation=CardDefaults.cardElevation(defaultElevation=2.dp)) {
                    DishImage(food.image,Modifier.fillMaxWidth().height(124.dp))
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(food.name,fontSize=16.sp,fontWeight=FontWeight.Bold,minLines=2,maxLines=2,
                            overflow=TextOverflow.Ellipsis,lineHeight=20.sp)
                        Spacer(Modifier.height(5.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            PriceText(food.price)
                            FilledIconButton(onClick={vm.addToCart(food.id)},modifier=Modifier.size(42.dp)) {
                                Icon(Icons.Default.Add,"เพิ่ม ${food.name}")
                            }
                        }
                        if(vm.cartQty(food.id)>0) Text("เลือกแล้ว ${vm.cartQty(food.id)}",fontSize=12.sp,color=Success)
                    }
                }
            }
        }
    }
}
@Composable
fun MenuBottom(vm:FoodyViewModel){
    Surface(color=Color.White,shadowElevation=7.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${vm.cart.sumOf{it.qty}} รายการ",fontSize=14.sp,fontWeight=FontWeight.Bold)
                PriceText(vm.cartTotal())
            }
            Button(onClick={vm.goto("cart")},enabled=vm.cart.isNotEmpty(),modifier=Modifier.height(52.dp),shape=RoundedCornerShape(14.dp)) {
                Text("ดูรายการสั่ง",fontSize=15.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.width(5.dp));Icon(Icons.Default.ArrowForward,null)
            }
        }
    }
}
@Composable
fun CartScreen(vm:FoodyViewModel){
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=14.dp)) {
        item { HeaderText("โต๊ะ ${vm.currentTable}  ·  ${vm.table(vm.currentTable).guests} คน") }
        if(vm.cart.isEmpty()) item { Pane {Text("ยังไม่มีรายการอาหาร",fontWeight=FontWeight.Bold)} }
        items(vm.cart.toList(),key={it.foodId}) { entry ->
            val food=vm.snapshot.foods.firstOrNull{it.id==entry.foodId}
            if(food!=null) Pane {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    DishImage(food.image,Modifier.size(64.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(food.name,maxLines=2,fontWeight=FontWeight.Bold,fontSize=15.sp)
                        PriceText(food.price*entry.qty)
                    }
                    IconButton(onClick={vm.updateQty(entry.foodId,entry.qty-1)}) {Icon(Icons.Default.Remove,"ลด")}
                    Text("${entry.qty}",fontWeight=FontWeight.Bold,fontSize=17.sp)
                    IconButton(onClick={vm.updateQty(entry.foodId,entry.qty+1)}) {Icon(Icons.Default.Add,"เพิ่ม")}
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value=entry.note,onValueChange={vm.updateNote(entry.foodId,it)},
                    label={Text("หมายเหตุ (ถ้ามี)")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp))
            }
        }
        item {Pane {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                HeaderText("รวม");PriceText(vm.cartTotal())
            }
            Text("ยอดสุดท้ายตรวจสอบจากเซิร์ฟเวอร์",fontSize=12.sp,color=SecondaryInk)
        }}
    }
}
@Composable
fun CheckoutScreen(vm:FoodyViewModel){
    val orders=vm.tableOrders(vm.currentTable)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {Pane {
            HeaderText("โต๊ะ ${vm.currentTable}")
            Text("${vm.table(vm.currentTable).guests} คน",color=SecondaryInk)
        }}
        item {Pane {
            HeaderText("สรุปบิล")
            Spacer(Modifier.height(12.dp))
            orders.flatMap{it.lines}.forEach{line ->
                Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("${line.name} ×${line.qty}",modifier=Modifier.weight(1f),fontSize=15.sp)
                    Text(money(line.qty*line.price),fontWeight=FontWeight.Bold)
                }
            }
            HorizontalDivider(Modifier.padding(vertical=11.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                HeaderText("ยอดชำระ");PriceText(vm.tableTotal(vm.currentTable))
            }
        }}
        item {Pane {
            Text("รับเงินสด (จำลอง)",fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Tag(if(vm.canCheckout(vm.currentTable))"พร้อมปิดโต๊ะ" else "ยังเสิร์ฟอาหารไม่ครบ",vm.canCheckout(vm.currentTable))
        }}
    }
}
@Composable
fun HistoryScreen(vm:FoodyViewModel){
    var today by remember{mutableStateOf(true)}
    val zone=java.util.TimeZone.getTimeZone("Asia/Bangkok")
    fun sameDay(epoch:Long):Boolean {
        val a=java.util.Calendar.getInstance(zone).apply{timeInMillis=epoch}
        val b=java.util.Calendar.getInstance(zone)
        return a.get(java.util.Calendar.YEAR)==b.get(java.util.Calendar.YEAR) && a.get(java.util.Calendar.DAY_OF_YEAR)==b.get(java.util.Calendar.DAY_OF_YEAR)
    }
    val orders=vm.snapshot.orders.filter{it.staffEmail==vm.email && (!today || sameDay(it.time))}.sortedByDescending{it.time}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            FilterChip(selected=today,onClick={today=true},label={Text("วันนี้")})
            FilterChip(selected=!today,onClick={today=false},label={Text("ทั้งหมด")})
        }
        if(orders.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("ยังไม่มีประวัติออเดอร์",color=SecondaryInk)}
        else LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
            items(orders,key={it.id}){order-> Pane {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text("โต๊ะ ${order.tableId}  ·  #${order.id}",fontWeight=FontWeight.Bold,fontSize=17.sp)
                    Tag(when(order.status){"PENDING"->"รอครัว";"READY"->"พร้อมเสิร์ฟ";"SERVED"->"เสิร์ฟแล้ว";else->"เช็กบิลแล้ว"},order.status!="PENDING")
                }
                Spacer(Modifier.height(7.dp))
                Text(order.lines.joinToString(", "){ "${it.name} ×${it.qty}" },fontSize=14.sp,color=SecondaryInk,maxLines=2)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(timeLabel(order.time),fontSize=13.sp,color=SecondaryInk)
                    PriceText(order.total)
                }
            }}
        }
    }
}
