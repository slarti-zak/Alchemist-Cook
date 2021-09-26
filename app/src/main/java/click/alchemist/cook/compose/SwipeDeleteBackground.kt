package click.alchemist.cook.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R

@ExperimentalMaterialApi
@Composable
fun SwipeDeleteBackground(dismissState: DismissState, clipShape: Shape = MaterialTheme.shapes.medium) {
	val direction = dismissState.dismissDirection ?: return

	val color by animateColorAsState(
		when (dismissState.targetValue) {
			DismissValue.Default -> Color.LightGray
			DismissValue.DismissedToEnd -> Color.Red
			DismissValue.DismissedToStart -> Color.Red
		}
	)
	val alignment = when (direction) {
		DismissDirection.StartToEnd -> Alignment.CenterStart
		DismissDirection.EndToStart -> Alignment.CenterEnd
	}

	val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

	if (dismissState.offset.value != 0f) {
		Box(
			Modifier
				.fillMaxSize()
				.clip(clipShape)
				.background(color)
				.padding(horizontal = 8.dp),
			contentAlignment = alignment,
		) {
			Icon(
				painterResource(R.drawable.ic_delete),
				contentDescription = "Delete icon",
				modifier = Modifier.scale(scale),
				tint = Color.White
			)
		}
	}
}