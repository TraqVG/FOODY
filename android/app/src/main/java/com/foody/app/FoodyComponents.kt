package com.foody.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun money(amount:Int):String = "฿" + String.format(Locale.US,"%,d", amount)
fun timeLabel(epoch:Long):String = SimpleDateFormat("HH:mm",Locale("th","TH"))
    .apply { timeZone = TimeZone.getTimeZone("Asia/Bangkok") }.format(Date(epoch))

@Composable
fun FoodyHeader(title:String,onBack:(()->Unit)?=null,onProfile:(()->Unit)?=null) {
    Surface(shadowElevation=2.dp) {
        Row(
            Modifier.fillMaxWidth().height(78.dp)
                .background(Brush.horizontalGradient(listOf(OrangeDark,Orange)))
                .padding(horizontal=14.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            if(onBack!=null) IconButton(onClick=onBack) {
                Icon(Icons.Default.ArrowBack,"กลับ",tint=Color.White)
            }
            Column(Modifier.weight(1f)) {
                if(onBack==null) Text("Foody",fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White.copy(alpha=.85f))
                Text(title,fontSize=23.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            if(onProfile!=null) IconButton(onClick=onProfile) {
                Icon(Icons.Default.AccountCircle,"โปรไฟล์และตั้งค่า",tint=Color.White,modifier=Modifier.size(29.dp))
            }
        }
    }
}

@Composable
fun FoodyNavigation(tabs:List<Pair<String,String>>,selected:String,onTap:(String)->Unit){
    NavigationBar(containerColor=Color.White,tonalElevation=6.dp) {
        tabs.forEach { (name,route) ->
            val icon = when(route) {
                "tables" -> Icons.Default.TableRestaurant
                "history" -> Icons.Default.History
                "reports" -> Icons.Default.Assessment
                else -> Icons.Default.RestaurantMenu
            }
            NavigationBarItem(
                selected=selected==route,
                onClick={onTap(route)},
                icon={Icon(icon,contentDescription=null)},
                label={Text(name,fontSize=14.sp)},
                colors=NavigationBarItemDefaults.colors(
                    selectedIconColor=Orange,selectedTextColor=Orange,
                    unselectedIconColor=SecondaryInk,unselectedTextColor=SecondaryInk,
                    indicatorColor=PaleOrange
                )
            )
        }
    }
}

@Composable
fun Pane(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Card(
        modifier=modifier,
        shape=RoundedCornerShape(17.dp),
        elevation=CardDefaults.cardElevation(defaultElevation=2.dp),
        colors=CardDefaults.cardColors(containerColor=Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp),content=content)
    }
}
@Composable
fun HeaderText(text:String,modifier:Modifier=Modifier) {
    Text(text,fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=modifier)
}
@Composable
fun Tag(text:String,positive:Boolean,modifier:Modifier=Modifier){
    Surface(modifier=modifier,shape=RoundedCornerShape(50),color=if(positive)PaleGreen else PaleOrange){
        Text(text,fontSize=13.sp,fontWeight=FontWeight.SemiBold,
            color=if(positive)Success else OrangeDark,
            modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp))
    }
}
@Composable
fun BottomButton(label:String,enabled:Boolean=true,danger:Boolean=false,onClick:()->Unit){
    Surface(color=Color.White,shadowElevation=5.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=16.dp,vertical=9.dp)) {
            Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=if(danger)Color(0xFFBF3535) else Orange)) {
                Text(label,fontSize=17.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}
@Composable
fun DishImage(image:Int,modifier:Modifier=Modifier) {
    val resource=when(image) {
        1->R.drawable.dish_01;2->R.drawable.dish_02;3->R.drawable.dish_03;4->R.drawable.dish_04
        5->R.drawable.dish_05;6->R.drawable.dish_06;7->R.drawable.dish_07;else->R.drawable.dish_08
    }
    Image(painterResource(resource),contentDescription="ภาพเมนูอาหาร",modifier=modifier,contentScale=ContentScale.Crop)
}
@Composable
fun PriceText(value:Int,modifier:Modifier=Modifier) {
    Text(money(value),fontSize=20.sp,fontWeight=FontWeight.Bold,color=OrangeDark,modifier=modifier)
}
