package click.alchemist.cook.service.recipe

import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.Timer

interface RecipeTimerParser {
    fun parse(recipe: Recipe): List<Timer>
    fun parseSingle(text: CharSequence, expectParenthesis: Boolean): Timer
}
