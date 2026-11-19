package com.foody.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TablesScreen(state:FoodyStore) {
    Column(Modifier.fillMaxSize().padding(16.dp)){
        val occupied=state.tables.count{it.guests>0}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            SummaryChip("ว่าง",6-occupied,true,Modifier.weight(1f))
            SummaryChip("มีลูกค้า",occupied,false,Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns=GridCells.Fixed(2),
            horizontalArrangement=Arrangement.spacedBy(12.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(bottom=16.dp)
        ){
            items(state.tables,key={it.id}) { seat->
                val active=seat.guests>0
                FoodyCard(Modifier.fillMaxWidth().clickable{state.selectTable(seat.id)}) {
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.TableRestaurant,contentDescription=null,
                            tint=FoodyOrange,modifier=Modifier.size(22.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("โต๊ะ ${seat.id}",fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1)
                    }
                    Spacer(Modifier.height(11.dp))
                    StatusPill(if(active)"มีลูกค้า" else "ว่าง",!active)
                    Spacer(Modifier.height(10.dp))
                    Text(if(active)"${seat.guests} คน" else "พร้อมเปิดโต๊ะ",fontSize=14.sp,color=FoodyMuted,maxLines=1)
                    if(active){
                        val total=state.ordersOf(seat.id).sumOf{it.lines.sumOf{l->l.qty}}
                        Text("${total} รายการ",fontSize=13.sp,color=FoodyMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label:String,count:Int,positive:Boolean,modifier:Modifier){
    Surface(modifier=modifier,color=if(positive)FoodyGreenPale else FoodyPeach,shape=RoundedCornerShape(14.dp)){
        Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.Center){
            Text("$label  ",fontSize=14.sp,color=if(positive)FoodyGreen else FoodyOrangeDark)
            Text("$count",fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(positive)FoodyGreen else FoodyOrangeDark)
        }
    }
}

@Composable
fun EmptyTableScreen(state:FoodyStore) {
    var touched by remember(state.currentTable) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp, vertical=14.dp)) {
        FoodyCard {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                SafeText("โต๊ะ ${state.currentTable}",23,true)
                StatusPill("ว่าง",true)
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle("จำนวนลูกค้า")
        Spacer(Modifier.height(12.dp))
        FoodyCard {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween){
                OutlinedIconButton(onClick={state.setGuests((state.guestInput.toIntOrNull()?:1)-1)}) {
                    Icon(Icons.Default.Remove,contentDescription="ลดจำนวนคน")
                }
                Text(state.guestInput.ifBlank{"1"},fontSize=44.sp,fontWeight=FontWeight.Bold)
                OutlinedIconButton(onClick={state.setGuests((state.guestInput.toIntOrNull()?:1)+1)}) {
                    Icon(Icons.Default.Add,contentDescription="เพิ่มจำนวนคน")
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        val cells=listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
        Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
            cells.chunked(3).forEach{row->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
                    row.forEach{value->
                        if(value.isEmpty()) Spacer(Modifier.weight(1f).height(54.dp))
                        else Surface(
                            color=Color.White,
                            shape=RoundedCornerShape(13.dp),
                            shadowElevation=1.dp,
                            modifier=Modifier.weight(1f).height(54.dp).clickable {
                                val old=if(touched)state.guestInput else ""
                                touched=true
                                state.guestInput=if(value=="⌫") old.dropLast(1).ifBlank{"1"}
                                else (old+value).take(2).trimStart('0').ifBlank{"1"}
                                val n=state.guestInput.toIntOrNull()?:1
                                if(n>20)state.guestInput="20"
                            }
                        ){
                            Box(contentAlignment=Alignment.Center) {
                                Text(value,fontSize=24.sp,fontWeight=FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        SafeText("สูงสุด 20 คนต่อโต๊ะ",13,color=FoodyMuted,modifier=Modifier.padding(bottom=8.dp))
    }
}

@Composable
fun OccupiedTableScreen(state:FoodyStore) {
    val seat=state.table(state.currentTable)
    val related=state.ordersOf(seat.id)
    LazyColumn(
        modifier=Modifier.fillMaxSize().padding(horizontal=16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp),
        contentPadding=PaddingValues(vertical=16.dp)
    ){
        item {
            FoodyCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    SafeText("โต๊ะ ${seat.id}",24,true)
                    StatusPill("มีลูกค้า",false)
                }
                Spacer(Modifier.height(8.dp))
                SafeText("${seat.guests} คน",16,color=FoodyMuted)
            }
        }
        item { SectionTitle("รายการอาหาร","".let{Modifier}, "${related.size} ออเดอร์") }
        if(related.isEmpty()) item {
            FoodyCard {
                SafeText("ยังไม่มีออเดอร์",16,true)
                SafeText("กดสั่งอาหารเพิ่มเพื่อส่งเข้าครัว",13,color=FoodyMuted)
            }
        }
        items(related,key={it.id}) { order->
            FoodyCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    SafeText("ออเดอร์ #${order.id}",15,true)
                    StatusPill(when(order.status){"PENDING"->"รอครัว";"READY"->"พร้อมเสิร์ฟ";else->"เสิร์ฟแล้ว"},order.status!="PENDING")
                }
                Spacer(Modifier.height(10.dp))
                order.lines.forEach { line->
                    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)) {
                            SafeText("${line.name}  ×${line.qty}",15,true)
                            if(line.note.isNotBlank()) SafeText(line.note,12,color=FoodyMuted)
                        }
                        SafeText(price(line.price*line.qty),16,true)
                    }
                }
                if(order.status=="READY"){
                    Spacer(Modifier.height(12.dp))
                    Button(onClick={state.markServed(order.id)},modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=FoodyGreen)){
                        Icon(Icons.Default.Check,contentDescription=null)
                        Spacer(Modifier.width(6.dp))
                        Text("ยืนยันเสิร์ฟแล้ว",fontSize=15.sp)
                    }
                }
            }
        }
        item{
            FoodyCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    SafeText("ยอดรวม",19,true)
                    SafeText(price(state.total(seat.id)),23,true,FoodyOrangeDark)
                }
                if(!state.canCheckout(seat.id)){
                    Spacer(Modifier.height(6.dp))
                    SafeText("รอครัวทำอาหารให้ครบก่อนเช็กบิล",13,color=FoodyMuted)
                }
            }
        }
    }
}

@Composable
fun TableActions(state:FoodyStore) {
    Surface(color=Color.White,shadowElevation=5.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),
            horizontalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedButton(
                onClick={state.go("menu")},
                modifier=Modifier.weight(1f).height(52.dp),
                shape=RoundedCornerShape(14.dp)
            ){ Text("สั่งอาหารเพิ่ม",fontSize=14.sp,fontWeight=FontWeight.Bold) }
            Button(
                onClick={state.go("checkout")},
                modifier=Modifier.weight(1f).height(52.dp),
                shape=RoundedCornerShape(14.dp)
            ){Text("เช็กบิล",fontSize=15.sp,fontWeight=FontWeight.Bold)}
        }
    }
}

