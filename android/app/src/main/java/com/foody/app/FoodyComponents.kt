package com.foody.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun price(value:Int):String="฿"+String.format(Locale.US,"%,d",value)
fun timeText(value:Long):String=SimpleDateFormat("HH:mm",Locale("th","TH")).apply{timeZone=TimeZone.getTimeZone("Asia/Bangkok")}.format(Date(value))

@Composable
fun FoodyTopBar(title:String,onBack:(()->Unit)?=null,onProfile:(()->Unit)?=null) {
    Surface(shadowElevation=3.dp,color=FoodyOrange) {
        Row(
            modifier=Modifier.fillMaxWidth().height(76.dp)
                .background(Brush.horizontalGradient(listOf(FoodyOrangeDark,FoodyOrange)))
                .padding(horizontal=16.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            if(onBack!=null){
                IconButton(onClick=onBack){
                    Icon(Icons.Default.ArrowBack,"กลับ",tint=Color.White)
                }
            }
            Column(Modifier.weight(1f)) {
                if(onBack==null) Text("Foody",color=Color.White.copy(alpha=.88f),fontSize=14.sp,fontWeight=FontWeight.Bold)
                Text(title,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            if(onProfile!=null) {
                IconButton(onClick=onProfile) {
                    Icon(Icons.Default.AccountCircle,"โปรไฟล์และตั้งค่า",tint=Color.White,modifier=Modifier.size(29.dp))
                }
            }
        }
    }
}

@Composable
fun FoodyBottomNav(labels:List<Pair<String,String>>,selected:String,onNavigate:(String)->Unit) {
    NavigationBar(containerColor=Color.White,tonalElevation=8.dp) {
        labels.forEach { (label,route)->
            val icon=when(route){
                "tables"->Icons.Default.TableRestaurant
                "history"->Icons.Default.History
                "reports"->Icons.Default.Assessment
                else->Icons.Default.RestaurantMenu
            }
            NavigationBarItem(
                selected=selected==route,
                onClick={onNavigate(route)},
                icon={Icon(icon,contentDescription=null)},
                label={Text(label,fontSize=14.sp,fontWeight=if(selected==route)FontWeight.Bold else FontWeight.Normal)},
                colors=NavigationBarItemDefaults.colors(
                    selectedIconColor=FoodyOrange,
                    selectedTextColor=FoodyOrange,
                    indicatorColor=FoodyPeach,
                    unselectedIconColor=FoodyMuted,
                    unselectedTextColor=FoodyMuted
                )
            )
        }
    }
}

@Composable
fun BottomAction(label:String,onClick:()->Unit,enabled:Boolean=true,danger:Boolean=false){
    Surface(color=Color.White,shadowElevation=6.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=16.dp, vertical=10.dp)){
            Button(
                onClick=onClick,enabled=enabled,
                modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                colors=ButtonDefaults.buttonColors(
                    containerColor=if(danger) Color(0xFFBE3030) else FoodyOrange
                ),shape=RoundedCornerShape(16.dp)
            ){
                Text(label,fontSize=17.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FoodyCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Card(
        modifier=modifier,
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=Color.White),
        elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
    ) { Column(Modifier.fillMaxWidth().padding(16.dp),content=content) }
}

@Composable
fun SectionTitle(text:String,modifier:Modifier=Modifier,detail:String?=null){
    Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Text(text,fontSize=18.sp,fontWeight=FontWeight.Bold,color=FoodyText)
        if(detail!=null) Text(detail,fontSize=13.sp,color=FoodyMuted)
    }
}

@Composable
fun StatusPill(label:String,positive:Boolean,modifier:Modifier=Modifier){
    Surface(modifier=modifier,color=if(positive)FoodyGreenPale else FoodyPeach,shape=RoundedCornerShape(50)) {
        Text(label,color=if(positive)FoodyGreen else FoodyOrangeDark,fontSize=13.sp,
            fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp),maxLines=1)
    }
}

@Composable
fun SafeText(text:String,size:Int=16,bold:Boolean=false,color:Color=FoodyText,modifier:Modifier=Modifier,maxLines:Int=2) {
    Text(text,color=color,fontSize=size.sp,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal,
        modifier=modifier,maxLines=maxLines,overflow=TextOverflow.Ellipsis,lineHeight=(size+5).sp)
}

@Composable
fun LoginScreen(state:FoodyStore){
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    Column(
        modifier=Modifier.fillMaxSize().background(FoodyBackground).verticalScroll(rememberScrollState()),
        horizontalAlignment=Alignment.CenterHorizontally
    ){
        Box(Modifier.fillMaxWidth().height(152.dp)
            .background(Brush.linearGradient(listOf(FoodyOrangeDark,FoodyOrange))).padding(24.dp),
            contentAlignment=Alignment.BottomStart){
            Column {
                Text("Foody",color=Color.White,fontSize=36.sp,fontWeight=FontWeight.ExtraBold)
                Text("ระบบจัดการออเดอร์ร้านอาหาร",color=Color.White.copy(alpha=.90f),fontSize=15.sp)
            }
        }
        Column(Modifier.fillMaxWidth().widthIn(max=440.dp).padding(22.dp)){
            Spacer(Modifier.height(12.dp))
            Text("เข้าสู่ระบบ",fontSize=27.sp,fontWeight=FontWeight.Bold,color=FoodyText)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value=user,onValueChange={user=it},
                label={Text("อีเมลร้าน")},
                singleLine=true,
                modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value=pass,onValueChange={pass=it},
                label={Text("รหัสผ่าน")},
                singleLine=true,
                visualTransformation=if(showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon={TextButton(onClick={showPassword=!showPassword}){Text(if(showPassword)"ซ่อน" else "แสดง")}},
                modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(14.dp)
            )
            if(state.message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(state.message,color=MaterialTheme.colorScheme.error,fontSize=14.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick={state.login(user,pass)},modifier=Modifier.fillMaxWidth().height(54.dp),
                shape=RoundedCornerShape(15.dp)){
                Text("เข้าสู่ระบบ",fontSize=18.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(25.dp))
            FoodyCard {
                SectionTitle("บัญชีทดลอง")
                Spacer(Modifier.height(10.dp))
                listOf(
                    "staff@foody.shop" to "พนักงานเสิร์ฟ",
                    "kitchen@foody.shop" to "พนักงานครัว",
                    "owner@foody.shop" to "เจ้าของร้าน"
                ).forEach{(account,label)->
                    Row(
                        Modifier.fillMaxWidth().clickable{user=account;pass="foody123";state.message=""}
                            .padding(vertical=11.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.SpaceBetween
                    ){
                        Column(Modifier.weight(1f)){
                            SafeText(label,15,true)
                            SafeText(account,13,color=FoodyMuted)
                        }
                        Text("เลือก",color=FoodyOrange,fontSize=14.sp,fontWeight=FontWeight.Bold)
                    }
                    if(account!="owner@foody.shop") HorizontalDivider(color=FoodyBackground)
                }
                Text("ใช้รหัสผ่าน: foody123",fontSize=12.sp,color=FoodyMuted)
            }
        }
    }
}

@Composable
fun SettingsScreen(state:FoodyStore) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        FoodyCard {
            Text(state.role?.label ?: "",color=FoodyOrange,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(state.email,fontSize=18.sp,fontWeight=FontWeight.Bold)
        }
        FoodyCard {
            SectionTitle("ข้อมูลร้าน")
            Spacer(Modifier.height(8.dp))
            SafeText("Foody • ร้านอาหารตามสั่ง",15)
            SafeText("ระบบภายในร้าน • 6 โต๊ะ",13,color=FoodyMuted)
        }
        FoodyCard {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                Column {
                    SafeText("การแจ้งเตือน",16,true)
                    SafeText("แจ้งรายการอาหารพร้อมเสิร์ฟ",12,color=FoodyMuted)
                }
                Switch(checked=state.notifications,onCheckedChange=state::changeNotifications)
            }
        }
        FoodyCard {
            SafeText("ระบบบัญชี",16,true)
            Spacer(Modifier.height(6.dp))
            SafeText("บัญชีถูกกำหนดล่วงหน้า ไม่มีการสมัครสมาชิกในแอป",13,color=FoodyMuted)
        }
    }
}
