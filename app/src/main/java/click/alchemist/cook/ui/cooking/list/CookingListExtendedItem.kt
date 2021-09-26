package click.alchemist.cook.ui.cooking.list

import click.alchemist.cook.viewmodel.RecipeGraphModel

data class CookingListExtendedItem(
	val graph: RecipeGraphModel,
	val activeRecipeId: String = "",
	val startedAt: Long = 0
) {
	fun startCooking() = graph.startCooking()
}
