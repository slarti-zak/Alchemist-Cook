package click.alchemist.cook.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R


@Composable
fun SwipeDeleteBackground(dismissState: SwipeToDismissBoxState, clipShape: Shape = MaterialTheme.shapes.medium) {
	val direction = dismissState.dismissDirection

	val color by animateColorAsState(
		when (dismissState.targetValue) {
			SwipeToDismissBoxValue.Settled -> Color.LightGray
			SwipeToDismissBoxValue.StartToEnd -> Color.Red
			SwipeToDismissBoxValue.EndToStart -> Color.Red
		}
	)
	val alignment = when (direction) {
		SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
		SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
		SwipeToDismissBoxValue.Settled -> Alignment.CenterStart
	}

	val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f)

	if (dismissState.progress != 0f) {
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
				contentDescription = stringResource(R.string.content_description_delete_icon),
				modifier = Modifier.scale(scale),
				tint = Color.White
			)
		}
	}
}