package click.alchemist.cook.compose

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun ToolbarTextField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	textStyle: TextStyle = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.onPrimary),
	focusedColor: Color = MaterialTheme.colors.onPrimary,
	unfocusedColor: Color = focusedColor.copy(alpha = 0.5f),
	placeholder: String = "",
) {
	val customTextSelectionColors = TextSelectionColors(
		handleColor = MaterialTheme.colors.secondary,
		backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.7f)
	)
	CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
		SimpleTextField(
			value = value,
			onValueChange = onValueChange,
			modifier = modifier,
			enabled = enabled,
			singleLine = true,
			textStyle = textStyle,
			focusedColor = focusedColor,
			unfocusedColor = unfocusedColor,
			placeholder = placeholder
		)
	}
}