@Composable
fun MenuPickerScreen(state:FoodyStore) {
    var search by remember {mutableStateOf("")}
    var category by remember {mutableStateOf("ทั้งหมด")}
    val categories=listOf("ทั้งหมด","อาหารจานเดียว","เครื่องดื่ม","กับข้าว","ของหวาน")
    val filtered=state.foods.filter{it.available && (category=="ทั้งหมด" || it.category==category) && it.name.contains(search,ignoreCase=true)}
    Column(Modifier.fillMaxSize().padding(top=10.dp)) {
        Text("โต๊ะ ${state.currentTable}  ·  ${state.table(state.currentTable).guests} คน",
            fontSize=16.sp,fontWeight=FontWeight.Bold,color=FoodyOrangeDark,
            modifier=Modifier.padding(horizontal=16.dp,vertical=6.dp))
        OutlinedTextField(
            value=search,onValueChange={search=it},singleLine=true,
            placeholder={Text("ค้นหาเมนู")},
            leadingIcon={Icon(Icons.Default.Search,contentDescription=null)},
            modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),
            shape=RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            categories.forEach{c->
                FilterChip(selected=category==c,onClick={category=c},
                    label={Text(c,fontSize=14.sp)},shape=RoundedCornerShape(50))
            }
        }
        if(filtered.isEmpty()){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                SafeText("ไม่พบเมนูที่ค้นหา",16,color=FoodyMuted)
            }
        }else LazyVerticalGrid(
            columns=GridCells.Fixed(2),
            modifier=Modifier.fillMaxSize(),
            contentPadding=PaddingValues(14.dp),
            horizontalArrangement=Arrangement.spacedBy(12.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)
        ){
            items(filtered,key={it.id}){item->
                Card(
                    shape=RoundedCornerShape(17.dp),
                    colors=CardDefaults.cardColors(containerColor=Color.White),
                    elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
                ){
                    DishPhoto(item.image,Modifier.fillMaxWidth().height(118.dp))
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(item.name,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=2,
                            overflow=TextOverflow.Ellipsis,minLines=2,lineHeight=20.sp)
                        Spacer(Modifier.height(7.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                            verticalAlignment=Alignment.CenterVertically){
                            Text(price(item.price),fontSize=19.sp,color=FoodyOrangeDark,fontWeight=FontWeight.Bold)
                            FilledIconButton(onClick={state.add(item.id)},modifier=Modifier.size(40.dp)) {
                                Icon(Icons.Default.Add,"เพิ่ม ${item.name}")
                            }
                        }
                        if(state.cartQty(item.id)>0)
                            SafeText("เลือกแล้ว ${state.cartQty(item.id)}",12,color=FoodyGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuCartDock(state:FoodyStore) {
    Surface(shadowElevation=7.dp,color=Color.White) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            val count=state.cart.sumOf{it.qty}
            Column(Modifier.weight(1f)) {
                SafeText("${count} รายการ",14,true)
                SafeText(price(state.cartTotal()),21,true,FoodyOrangeDark)
            }
            Button(onClick={state.go("cart")},enabled=count>0,
                shape=RoundedCornerShape(14.dp),modifier=Modifier.height(52.dp)){
                Text("ดูรายการสั่ง",fontSize=15.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.width(3.dp))
                Icon(Icons.Default.ArrowForward,null)
            }
        }
    }
}

@Composable
fun CartScreen(state:FoodyStore) {
    val selected=state.cart.toList()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp),
        contentPadding=PaddingValues(vertical=16.dp)
    ){
        item { SafeText("โต๊ะ ${state.currentTable}  ·  ${state.table(state.currentTable).guests} คน",17,true,FoodyOrangeDark) }
        if(selected.isEmpty()) item {
            FoodyCard { SafeText("ยังไม่มีรายการสั่งอาหาร",16,true) }
        }
        items(selected,key={it.foodId}){cart->
            val food=state.foods.firstOrNull{it.id==cart.foodId}
            if(food!=null) FoodyCard {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    DishPhoto(food.image,Modifier.size(68.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        SafeText(food.name,16,true)
                        SafeText(price(food.price*cart.qty),16,true,FoodyOrangeDark)
                    }
                    IconButton(onClick={state.qty(cart.foodId,cart.qty-1)}) {
                        Icon(Icons.Default.Remove,"ลด")
                    }
                    SafeText("${cart.qty}",18,true)
                    IconButton(onClick={state.qty(cart.foodId,cart.qty+1)}) {
                        Icon(Icons.Default.Add,"เพิ่ม")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value=cart.note,onValueChange={state.note(cart.foodId,it)},
                    label={Text("หมายเหตุ (ถ้ามี)")},singleLine=true,
                    modifier=Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(12.dp)
                )
            }
        }
        item {
            FoodyCard{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    SafeText("รวม",18,true)
                    SafeText(price(state.cartTotal()),23,true,FoodyOrangeDark)
                }
            }
            if(state.message.isNotBlank()) Text(state.message,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(8.dp))
        }
    }
}

@Composable
fun CheckoutScreen(state:FoodyStore) {
    val orders=state.ordersOf(state.currentTable)
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        FoodyCard{
            SectionTitle("โต๊ะ ${state.currentTable}")
            Spacer(Modifier.height(6.dp))
            SafeText("${state.table(state.currentTable).guests} คน",15,color=FoodyMuted)
        }
        FoodyCard {
            SectionTitle("สรุปรายการ")
            Spacer(Modifier.height(12.dp))
            orders.flatMap{it.lines}.forEach{line->
                Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    SafeText("${line.name} ×${line.qty}",14,modifier=Modifier.weight(1f))
                    SafeText(price(line.price*line.qty),16,true)
                }
            }
            HorizontalDivider(Modifier.padding(vertical=14.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                SafeText("ยอดชำระ",20,true)
                SafeText(price(state.total(state.currentTable)),26,true,FoodyOrangeDark)
            }
        }
        FoodyCard {
            SectionTitle("การชำระเงิน")
            Spacer(Modifier.height(7.dp))
            SafeText("รับเงินสด (จำลอง)",15)
            Spacer(Modifier.height(8.dp))
            StatusPill(if(state.canCheckout(state.currentTable))"พร้อมปิดโต๊ะ" else "ยังมีอาหารรอครัว",state.canCheckout(state.currentTable))
        }
    }
}

