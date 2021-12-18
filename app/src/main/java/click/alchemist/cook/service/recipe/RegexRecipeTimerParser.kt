package click.alchemist.cook.service.recipe

import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.Timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RegexRecipeTimerParser : RecipeTimerParser {
	private val regex = Regex("""\(\(([^()]+)-([\d:]+)\)\)""")
	private val regexNoParenthesis = Regex("""^([^()]+)-([\d:]+)$""")

	override fun parse(recipe: Recipe): List<Timer> {
		val matches = regex.findAll(recipe.content)

		return matches.map(this::createTimerFromMatch).distinctBy { it.name }.toList()
	}

	override fun parseSingle(text: CharSequence, expectParenthesis: Boolean): Timer {
		val regexToUse = if (expectParenthesis) regex else regexNoParenthesis
		val match = regexToUse.find(text) ?: return Timer("", DbDuration.ZERO)

		return createTimerFromMatch(match)
	}

	private fun createTimerFromMatch(match: MatchResult): Timer {
        val (name, time) = match.destructured
		val duration = timerText(time)

		return Timer(name, DbDuration(duration))
	}

	private fun timerText(text: String): Duration {
		if (text.isBlank()) return Duration.ZERO

		val entries = mutableListOf<String>()
		var currentToParse = ""

		var i = text.length
		while (i-- > 0) {
			val char = text[i]
			currentToParse = if (char.isDigit()) {
				char + currentToParse
			} else if (char == ':') {
				entries.add(currentToParse)
				""
			} else {
				break
			}
		}
		entries.add(currentToParse)

		val seconds = parseTimer(entries, 0)
		val minutes = parseTimer(entries, 1)
		val hours = parseTimer(entries, 2)

		return hours.hours + minutes.minutes + seconds.seconds
	}

	private fun parseTimer(entries: List<String>, index: Int): Int {
		if (index < entries.size)
			return entries[index].toIntOrNull() ?: 0
		return 0
	}
}