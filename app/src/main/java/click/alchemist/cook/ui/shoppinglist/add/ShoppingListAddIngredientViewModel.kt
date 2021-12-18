package click.alchemist.cook.ui.shoppinglist.add

import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.service.couchbase.repository.IngredientRepository
import click.alchemist.cook.service.couchbase.repository.ShoppingListRepository
import click.alchemist.cook.ui.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield
import java.math.BigDecimal


class ShoppingListAddIngredientViewModel(
	private val shoppingListRepository: ShoppingListRepository,
	ingredientRepository: IngredientRepository,
	shoppingListId: String
) : BaseViewModel() {
	val typedIngredient = MutableStateFlow("")

	val shoppingList = shoppingListRepository.live(shoppingListId)
		.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

	val ingredients = typedIngredient
		.map { it.trim() }
		.distinctUntilChanged()
		.combine(ingredientRepository.all) { typed, allIngredients ->
			if (typed.isBlank()) allIngredients.toList() else allIngredients.filter { it.contains(typed, true) }
		}

	suspend fun addIngredient(ingredient: String, amountString: String, unit: IngredientUnit): String? {
		val trimmedName = ingredient.trim()
		val amount = getAmount(amountString)
		val list = shoppingList.first()
		if (amount > BigDecimal.ZERO && trimmedName.isNotEmpty()) {
			val newIngredient = Ingredient(trimmedName, amount, unit)
			val itemToSave = list.added(newIngredient)
			shoppingListRepository.save(itemToSave)

			// Yielding as some keyboards may automatically override the value with a trimmed value overwriting the emitted ""
			yield()
			yield()

			typedIngredient.emit("")
			return trimmedName
		}
		return null
	}

	private fun getAmount(amountString: String): BigDecimal {
		return try {
			BigDecimal(amountString)
		} catch (e: Exception) {
			BigDecimal.ONE
		}
	}
}
