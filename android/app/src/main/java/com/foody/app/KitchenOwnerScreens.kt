package com.foody.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.TimeZone

@Composable
fun KitchenScreen(state:FoodyStore) {
    val pending=state.orders.filter{it.status=="PENDING"}.sortedBy{it.time}
    val ready=state.orders.count{it.status=="READY"}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            KitchenCount("รอทำ",pending.size,false,Modifier.weight(1f))
            KitchenCount("เสร็จแล้ว",ready,true,Modifier.weight(1f))
        }
        if(pending.isEmpty()) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check,null,tint=FoodyGreen,modifier=Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    SafeText("ไม่มีออเดอร์ค้าง",20,true)
                }
            }
        }else LazyColumn(
            modifier=Modifier.fillMaxSize().padding(horizontal=16.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(bottom=22.dp)
        ){
            items(pending,key={it.id}){order->
                FoodyCard {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        SafeText("โต๊ะ ${order.tableId}",23,true)
                        SafeText("${timeText(order.time)} น.",13,color=FoodyMuted)
                    }
                    HorizontalDivider(Modifier.padding(vertical=12.dp))
                    order.lines.forEach{line->
                        SafeText("•  ${line.name} ×${line.qty}",18,true,modifier=Modifier.padding(vertical=4.dp),maxLines=3)
                        if(line.note.isNotBlank()) SafeText("   ${line.note}",14,color=FoodyOrangeDark)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick={state.markReady(order.id)},
                        colors=ButtonDefaults.buttonColors(containerColor=FoodyGreen),
                        modifier=Modifier.fillMaxWidth().height(50.dp),
                        shape=RoundedCornerShape(13.dp)){
                        Icon(Icons.Default.Check,contentDescription=null)
                        Spacer(Modifier.width(6.dp))
                        Text("เสร็จแล้ว",fontSize=17.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KitchenCount(label:String,value:Int,positive:Boolean,modifier:Modifier=Modifier) {
    Surface(modifier=modifier,color=if(positive)FoodyGreenPale else FoodyPeach,shape=RoundedCornerShape(13.dp)){
        Row(Modifier.padding(13.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
            SafeText(label,14,true,if(positive)FoodyGreen else FoodyOrangeDark)
            Spacer(Modifier.width(8.dp))
            SafeText("$value",20,true,if(positive)FoodyGreen else FoodyOrangeDark)
        }
    }
}

@Composable
fun ReportsScreen(state:FoodyStore) {
    val recent=state.sales.filter{state.timeIsToday(it.time)}.sortedByDescending{it.time}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ReportNumber("วันนี้",state.salesTotal(0),Modifier.weight(1f))
                ReportNumber("เดือนนี้",state.salesTotal(1),Modifier.weight(1f))
                ReportNumber("ปีนี้",state.salesTotal(2),Modifier.weight(1f))
            }
        }
        item {
            FoodyCard {
                SectionTitle("ยอดขายวันนี้")
                Spacer(Modifier.height(16.dp))
                val tz=TimeZone.getTimeZone("Asia/Bangkok")
                val values=(10..16).map{hour->
                    state.sales.filter{state.timeIsToday(it.time) && Calendar.getInstance(tz).apply{timeInMillis=it.time}.get(Calendar.HOUR_OF_DAY)==hour}.sumOf{it.amount}
                }
                val max=(values.maxOrNull()?:0).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().height(160.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),
                    verticalAlignment=Alignment.Bottom){
                    values.forEachIndexed{index,value->
                        Column(Modifier.weight(1f).fillMaxHeight(),verticalArrangement=Arrangement.Bottom,
                            horizontalAlignment=Alignment.CenterHorizontally){
                            Box(Modifier.fillMaxWidth(.73f).height((9+115*value/max).dp)
                                .background(if(value==0)FoodyPeach else FoodyOrange,RoundedCornerShape(topStart=7.dp,topEnd=7.dp)))
                            Spacer(Modifier.height(6.dp))
                            SafeText("${index+10}",12,color=FoodyMuted)
                        }
                    }
                }
            }
        }
        item { SectionTitle("รายการขายวันนี้",detail="${recent.size} บิล") }
        if(recent.isEmpty()) item {
            FoodyCard {
                SafeText("ยังไม่มียอดขายวันนี้",16,true)
                SafeText("เมื่อเช็กบิล รายการจะปรากฏที่นี่",13,color=FoodyMuted)
            }
        }
        items(recent,key={it.id}) { sale->
            FoodyCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    SafeText("${timeText(sale.time)}  ·  โต๊ะ ${sale.tableId}",16,true)
                    SafeText(price(sale.amount),20,true,FoodyOrangeDark)
                }
            }
        }
    }
}

