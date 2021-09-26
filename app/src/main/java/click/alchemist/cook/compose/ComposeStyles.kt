package click.alchemist.cook.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import click.alchemist.cook.R

fun textStyle() = TextStyle()

fun textTitleStyle() = textStyle().copy(fontSize = 16.sp)

fun textLargeTitleStyle() = textTitleStyle().copy(fontSize = 20.sp)

@Composable
fun textSubtitleStyle() = textStyle().copy(fontSize = 12.sp, color = colorResource(R.color.COLOR1_0))

@Composable
fun textLargeSubtitleStyle() = textSubtitleStyle().copy(fontSize = 16.sp)

fun textIngredientStyle() = textStyle().copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)

fun textIngredientStyleDisabled() = textIngredientStyle().copy(textDecoration = TextDecoration.LineThrough)

fun textIngredientAmountUnitStyle() = textStyle().copy(fontSize = 14.sp)

@Composable
fun textHeaderStyle() = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.COLOR0_1))

@Composable
fun AppTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colors = appColors(),
		typography = Typography(subtitle2 = textLargeSubtitleStyle()),
		content = content,
		shapes = Shapes(medium = RoundedCornerShape(8.dp))
	)
}