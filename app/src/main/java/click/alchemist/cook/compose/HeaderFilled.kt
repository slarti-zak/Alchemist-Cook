package click.alchemist.cook.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HeaderFilled(name: String, modifier: Modifier = Modifier) {
	Surface(color = MaterialTheme.colors.primary, modifier = modifier) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp),
			contentAlignment = Alignment.Center
		) {
			Text(name, fontWeight = FontWeight.Bold)
		}
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		HeaderFilled("Todo")
	}
}