@Composable
private fun ReportNumber(title:String,total:Int,modifier:Modifier){
    Surface(modifier=modifier,color=if(title=="เดือนนี้")FoodyPeach else FoodyGreenPale,shape=RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(vertical=15.dp,horizontal=8.dp)) {
            SafeText(title,12,true,color=FoodyMuted,maxLines=1)
            Spacer(Modifier.height(7.dp))
            Text(price(total),fontSize=15.sp,fontWeight=FontWeight.Bold,
                color=FoodyText,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ManageScreen(state:FoodyStore,onEdit:(Int?)->Unit) {
    var search by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<Food?>(null)}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(
                value=search,onValueChange={search=it},singleLine=true,
                modifier=Modifier.weight(1f),
                placeholder={Text("ค้นหาเมนู")},
                leadingIcon={Icon(Icons.Default.Search,contentDescription=null)},
                shape=RoundedCornerShape(14.dp)
            )
            Button(onClick={onEdit(null)},modifier=Modifier.height(55.dp),shape=RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add,contentDescription=null)
                Text("เพิ่ม",fontWeight=FontWeight.Bold)
            }
        }
        val list=state.foods.filter{it.name.contains(search,ignoreCase=true)}
        if(list.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            SafeText("ไม่พบเมนูที่ค้นหา",16,color=FoodyMuted)
        } else LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp),
            contentPadding=PaddingValues(bottom=18.dp)){
            items(list,key={it.id}){food->
                FoodyCard {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        DishPhoto(food.image,Modifier.size(72.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            SafeText(food.name,16,true)
                            SafeText(price(food.price),19,true,FoodyOrangeDark)
                            SafeText(food.category,12,color=FoodyMuted)
                        }
                        Switch(checked=food.available,onCheckedChange={state.toggleMenu(food.id)})
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        StatusPill(if(food.available)"พร้อมขาย" else "หยุดขาย",food.available)
                        Row {
                            IconButton(onClick={onEdit(food.id)}){Icon(Icons.Default.Edit,"แก้ไข ${food.name}",tint=FoodyMuted)}
                            IconButton(onClick={pendingDelete=food}) {
                                Icon(Icons.Default.DeleteOutline,"ลบ ${food.name}",tint=MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
    val candidate=pendingDelete
    if(candidate!=null){
        AlertDialog(
            onDismissRequest={pendingDelete=null},
            title={Text("ลบเมนู?")},
            text={Text("ลบ ${candidate.name} ออกจากรายการขาย (ไม่ลบประวัติออเดอร์)")},
            confirmButton={TextButton(onClick={state.deleteMenu(candidate.id);pendingDelete=null}) {
                Text("ลบ",color=MaterialTheme.colorScheme.error)
            }},
            dismissButton={TextButton(onClick={pendingDelete=null}){Text("ยกเลิก")}}
        )
    }
}

@Composable
fun MenuEditorScreen(state:FoodyStore,foodId:Int?,onSaved:()->Unit) {
    val existing=state.foods.firstOrNull{it.id==foodId}
    var name by remember(foodId){mutableStateOf(existing?.name ?: "")}
    var amount by remember(foodId){mutableStateOf(existing?.price?.toString() ?: "")}
    var category by remember(foodId){mutableStateOf(existing?.category ?: "อาหารจานเดียว")}
    var expanded by remember{mutableStateOf(false)}
    val groups=listOf("อาหารจานเดียว","กับข้าว","เครื่องดื่ม","ของหวาน")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        FoodyCard {
            SectionTitle(if(existing==null)"เพิ่มเมนูใหม่" else "แก้ไขเมนู")
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value=name,onValueChange={name=it},label={Text("ชื่ออาหาร")},
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value=amount,onValueChange={amount=it.filter{c->c.isDigit()}.take(6)},
                label={Text("ราคา (บาท)")},modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            SafeText("หมวดหมู่",15,true)
            Box {
                OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text(category)}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
                    groups.forEach{g-> DropdownMenuItem(text={Text(g)},onClick={category=g;expanded=false})}
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick={state.saveMenu(foodId,name,amount.toIntOrNull()?:0,category)},
                enabled=name.trim().isNotEmpty() && (amount.toIntOrNull()?:0)>0,
                modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){
                Text("บันทึกเมนู",fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
        }
        FoodyCard {
            SafeText("ข้อมูลเมนูจะจัดเก็บใน Room ของเครื่องนี้",13,color=FoodyMuted)
            SafeText("รายการอาหารที่สั่งไปแล้วจะเก็บชื่อและราคาเดิม",13,color=FoodyMuted)
        }
    }
}
