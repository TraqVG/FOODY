package com.foody.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Orange = Color(0xFFF76520)
val OrangeDark = Color(0xFFE65314)
val PaleOrange = Color(0xFFFFF0E6)
val Ink = Color(0xFF242329)
val SecondaryInk = Color(0xFF62616A)
val Canvas = Color(0xFFFAF9F7)
val Success = Color(0xFF12844C)
val PaleGreen = Color(0xFFE3F6E9)

private val FoodyColors = lightColorScheme(
    primary=Orange, onPrimary=Color.White,
    primaryContainer=PaleOrange, onPrimaryContainer=OrangeDark,
    secondary=Success, onSecondary=Color.White,
    background=Canvas, onBackground=Ink,
    surface=Color.White, onSurface=Ink,
    error=Color(0xFFCB3535)
)

@Composable
fun FoodyTheme(content: @Composable ()->Unit) {
    MaterialTheme(colorScheme=FoodyColors, typography=Typography(), content=content)
}
