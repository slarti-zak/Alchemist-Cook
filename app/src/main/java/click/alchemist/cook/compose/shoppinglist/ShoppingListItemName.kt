package click.alchemist.cook.compose.shoppinglist

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.textIngredientStyle

@Composable
fun ShoppingListItemName(name: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
	var jump by remember { mutableStateOf(false) }
	val offset by animateIntOffsetAsState(
		targetValue = if (jump) IntOffset(0, -50) else IntOffset.Zero,
		finishedListener = { jump = false })

	Text(
		text = name,
		style = textIngredientStyle(),
		textAlign = TextAlign.Start,
		modifier = modifier
			.fillMaxWidth()
			.clickable(onClick = {
				if (onClick != null) {
					onClick()
					jump = true
				}
			})
			.offset { offset }
			.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
	)
}

@Preview
@Composable
private fun Preview() {
	MaterialTheme {
		ShoppingListItemName("Milk")
	}
}
