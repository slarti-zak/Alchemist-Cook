package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.Timer
import kotlin.time.Duration
import kotlin.time.DurationUnit

class TimerModel(
    val timer: Timer,
    val runningTimer: RunningTimer? = null,
    val remaining: Duration = Duration.ZERO,
    val percentage: Double = 0.0
) {
//	fun percentage(time: Long) :Double {
//		if (runningTimer == null) return 0.0
//		val elapsed = (time - runningTimer.startedAt).milliseconds
//		return elapsed / timer.duration.dbDuration
//	}

    companion object {
        fun fromRunningTimer(runningTimer: RunningTimer, now: Long): TimerModel {
            val started = runningTimer.startedAt
            val elapsed = now - started

            val remaining =
                runningTimer.duration.dbDuration - Duration.milliseconds(elapsed).coerceIn(Duration.ZERO, runningTimer.duration.dbDuration)
            val percentage = (elapsed.toDouble() / runningTimer.duration.dbDuration.toDouble(DurationUnit.MILLISECONDS)).coerceIn(0.0, 1.0)

            return TimerModel(Timer(runningTimer.title, runningTimer.duration), runningTimer, remaining, percentage)
        }
    }
}