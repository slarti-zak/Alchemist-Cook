package click.alchemist.cook.ui.recipe.shopping

import click.alchemist.cook.model.Ingredient
import java.util.UUID

data class RecipeShoppingIngredient(
	val ingredient: Ingredient,
	val shoppingIngredient: Ingredient?,
	val selected: Boolean = true,
	val id: UUID = UUID.randomUUID()
)
