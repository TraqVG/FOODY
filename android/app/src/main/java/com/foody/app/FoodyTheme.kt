package com.foody.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FoodyOrange=Color(0xFFF56623)
val FoodyOrangeDark=Color(0xFFD65216)
val FoodyBackground=Color(0xFFFAF8F6)
val FoodyText=Color(0xFF27252A)
val FoodyMuted=Color(0xFF716C70)
val FoodyGreen=Color(0xFF167548)
val FoodyGreenPale=Color(0xFFE5F7EC)
val FoodyPeach=Color(0xFFFFEEE3)

private val Colors=lightColorScheme(
    primary=FoodyOrange, onPrimary=Color.White,
    primaryContainer=FoodyPeach,onPrimaryContainer=FoodyOrangeDark,
    secondary=FoodyGreen,onSecondary=Color.White,
    background=FoodyBackground,onBackground=FoodyText,
    surface=Color.White,onSurface=FoodyText,
    error=Color(0xFFC83732)
)
@Composable
fun FoodyTheme(content: @Composable ()->Unit) {
    MaterialTheme(colorScheme=Colors,typography=Typography(),content=content)
}
