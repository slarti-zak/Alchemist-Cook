package click.alchemist.cook.ui.cooking.list

import click.alchemist.cook.model.Recipe
import click.alchemist.cook.viewmodel.IngredientModel
import click.alchemist.cook.viewmodel.TimerModel

data class CookingRecipeListItem(
	val recipe: Recipe,
	val servings: Int,
	val ingredients: List<IngredientModel>,
	val timers: List<TimerModel>
)
