package click.alchemist.cook.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle

@Composable
fun SimpleTextField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	singleLine: Boolean = false,
	textStyle: TextStyle = TextStyle.Default,
	focusedColor: Color = MaterialTheme.colors.primary,
	unfocusedColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
	placeholder: String = "",
) {
	var focused by remember { mutableStateOf(false) }
	val color by animateColorAsState(targetValue = if (focused) focusedColor else unfocusedColor)

	BasicTextField(
		value,
		onValueChange = onValueChange,
		modifier.then(Modifier.onFocusChanged { focused = it.isFocused }),
		enabled = enabled,
		singleLine = singleLine,
		textStyle = textStyle,
		cursorBrush = SolidColor(focusedColor),
		decorationBox = { innerTextField ->
			Column(Modifier.width(IntrinsicSize.Min)) {
				Box() {
					if (placeholder.isNotBlank() && value.isEmpty()) {
						CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
							Text(text = placeholder)
						}
					}
					innerTextField()
				}
				Divider(Modifier.fillMaxWidth(), color = color)
			}
		}
	)
}