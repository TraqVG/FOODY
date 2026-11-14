package com.foody.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
fun KitchenScreen(vm:FoodyViewModel){
    val pending=vm.snapshot.orders.filter{it.status=="PENDING"}.sortedBy{it.time}
    val ready=vm.snapshot.orders.count{it.status=="READY"}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            SimpleCounter("รอทำ",pending.size,false,Modifier.weight(1f))
            SimpleCounter("เสร็จแล้ว",ready,true,Modifier.weight(1f))
        }
        if(pending.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.Check,null,tint=Success,modifier=Modifier.size(45.dp))
                Spacer(Modifier.height(9.dp))
                HeaderText("ไม่มีออเดอร์ค้าง")
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),
            contentPadding=PaddingValues(bottom=25.dp)) {
            items(pending,key={it.id}) { order ->
                Pane {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Text("โต๊ะ ${order.tableId}",fontSize=25.sp,fontWeight=FontWeight.Bold)
                        Text("${timeLabel(order.time)} น.",fontSize=13.sp,color=SecondaryInk)
                    }
                    HorizontalDivider(Modifier.padding(vertical=11.dp))
                    order.lines.forEach { line ->
                        Text("•  ${line.name}  ×${line.qty}",fontSize=18.sp,fontWeight=FontWeight.Bold,
                            modifier=Modifier.padding(vertical=4.dp),lineHeight=24.sp)
                        if(line.note.isNotBlank()) Text("   ${line.note}",fontSize=14.sp,color=OrangeDark)
                    }
                    Spacer(Modifier.height(13.dp))
                    Button(onClick={vm.finishCooking(order.id)},enabled=!vm.busy,
                        colors=ButtonDefaults.buttonColors(containerColor=Success),shape=RoundedCornerShape(13.dp),
                        modifier=Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Default.Check,null)
                        Spacer(Modifier.width(8.dp))
                        Text("เสร็จแล้ว",fontWeight=FontWeight.Bold,fontSize=17.sp)
                    }
                }
            }
        }
    }
}
@Composable
private fun SimpleCounter(title:String,count:Int,positive:Boolean,modifier:Modifier=Modifier){
    Surface(modifier=modifier,color=if(positive)PaleGreen else PaleOrange,shape=RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            Text(title,color=if(positive)Success else OrangeDark,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text("$count",fontSize=21.sp,fontWeight=FontWeight.Bold,color=if(positive)Success else OrangeDark)
        }
    }
}

