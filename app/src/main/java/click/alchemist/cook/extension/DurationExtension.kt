package click.alchemist.cook.extension

import click.alchemist.cook.model.DbDuration
import kotlin.time.Duration

fun Duration.humanReadable(cutZeroes: Boolean = true): String {
	if (this.isNegative()) return "-" + (-this).humanReadable(cutZeroes)

	this.toComponents { hours, minutes, seconds, _ ->
		return when {
			hours > 0 -> {
				val minuteString = minutes.toString().padStart(2, '0')
				val secondsString = seconds.toString().padStart(2, '0')
				"$hours:$minuteString:$secondsString"
			}
			minutes > 0 -> {
				if (seconds == 0 && cutZeroes) {
					"$minutes m"
				} else {
					val secondsString = seconds.toString().padStart(2, '0')
					"$minutes:$secondsString"
				}
			}
			else -> {
				"$seconds s"
			}
		}
	}
}

fun DbDuration.humanReadable(cutZeroes: Boolean = true) = this.dbDuration.humanReadable(cutZeroes)