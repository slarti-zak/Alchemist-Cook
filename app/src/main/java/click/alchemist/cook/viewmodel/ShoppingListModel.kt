package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem

data class ShoppingListModel(
	val shoppingList: ShoppingList,
	val ingredients: List<ShoppingListItem> = listOf(),
) {
	val completedCount: Int get() = ingredients.count { it.finished }

	fun added(ingredientsToAdd: List<Ingredient>): List<ShoppingListItem> {
		val groupsToAdd = ingredientsToAdd.groupBy { it.name }

		return groupsToAdd.map { pair ->
			val name = pair.key
			val toAdd = pair.value.reduce { a, b -> Ingredient(name, a.amount + b.amount, a.unitCategory) }

			val existingItem = ingredients.firstOrNull {
				toAdd.unitCategory == it.ingredient.unitCategory
						&& name == it.ingredient.name
						&& !it.finished
			}

			if (existingItem == null) {
				ShoppingListItem(shoppingListId = shoppingList.id, ingredient = toAdd)
			} else {
				val newIngredient = existingItem.ingredient.copy(amount = existingItem.ingredient.amount + toAdd.amount)
				existingItem.copy(ingredient = newIngredient)
			}
		}
	}

	fun added(ingredientToAdd: Ingredient): ShoppingListItem {
		return added(listOf(ingredientToAdd)).single()
	}
}