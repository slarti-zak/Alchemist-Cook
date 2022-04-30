package click.alchemist.cook.compose.recipe

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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


	Box(
		Modifier
			.border(BorderStroke(0.75.dp, MaterialTheme.colors.primary), shape = MaterialTheme.shapes.small)
			.clip(MaterialTheme.shapes.small)
			.background(MaterialTheme.colors.surface)
	)
	{
		Column(
			Modifier
				.width(IntrinsicSize.Max)
				.clearAndSetSemantics {}) {
			Row(
				Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_timer_sand_empty),
					contentDescription = "Timer"
				)
				Text(time)
				Spacer(Modifier.weight(1f))
				SmallButton(
					onClick = { onClick(node) },
					shape = MaterialTheme.shapes.small.copy(bottomStart = animatedCorner, topStart = animatedCorner, bottomEnd = noCorner)
				) {
					Text(if (node.timer == null) "Start" else "Stop", maxLines = 1, softWrap = false)
				}
			}

			AnimatedVisibility(
				node.timer != null,
				Modifier.fillMaxWidth(),
				enter = expandVertically(expandFrom = Alignment.Top),
				exit = shrinkVertically(shrinkTowards = Alignment.Top)
			) {
				SmallButton(
					onClick = { onAddMinute(node) },
					shape = MaterialTheme.shapes.small.copy(topStart = noCorner, topEnd = noCorner)
				) {
					Text(stringResource(id = R.string.add_minute), maxLines = 1, softWrap = false)
				}
			}
		}
	}
}

/**
 * In contrast to [Button] does not enforce a min height using [Modifier.minimumTouchTargetSize].
 */
@Composable
fun SmallButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	shape: Shape = MaterialTheme.shapes.small,
	colors: ButtonColors = ButtonDefaults.buttonColors(),
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
	content: @Composable RowScope.() -> Unit
) {
	val contentColor by colors.contentColor(enabled)
	CompositionLocalProvider(
		LocalContentColor provides contentColor,
	) {
		Box(
			modifier = modifier
				.background(shape = shape, color = colors.backgroundColor(enabled = true).value)
				.clip(shape)
				.clickable(
					interactionSource = interactionSource,
					indication = rememberRipple(),
					enabled = enabled,
					role = Role.Button,
					onClick = onClick
				)
				.semantics(mergeDescendants = false) {}
				.pointerInput(Unit) {},
			propagateMinConstraints = true
		) {
			CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
				ProvideTextStyle(
					value = MaterialTheme.typography.button
				) {
					Row(
						Modifier
							.defaultMinSize(
								minWidth = ButtonDefaults.MinWidth,
								minHeight = ButtonDefaults.MinHeight
							)
							.padding(contentPadding),
						horizontalArrangement = Arrangement.Center,
						verticalAlignment = Alignment.CenterVertically,
						content = content
					)
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