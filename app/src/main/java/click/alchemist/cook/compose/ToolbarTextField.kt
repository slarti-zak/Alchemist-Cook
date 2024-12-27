package click.alchemist.cook.compose

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
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
	textStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(color = TopAppBarDefaults.topAppBarColors().titleContentColor),
	focusedColor: Color = TopAppBarDefaults.topAppBarColors().titleContentColor,
	unfocusedColor: Color = focusedColor.copy(alpha = 0.5f),
	placeholder: String = "",
) {
	val customTextSelectionColors = TextSelectionColors(
		handleColor = MaterialTheme.colorScheme.secondary,
		backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
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