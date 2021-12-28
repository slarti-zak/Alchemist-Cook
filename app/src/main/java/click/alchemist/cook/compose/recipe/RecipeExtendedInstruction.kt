package click.alchemist.cook.compose.recipe

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import click.alchemist.cook.R
import click.alchemist.cook.compose.*
import click.alchemist.cook.extension.humanReadable
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@Composable
fun RecipeExtendedInstruction(
	node: RecipeGraphNodeModel,
	onClick: ((RecipeGraphNodeModel) -> Unit)? = null,
	onSwipeDelete: ((RecipeGraphNodeModel) -> Unit)? = null,
	onFinished: ((RecipeGraphNodeModel) -> Unit) = {},
	onTimerToggle: ((RecipeGraphNodeModel) -> Unit) = {},
	onAddMinute: ((RecipeGraphNodeModel) -> Unit) = {},
	markdownService: MarkdownService? = null
) {
	if (onSwipeDelete == null) {
		RecipeExtendedInstructionCard(node, onClick, onFinished, onTimerToggle, onAddMinute, markdownService)
	} else {
		val dismissState = rememberDismissState(
			confirmStateChange = {
				val dismissed = it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart
				if (dismissed) onSwipeDelete(node)
				dismissed
			}
		)

		SwipeToDismiss(state = dismissState, background = { SwipeDeleteBackground(dismissState, clipShape = MaterialTheme.shapes.small) }) {
			RecipeExtendedInstructionCard(node, onClick, onFinished, onTimerToggle, onAddMinute, markdownService)
		}
	}
}


@Composable
private fun RecipeExtendedInstructionCard(
	node: RecipeGraphNodeModel,
	onClick: ((RecipeGraphNodeModel) -> Unit)?,
	onFinished: ((RecipeGraphNodeModel) -> Unit) = {},
	onTimerToggle: ((RecipeGraphNodeModel) -> Unit) = {},
	onAddMinute: ((RecipeGraphNodeModel) -> Unit) = {},
	markdownService: MarkdownService?
) {
	Column {
		if (!node.isSingleRecipe) {
			Card(shape = RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp), backgroundColor = MaterialTheme.colors.primaryVariant) {
				Text(node.recipeName, Modifier.padding(8.dp, 0.dp))
			}
		}
		val background by animateColorAsState(getBackgroundColor(node))
		Card(
			shape = RoundedCornerShape(if (node.isSingleRecipe) 8.dp else 0.dp, 8.dp, 8.dp, 8.dp), elevation = 4.dp,
			backgroundColor = background
		) {
			Column(
				Modifier
					.then(if (onClick != null) Modifier.clickable(onClick = { onClick(node) }) else Modifier)
					.padding(8.dp)
			) {
				val hasNodeTime = node.node.duration > DbDuration.ZERO
				val hasUserTime = node.userTime > Duration.ZERO
				val timeTaken = node.timeTaken?.coerceAtLeast(Duration.ZERO)
				val hasTimeTaken = timeTaken != null

				AnimatedVisibility(hasNodeTime || hasUserTime || hasTimeTaken) {
					Row(
						Modifier
							.fillMaxWidth()
							.padding(bottom = 8.dp)
					) {
						if (hasNodeTime || hasUserTime) {
							Icon(
								painterResource(R.drawable.ic_clock_outline),
								contentDescription = "Step Time",
								tint = lightIcon,
								modifier = Modifier.padding(end = 8.dp)
							)
						}
						if (hasNodeTime) {
							Text(text = node.node.duration.humanReadable(), Modifier.alignByBaseline())
						}
						if (hasUserTime) {
							Text(text = "(${node.userTime.humanReadable()})", Modifier.alignByBaseline())
						}

						Spacer(Modifier.weight(1f))

						if (hasTimeTaken) {
							Row {
								Icon(
									painterResource(R.drawable.ic_clock_fast),
									contentDescription = "Elapsed Time",
									tint = lightIcon
								)
								Text(
									text = timeTaken!!.humanReadable(),
									Modifier
										.alignByBaseline()
										.padding(start = 8.dp)
								)
							}
						}
					}
				}

				val textSize = with(LocalDensity.current) { MaterialTheme.typography.body1.fontSize.toPx() }
				AndroidView(
					factory = { context ->
						TextView(context)
							.apply { if (onClick != null) setOnClickListener { onClick.invoke(node) } }
					},
					Modifier.fillMaxWidth()
				) { textView ->
					textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
					if (markdownService == null) {
						textView.text = node.node.text
					} else {
						markdownService.render(node.node.text, textView)
					}
				}

				AnimatedVisibility(node.canBeProcessed) {
					Row(Modifier.padding(top = 8.dp)) {
						if (node.node.duration.dbDuration > Duration.ZERO) {
							TimerButton(onTimerToggle, onAddMinute, node)
						}

						Spacer(Modifier.weight(1f))

						Button(onClick = { onFinished(node) }) {
							Text(stringResource(R.string.general_complete))
						}
					}
				}
			}
		}
	}
}


