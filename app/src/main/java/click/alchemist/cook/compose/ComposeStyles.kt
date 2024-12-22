package click.alchemist.cook.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun textStyle() = TextStyle()

fun textTitleStyle() = textStyle().copy(fontSize = 16.sp)

fun textLargeTitleStyle() = textTitleStyle().copy(fontSize = 20.sp)

fun textSubtitleStyle() = textStyle().copy(fontSize = 12.sp, color = COLOR1_0)

fun textLargeSubtitleStyle() = textSubtitleStyle().copy(fontSize = 16.sp)

fun textIngredientStyle() = textStyle().copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)

fun textIngredientStyleDisabled() = textIngredientStyle().copy(textDecoration = TextDecoration.LineThrough)

fun textIngredientAmountUnitStyle() = textStyle().copy(fontSize = 14.sp)

fun textHeaderStyle() = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = COLOR0_1)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = appColors(),
		typography = Typography(),
		content = content,
		shapes = Shapes(medium = RoundedCornerShape(8.dp))
	)
}