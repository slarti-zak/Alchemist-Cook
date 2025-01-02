package click.alchemist.cook.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

fun textStyle() = TextStyle()

fun textSubtitleStyle() = textStyle().copy(fontSize = 12.sp, color = COLOR1_0)

fun textIngredientStyle() = textStyle().copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)

@Composable
fun textIngredientStyleDisabled() = textIngredientStyle().copy(
	textDecoration = TextDecoration.LineThrough,
	color = MaterialTheme.colorScheme.outline
)

fun textIngredientAmountUnitStyle() = textStyle().copy(fontSize = 14.sp)

@Composable
fun textHeaderStyle() = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
	val colorScheme = if (darkTheme) {
		darkColorScheme
	} else {
		lightColorScheme
	}


	val view = LocalView.current
	if (!view.isInEditMode) {
		SideEffect {
			val window = (view.context as Activity).window
			window.statusBarColor = colorScheme.primary.toArgb()
			WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
		}
	}

	MaterialTheme(
		colorScheme = if (darkTheme) {
			darkColorScheme
		} else {
			lightColorScheme
		},
		typography = Typography(),
		content = content,
		shapes = Shapes(medium = RoundedCornerShape(8.dp))
	)
}