@Composable
fun ReportsScreen(vm:FoodyViewModel){
    val timeZone=TimeZone.getTimeZone("Asia/Bangkok")
    fun within(epoch:Long,precision:Int):Boolean{
        val a=Calendar.getInstance(timeZone).apply { timeInMillis=epoch }
        val b=Calendar.getInstance(timeZone)
        return a.get(Calendar.YEAR)==b.get(Calendar.YEAR) &&
            (precision>=2 || a.get(Calendar.MONTH)==b.get(Calendar.MONTH)) &&
            (precision>=1 || a.get(Calendar.DAY_OF_MONTH)==b.get(Calendar.DAY_OF_MONTH))
    }
    val today=vm.snapshot.sales.filter{within(it.time,0)}.sortedByDescending{it.time}
    val dayTotal=today.sumOf{it.amount}
    val monthTotal=vm.snapshot.sales.filter{within(it.time,1)}.sumOf{it.amount}
    val yearTotal=vm.snapshot.sales.filter{within(it.time,2)}.sumOf{it.amount}
    val hourly=(10..16).map{hour -> today.filter{Calendar.getInstance(timeZone).apply{timeInMillis=it.time}.get(Calendar.HOUR_OF_DAY)==hour}.sumOf{it.amount} }
    val scale=(hourly.maxOrNull()?:0).coerceAtLeast(1)
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(13.dp),contentPadding=PaddingValues(16.dp)) {
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SalesCount("วันนี้",dayTotal,Modifier.weight(1f))
            SalesCount("เดือนนี้",monthTotal,Modifier.weight(1f))
            SalesCount("ปีนี้",yearTotal,Modifier.weight(1f))
        } }
        item { Pane {
            HeaderText("ยอดขายวันนี้")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(164.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.Bottom) {
                hourly.forEachIndexed{index,value->
                    Column(Modifier.weight(1f).fillMaxHeight(),verticalArrangement=Arrangement.Bottom,horizontalAlignment=Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth(.78f).height((8+118*value/scale).dp)
                            .background(if(value==0)PaleOrange else Orange,RoundedCornerShape(topStart=6.dp,topEnd=6.dp)))
                        Spacer(Modifier.height(8.dp))
                        Text("${index+10}",fontSize=12.sp,color=SecondaryInk)
                    }
                }
            }
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            HeaderText("รายการขายวันนี้")
            Text("${today.size} บิล",fontSize=13.sp,color=SecondaryInk)
        } }
        if(today.isEmpty()) item {Pane {Text("ยังไม่มียอดขายวันนี้",fontWeight=FontWeight.SemiBold)} }
        items(today,key={it.id}) { sale -> Pane {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("${timeLabel(sale.time)}  ·  โต๊ะ ${sale.tableId}",fontSize=16.sp,fontWeight=FontWeight.Bold)
                PriceText(sale.amount)
            }
        } }
    }
}
@Composable
private fun SalesCount(label:String,value:Int,modifier:Modifier){
    Surface(color=if(label=="เดือนนี้")PaleOrange else PaleGreen,modifier=modifier,shape=RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(vertical=15.dp,horizontal=8.dp)) {
            Text(label,fontSize=12.sp,color=SecondaryInk,maxLines=1)
            Spacer(Modifier.height(7.dp))
            Text(money(value),fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}
@Composable
fun ManageMenuScreen(vm:FoodyViewModel,onEdit:(Int?)->Unit){
    var search by remember { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<FoodDto?>(null) }
    val foods=vm.snapshot.foods.filter{it.name.contains(search,ignoreCase=true)}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=search,onValueChange={search=it},singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("ค้นหาเมนู")},
                modifier=Modifier.weight(1f),shape=RoundedCornerShape(13.dp))
            Button(onClick={onEdit(null)},modifier=Modifier.height(54.dp),shape=RoundedCornerShape(13.dp)) {
                Icon(Icons.Default.Add,null);Text("เพิ่ม",fontWeight=FontWeight.Bold)
            }
        }
        if(foods.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {Text("ไม่พบเมนู",color=SecondaryInk)}
        else LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(11.dp),contentPadding=PaddingValues(bottom=18.dp)) {
            items(foods,key={it.id}){food-> Pane {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    DishImage(food.image,Modifier.size(74.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(food.name,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=2)
                        PriceText(food.price)
                        Text(food.category,fontSize=12.sp,color=SecondaryInk)
                    }
                    Switch(checked=food.available,enabled=!vm.busy,onCheckedChange={vm.toggleFood(food)})
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Tag(if(food.available)"พร้อมขาย" else "หยุดขาย",food.available)
                    Row {
                        IconButton(onClick={onEdit(food.id)}){Icon(Icons.Default.Edit,"แก้ไข ${food.name}",tint=SecondaryInk)}
                        IconButton(onClick={deleteCandidate=food}) {
                            Icon(Icons.Default.DeleteOutline,"ลบ ${food.name}",tint=MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } }
        }
    }
    deleteCandidate?.let{food->
        AlertDialog(onDismissRequest={deleteCandidate=null},title={Text("ลบเมนู?")},
            text={Text("${food.name} จะถูกนำออกจากหน้าขาย แต่ยังเก็บประวัติออเดอร์เดิม")},
            confirmButton={TextButton(onClick={vm.deleteFood(food.id);deleteCandidate=null}){Text("ลบ")}},
            dismissButton={TextButton(onClick={deleteCandidate=null}){Text("ยกเลิก")}})
    }
}
@Composable
fun MenuEditorScreen(vm:FoodyViewModel,editingId:Int?){
    val source=vm.snapshot.foods.firstOrNull{it.id==editingId}
    var name by remember(editingId){mutableStateOf(source?.name?:"")}
    var price by remember(editingId){mutableStateOf(source?.price?.toString()?:"")}
    var group by remember(editingId){mutableStateOf(source?.category?:"อาหารจานเดียว")}
    var expanded by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Pane {
            HeaderText(if(source==null)"เพิ่มเมนูใหม่" else "แก้ไขเมนู")
            Spacer(Modifier.height(15.dp))
            OutlinedTextField(value=name,onValueChange={name=it},modifier=Modifier.fillMaxWidth(),label={Text("ชื่ออาหาร")},shape=RoundedCornerShape(13.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value=price,onValueChange={price=it.filter(Char::isDigit).take(7)},
                modifier=Modifier.fillMaxWidth(),label={Text("ราคา (บาท)")},shape=RoundedCornerShape(13.dp))
            Spacer(Modifier.height(12.dp))
            Text("หมวดหมู่",fontWeight=FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text(group) }
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    listOf("อาหารจานเดียว","กับข้าว","เครื่องดื่ม","ของหวาน").forEach { c ->
                        DropdownMenuItem(text={Text(c)},onClick={group=c;expanded=false})
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick={vm.saveFood(editingId,name,price.toIntOrNull()?:0,group)},
                enabled=name.isNotBlank() && (price.toIntOrNull()?:0)>0 && !vm.busy,
                modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(14.dp)) {
                Text("บันทึกเมนู",fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}
@Composable
fun SettingsScreen(vm:FoodyViewModel){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Pane {
            Text(vm.role?.label.orEmpty(),fontWeight=FontWeight.Bold,color=Orange,fontSize=15.sp)
            Spacer(Modifier.height(7.dp))
            Text(vm.email,fontSize=18.sp,fontWeight=FontWeight.Bold)
        }
        Pane {
            HeaderText("ข้อมูลร้าน")
            Text("Foody • ร้านอาหารตามสั่ง",fontSize=15.sp)
            Text("ระบบภายในร้าน 6 โต๊ะ",color=SecondaryInk,fontSize=13.sp)
        }
        Pane {
            HeaderText("การแจ้งเตือน")
            Text("แจ้งเตือนออเดอร์ใหม่และอาหารพร้อมเมื่อเปิดแอป",fontSize=14.sp,color=SecondaryInk)
        }
        Pane {
            Text("ระบบบัญชี",fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("บัญชีกำหนดโดยร้าน ไม่มีหน้าสมัครสมาชิก",fontSize=13.sp,color=SecondaryInk)
        }
    }
}
