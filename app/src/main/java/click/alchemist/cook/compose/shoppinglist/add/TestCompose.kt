package click.alchemist.cook.compose.shoppinglist.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TestCompose() {
	Row(verticalAlignment = Alignment.CenterVertically) {
		Box(
			modifier = Modifier
				.size(70.dp, 70.dp)
				.background(Color.Blue)
		) {
		}
		Box(
			modifier = Modifier
				.size(70.dp, 70.dp)
				.background(Color.Yellow)
		) {
		}
		Box(
			modifier = Modifier
				.size(100.dp, 100.dp)
				.background(Color.Red)
		) {
		}
	}
}

@Preview
@Composable
private fun Preview() {
	MaterialTheme {
		TestCompose()
	}
}