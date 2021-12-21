package click.alchemist.cook.compose

import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val textDefault = Color(0xFF464646)
val lightIcon = Color(0xFF8C8C8C)

val COLOR0_0_0 = Color(0xFFF9FBFA)
val COLOR0_0 = Color(0xFF75af96)
val COLOR0_1 = Color(0xFF499272)
val COLOR0_2 = Color(0xFF277553)
val COLOR0_3 = Color(0xFF0f5738)
val COLOR0_4 = Color(0xFF003a21)

val COLOR1_0 = Color(0xFF718ea4)
val COLOR1_1 = Color(0xFF496d89)
val COLOR1_2 = Color(0xFF1B659C)
val COLOR1_3 = Color(0xFF29506d)
val COLOR1_4 = Color(0xFF123652)

val COLOR2_0 = Color(0xFFFFDBAA)
val COLOR2_1 = Color(0xFFd4a76a)
val COLOR2_2 = Color(0xFFaa5939)
val COLOR2_3 = Color(0xFF805215)
val COLOR2_4 = Color(0xFF553100)

val COLOR3_2 = Color(0xFFaa7939)

val cookingGraphNeutral= Color(0xFFFFFFFF)
val cookingGraphProcessable= Color(0xFFFFFCCE)
val cookingGraphNotProcessable= Color(0xFFE8E8E8)
val cookingGraphFinished= Color(0xFFDCFBEE)

fun appColors() = lightColors(
    primary = COLOR0_1,
    primaryVariant = COLOR0_2,
    onPrimary = Color.White,
    secondary = COLOR1_0,
    onSecondary = Color.White,
    secondaryVariant = COLOR1_1,
    onSurface = textDefault,
    onBackground = textDefault,
)