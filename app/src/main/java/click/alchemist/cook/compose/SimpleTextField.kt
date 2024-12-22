package click.alchemist.cook.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// From TextField.kt
internal fun Modifier.drawIndicatorLine(lineWidth: Dp, color: Color): Modifier {
	return drawBehind {
		val strokeWidth = lineWidth.value * density
		val y = size.height - strokeWidth / 2
		drawLine(
			color,
			Offset(0f, y),
			Offset(size.width, y),
			strokeWidth
		)
	}
}

@Composable
fun SimpleTextField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	singleLine: Boolean = false,
	textStyle: TextStyle = TextStyle.Default,
	focusedColor: Color = MaterialTheme.colorScheme.primary,
	unfocusedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
	placeholder: String = "",
) {
	var focused by remember { mutableStateOf(false) }
	val color by animateColorAsState(targetValue = if (focused) focusedColor else unfocusedColor)

	BasicTextField(
		value,
		onValueChange = onValueChange,
		modifier
			.onFocusChanged { focused = it.isFocused }
			.drawIndicatorLine(1.dp, color),
		enabled = enabled,
		singleLine = singleLine,
		textStyle = textStyle,
		cursorBrush = SolidColor(focusedColor),
		decorationBox = { innerTextField ->
			Box {
				if (placeholder.isNotBlank() && value.isEmpty()) {
					CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
						Text(text = placeholder)
					}
				}
				innerTextField()
			}
		}
	)
}