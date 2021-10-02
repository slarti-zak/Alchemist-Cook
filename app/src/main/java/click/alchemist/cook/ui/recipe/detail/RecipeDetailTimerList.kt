package click.alchemist.cook.ui.recipe.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.timer.TimerItem
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.Timer
import click.alchemist.cook.viewmodel.TimerModel
import kotlin.time.Duration

@ExperimentalAnimationApi
@Composable
fun RecipeDetailTimerList(timers: List<TimerModel>, onClick: (TimerModel) -> Unit = {}, onAddMinute: (TimerModel) -> Unit = {}) {
	LazyColumn(
		Modifier.fillMaxSize(),
		contentPadding = PaddingValues(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		content = { recipeDetailTimerListContent(timers, onClick, onAddMinute) }
	)
}

@ExperimentalAnimationApi
fun LazyListScope.recipeDetailTimerListContent(
	timers: List<TimerModel>,
	onClick: (TimerModel) -> Unit,
	onAddMinute: (TimerModel) -> Unit
) {
	items(timers, { it.timer.name }) { timer ->
		TimerItem(timer = timer, onClick = onClick, onAddMinute = { onAddMinute(timer) })
	}
}

@ExperimentalAnimationApi
@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeDetailTimerList(
			listOf(
				TimerModel(Timer("Timer", DbDuration(Duration.minutes(5)))),
				TimerModel.fromRunningTimer(RunningTimer("Id", title = "Timer", duration = DbDuration(Duration.minutes(5))), 0L),
			)
		)
	}
}