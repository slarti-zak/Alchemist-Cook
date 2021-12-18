package click.alchemist.cook.compose.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.textIngredientStyle
import click.alchemist.cook.compose.textSubtitleStyle
import click.alchemist.cook.extension.humanReadable
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.Timer
import click.alchemist.cook.viewmodel.TimerModel
import kotlin.time.Duration.Companion.seconds


@Composable
fun TimerItem(modifier: Modifier = Modifier, timer: TimerModel, onClick: ((TimerModel) -> Unit) = { }, onAddMinute: ((TimerModel) -> Unit) = { }) {
    val percentage by animateFloatAsState(
        targetValue = timer.percentage.toFloat(),
        animationSpec = if (timer.runningTimer == null) snap() else tween(500, easing = LinearEasing)
    )
    Card(modifier, elevation = 4.dp) {
        Column(
            Modifier
                .padding(8.dp)
        ) {
            val text = buildAnnotatedString {
                withStyle(textIngredientStyle().toSpanStyle()) {
                    append(timer.timer.name)
                }

                withStyle(textSubtitleStyle().toSpanStyle()) {
                    append(' ')
                    append('(')
                    append(timer.timer.duration.humanReadable())
                    append(')')
                }
            }

            Text(text)

            val paddingTop by animateDpAsState(if (timer.runningTimer == null) 16.dp else 0.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = paddingTop, bottom = 16.dp)
            ) {
                AnimatedVisibility(visible = timer.runningTimer != null) {
                    Text(timer.remaining.humanReadable())
                }
                LinearProgressIndicator(percentage, Modifier.fillMaxWidth())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onClick(timer) }) {
                    Text(stringResource(if (timer.runningTimer == null) R.string.general_play else R.string.general_stop))
                }
                AnimatedVisibility(timer.runningTimer != null && timer.percentage < 1.0) {
                    Button(onClick = { onAddMinute(timer) }) {
                        Text(stringResource(R.string.add_minute))
                    }
                }
            }
        }
    }
}


@Preview("Stopped")
@Composable
private fun PreviewStop() {
    AppTheme {
        TimerItem(timer = TimerModel(Timer("Timer Name", DbDuration(5.seconds))))
    }
}


@Preview("Running")
@Composable
private fun PreviewRunning() {
    AppTheme {
        val timer = Timer("Timer Name", DbDuration(5.seconds))
        TimerItem(timer = TimerModel(timer, RunningTimer(), percentage = 0.5))
    }
}
