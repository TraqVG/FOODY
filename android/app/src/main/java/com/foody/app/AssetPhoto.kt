package com.foody.app

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun DishPhoto(imageId:Int,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val bitmap=remember(imageId) {
        runCatching {
            val source=context.assets.open("food-${imageId.coerceIn(1,8)}.txt").bufferedReader().use{it.readText()}
            val start=source.indexOf("data:image/webp;base64,")
            require(start>=0)
            val after=source.substring(start+"data:image/webp;base64,".length)
            val str=after.takeWhile{it.isLetterOrDigit() || it=='+' || it=='/' || it=='='}
            val bytes=Base64.decode(str,Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes,0,bytes.size).asImageBitmap()
        }.getOrNull()
    }
    if(bitmap!=null) Image(bitmap=bitmap,contentDescription="ภาพเมนูอาหาร",modifier=modifier,contentScale=ContentScale.Crop)
    else Box(modifier.background(FoodyPeach),contentAlignment=Alignment.Center) {
        Icon(Icons.Default.Restaurant,contentDescription=null,tint=FoodyOrange,modifier=Modifier.fillMaxSize(0.42f))
    }
}
