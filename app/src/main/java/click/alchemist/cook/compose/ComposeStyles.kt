package click.alchemist.cook.compose

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
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

/**
 * Requires the hosting [Activity] to be edge-to-edge (see [androidx.activity.enableEdgeToEdge]) with
 * a transparent status bar style — [Window.statusBarColor] has no non-deprecated replacement, so
 * instead of painting the system-drawn bar we let it stay transparent and paint our own scrim behind
 * it from within the composition.
 */
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
			WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
		}
	}

	MaterialTheme(
		colorScheme = colorScheme,
		typography = Typography(),
		shapes = Shapes(medium = RoundedCornerShape(8.dp))
	) {
		Box(Modifier.fillMaxSize()) {
			content()
			Box(
				Modifier
					.fillMaxWidth()
					.windowInsetsTopHeight(WindowInsets.statusBars)
					.background(colorScheme.primary)
			)
		}
	}
}