@Composable
private fun TimerButton(
	onClick: (RecipeGraphNodeModel) -> Unit,
	onAddMinute: (RecipeGraphNodeModel) -> Unit,
	node: RecipeGraphNodeModel
) {
	val cornerSize by animateDpAsState(targetValue = if (node.timer == null) 5.dp else 0.dp)
	val animatedCorner = CornerSize(cornerSize)
	val noCorner = CornerSize(0.dp)
	val time =
		if (node.timer == null) node.node.duration.humanReadable(false)
		else node.timer.remaining.humanReadable(false)

	Card(
		shape = MaterialTheme.shapes.small,
		elevation = 0.dp,
		border = BorderStroke(0.75.dp, MaterialTheme.colors.primary)
	) {

		ConstraintLayout {
			val (icon, text, space, button1, button2) = createRefs()
			val bottomBarrier = createBottomBarrier(icon, text, button1)

			Icon(painter = painterResource(id = R.drawable.ic_timer_sand_empty),
				contentDescription = "Timer",
				modifier = Modifier.constrainAs(icon) {
					start.linkTo(parent.start)
					end.linkTo(text.start)
					top.linkTo(parent.top)
					bottom.linkTo(bottomBarrier)
				})
			Text(time,
				Modifier.constrainAs(text) {
					start.linkTo(icon.end)
					end.linkTo(space.start, 4.dp)
					top.linkTo(parent.top)
					bottom.linkTo(bottomBarrier)
				})
			Spacer(modifier = Modifier.constrainAs(space) {
				start.linkTo(text.end)
				end.linkTo(button1.start)
				top.linkTo(parent.top)
				bottom.linkTo(bottomBarrier)
			})
			Button(
				onClick = { onClick(node) },
				shape = MaterialTheme.shapes.small.copy(bottomStart = animatedCorner, topStart = animatedCorner, bottomEnd = noCorner),
				modifier = Modifier.constrainAs(button1) {
					start.linkTo(space.end)
					end.linkTo(parent.end)
					top.linkTo(parent.top)
					bottom.linkTo(bottomBarrier)
				}
			) {
				Text(if (node.timer == null) "Start" else "Stop", maxLines = 1, softWrap = false)
			}
			AnimatedVisibility(node.timer != null,
				enter = expandVertically(expandFrom = Alignment.Top),
				exit = shrinkVertically(shrinkTowards = Alignment.Top),
				modifier = Modifier.constrainAs(button2) {
					start.linkTo(parent.start)
					end.linkTo(parent.end)
					top.linkTo(bottomBarrier)
					bottom.linkTo(parent.bottom)
				}) {
				Button(
					onClick = { onAddMinute(node) },
					shape = MaterialTheme.shapes.small.copy(topStart = noCorner, topEnd = noCorner)
				) {
					Text(stringResource(id = R.string.add_minute), maxLines = 1, softWrap = false)
				}
			}
		}
	}
}

@Composable
private fun getBackgroundColor(item: RecipeGraphNodeModel): Color {
	return when {
		item.isPreview -> MaterialTheme.colors.surface
		item.isFinished -> cookingGraphFinished
		item.canBeProcessed ->
			cookingGraphProcessable
//				if (item.timeHasCome) R.color.cookingGraphProcessable
//				else R.color.cookingGraphNeutral
//			)
		else -> cookingGraphNotProcessable
	}
}


@Preview("Default")
@Composable
private fun PreviewDefault() {
	AppTheme {
		RecipeExtendedInstruction(RecipeGraphNodeModel(RecipeGraphNode("1", "Text Content"), "Recipe Name"))
	}
}


@Preview("Single Recipe")
@Composable
private fun PreviewSingleRecipe() {
	AppTheme {
		RecipeExtendedInstruction(RecipeGraphNodeModel(RecipeGraphNode("1", "Text Content"), ""))
	}
}


@Preview("Duration")
@Composable
private fun PreviewDuration() {
	AppTheme {
		RecipeExtendedInstruction(RecipeGraphNodeModel(RecipeGraphNode("1", "Text Content", DbDuration(5.minutes)), "Recipe Name"))
	}
}


@Preview("Running")
@Composable
private fun PreviewRunningDuration() {
	AppTheme {
		RecipeExtendedInstruction(
			RecipeGraphNodeModel(
				RecipeGraphNode("1", "Text Content", DbDuration(5.minutes)),
				"Recipe Name",
				dependenciesSatisfied = true,
				graphStartTime = 10
			).apply {
				timeTaken = 1.minutes + 30.seconds
			})
	}
}


@Preview("Running with Timer")
@Composable
private fun PreviewRunningDurationWithTimer() {
	AppTheme {
		RecipeExtendedInstruction(
			RecipeGraphNodeModel(
				RecipeGraphNode("1", "Text Content", DbDuration(5.minutes)),
				"Recipe Name",
				dependenciesSatisfied = true,
				timer = previewRunningTimer(),
				graphStartTime = 10
			).apply {
				timeTaken = 1.minutes + 30.seconds
			})
	}
}