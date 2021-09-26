package click.alchemist.cook.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import click.alchemist.cook.R
import kotlin.time.Duration
import kotlin.time.hours
import kotlin.time.minutes
import kotlin.time.seconds

@Composable
fun DurationPickerDialog(initialDuration: Duration = Duration.ZERO, onDurationChanged: (Duration) -> Unit, dismiss: () -> Unit) {
	var time by remember { mutableStateOf(initialDuration) }
	Dialog(onDismissRequest = dismiss) {
		Surface(
			shape = MaterialTheme.shapes.medium,
			color = MaterialTheme.colors.surface
		) {
			Column(horizontalAlignment = Alignment.End) {
				DurationPicker(initialDuration, onDurationChanged = { time = it })
				Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					TextButton(onClick = dismiss) { Text(stringResource(R.string.general_cancel)) }
					TextButton(onClick = { onDurationChanged(time) }) { Text(stringResource(R.string.general_apply)) }
				}
			}
		}
	}
}

@Composable
private fun DurationPicker(initialDuration: Duration = Duration.ZERO, onDurationChanged: (Duration) -> Unit) {
	var time by remember { mutableStateOf(DurationPickerData.fromDuration(initialDuration)) }

	Column {
		CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
			Surface(color = MaterialTheme.colors.primary) {
				Row(
					Modifier
						.fillMaxWidth()
						.padding(start = 8.dp)
						.height(IntrinsicSize.Min)
				) {
					val timeString = buildAnnotatedString {
						val numberStyle = MaterialTheme.typography.h3.toSpanStyle()
						val captionStyle = MaterialTheme.typography.caption.toSpanStyle()

						withStyle(numberStyle) { append(time.hours) }
						withStyle(captionStyle) { append('h') }

						withStyle(numberStyle) { append(time.minutes) }
						withStyle(captionStyle) { append('m') }

						withStyle(numberStyle) { append(time.seconds) }
						withStyle(captionStyle) { append('s') }
					}

					Text(timeString)
					Spacer(modifier = Modifier.weight(1f))
					IconButton(
						onClick = {
							time = time.unshift()
							onDurationChanged(time.toDuration())
						},
						Modifier
							.fillMaxHeight()
							.wrapContentHeight()
					) {
						Icon(painter = painterResource(id = R.drawable.ic_backspace_outline), contentDescription = "Remove Last Number")
					}
					IconButton(
						onClick = {
							time = DurationPickerData()
							onDurationChanged(time.toDuration())
						},
						Modifier
							.fillMaxHeight()
							.wrapContentHeight()
					) {
						Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Clear")
					}
				}
			}
		}

		val onClicked: (String) -> Unit = {
			time = time.add(it)
			onDurationChanged(time.toDuration())
		}
		Row(Modifier.fillMaxWidth()) {
			TimeButton("1", onClicked, Modifier.weight(1f))
			TimeButton("2", onClicked, Modifier.weight(1f))
			TimeButton("3", onClicked, Modifier.weight(1f))
		}

		Row(Modifier.fillMaxWidth()) {
			TimeButton("4", onClicked, Modifier.weight(1f))
			TimeButton("5", onClicked, Modifier.weight(1f))
			TimeButton("6", onClicked, Modifier.weight(1f))
		}

		Row(Modifier.fillMaxWidth()) {
			TimeButton("7", onClicked, Modifier.weight(1f))
			TimeButton("8", onClicked, Modifier.weight(1f))
			TimeButton("9", onClicked, Modifier.weight(1f))
		}

		Row(Modifier.fillMaxWidth()) {
			Spacer(Modifier.weight(1f))
			TimeButton("0", onClicked, Modifier.weight(1f))
			TimeButton("00", onClicked, Modifier.weight(1f))
		}
	}
}


@Composable
private fun TimeButton(time: String, onClicked: (String) -> Unit, modifier: Modifier) {
	TextButton(onClick = { onClicked(time) }, modifier) {
		Text(time)
	}
}

@Preview(showBackground = true)
@Composable
private fun DurationPickerPreview() {
	AppTheme {
		DurationPicker(5.minutes, {})
	}
}

private data class DurationPickerData(val hours: String = "00", val minutes: String = "00", val seconds: String = "00") {
	fun add(toAdd: String): DurationPickerData {
		var newSeconds = seconds + toAdd
		val secondsOverHead = newSeconds.substring(0, newSeconds.length - 2)
		newSeconds = newSeconds.substring(newSeconds.length - 2)

		var newMinutes = minutes + secondsOverHead
		val minutesOverHead = newMinutes.substring(0, newMinutes.length - 2)
		newMinutes = newMinutes.substring(newMinutes.length - 2)

		var newHours = hours + minutesOverHead
		val hoursOverHead = newHours.substring(0, newHours.length - 2)
		newHours = if (hoursOverHead.any { it != '0' }) {
			"99"
		} else {
			newHours.substring(newHours.length - 2)
		}

		return DurationPickerData(newHours, newMinutes, newSeconds)
	}

	fun unshift(): DurationPickerData {
		val newHours = '0' + hours.substring(0, hours.lastIndex)
		val newMinutes = hours.last() + minutes.substring(0, minutes.lastIndex)
		val newSeconds = minutes.last() + seconds.substring(0, seconds.lastIndex)
		return DurationPickerData(newHours, newMinutes, newSeconds)
	}

	fun toDuration(): Duration {
		return hours.toInt().hours + minutes.toInt().minutes + seconds.toInt().seconds
	}

	companion object {
		fun fromDuration(duration: Duration): DurationPickerData {
			return duration.toComponents { hours, minutes, seconds, _ ->
				val secondsString = seconds.toString().padStart(2, '0')
				val minutesString = minutes.toString().padStart(2, '0')
				val hoursString = hours.toString().padStart(2, '0')
				DurationPickerData(hoursString, minutesString, secondsString)
			}
		}
	}
}