@Composable
fun HistoryScreen(state:FoodyStore) {
    var todayOnly by remember{mutableStateOf(true)}
    val all=state.orders.filter{it.staffEmail==state.email}.sortedByDescending{it.time}
    val display=if(todayOnly) all.filter{state.timeIsToday(it.time)} else all
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=todayOnly,onClick={todayOnly=true},label={Text("วันนี้")})
            FilterChip(selected=!todayOnly,onClick={todayOnly=false},label={Text("ทั้งหมด")})
        }
        if(display.isEmpty()) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                SafeText("ยังไม่มีประวัติออเดอร์",16,color=FoodyMuted)
            }
        } else LazyColumn(
            Modifier.fillMaxSize().padding(horizontal=16.dp),
            contentPadding=PaddingValues(bottom=16.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            items(display,key={it.id}) { item->
                FoodyCard {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        SafeText("โต๊ะ ${item.tableId}  ·  #${item.id}",17,true)
                        StatusPill(when(item.status){"PENDING"->"รอครัว";"READY"->"พร้อมเสิร์ฟ";"SERVED"->"เสิร์ฟแล้ว";else->"เช็กบิลแล้ว"},item.status!="PENDING")
                    }
                    Spacer(Modifier.height(7.dp))
                    SafeText(item.lines.joinToString(", ") { "${it.name} ×${it.qty}" },14,color=FoodyMuted)
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        SafeText(timeText(item.time),13,color=FoodyMuted)
                        SafeText(price(item.lines.sumOf{it.price*it.qty}),18,true,FoodyOrangeDark)
                    }
                }
            }
        }
    }
}
