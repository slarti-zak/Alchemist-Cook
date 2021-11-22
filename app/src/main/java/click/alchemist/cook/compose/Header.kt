package click.alchemist.cook.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Header(name: String, modifier: Modifier = Modifier) {
	Row(
		modifier = modifier.then(
			Modifier
				.fillMaxWidth()
				.padding(8.dp)
		),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			Modifier
				.weight(1f)
				.height(1.dp)
				.background(color = MaterialTheme.colors.secondary)
		)

		Text(
			text = name,
			modifier = Modifier.padding(horizontal = 8.dp),
			style = textHeaderStyle()
		)

		Box(
			Modifier
				.weight(1f)
				.height(1.dp)
				.background(color = MaterialTheme.colors.secondary)
		)
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		Header("Todo")
	}
}
