package click.alchemist.cook.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
				.background(color = MaterialTheme.colorScheme.secondary)
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
				.background(color = MaterialTheme.colorScheme.secondary)
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
