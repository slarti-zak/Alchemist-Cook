package click.alchemist.cook.ui.recipe.shopping

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.service.store.repository.RecipeRepository
import click.alchemist.cook.service.store.repository.ShoppingListRepository
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.Serving
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class RecipeShoppingViewModel(
	private val recipeRepository: RecipeRepository,
	private val shoppingListRepository: ShoppingListRepository
) : BaseViewModel() {

	private val _ingredients = MutableStateFlow<List<RecipeShoppingIngredient>?>(null)
	val ingredients: StateFlow<List<RecipeShoppingIngredient>?> get() = _ingredients

	private val _shoppingLists = MutableStateFlow<List<ShoppingListModel>>(emptyList())
	val shoppingLists: StateFlow<List<ShoppingListModel>> get() = _shoppingLists

	private val _selectedShoppingList = MutableStateFlow<ShoppingListModel?>(null)
	val selectedShoppingList: StateFlow<ShoppingListModel?> get() = _selectedShoppingList

	suspend fun load(recipeId: String, servings: Serving) {
		if (_ingredients.value != null) return

		val recipe = recipeRepository.load(recipeId) ?: return
		_ingredients.value = recipe.ingredients.map {
			if (it.unitCategory == IngredientCategory.HEADER) {
				RecipeShoppingIngredient(it, null)
			} else {
				val servedIngredient = servings.from(it)
				RecipeShoppingIngredient(servedIngredient, null)
			}
		}

		shoppingListRepository.live()
			.onEach(this::shoppingListsUpdated)
			.launchIn(viewModelScope)
	}

	private fun shoppingListsUpdated(newLists: List<ShoppingListModel>) {
		_shoppingLists.value = newLists

		val existingList = _selectedShoppingList.value
		val newList = existingList?.let { newLists.firstOrNull { newList -> newList.shoppingList.id == it.shoppingList.id } }
			?: newLists.firstOrNull()
		_selectedShoppingList.value = newList
		updateIngredientInfo()
	}

	fun setSelectedShoppingList(shoppingList: ShoppingListModel?) {
		_selectedShoppingList.value = shoppingList
		updateIngredientInfo()
	}

	fun toggleIngredient(ingredient: RecipeShoppingIngredient) {
		_ingredients.value = _ingredients.value?.map {
			if (it.id == ingredient.id) {
				it.copy(selected = !it.selected)
			} else {
				it
			}
		}
	}

	private fun updateIngredientInfo() {
		val ingredients = _ingredients.value ?: return
		val selected = _selectedShoppingList.value

		val newValue =
			if (selected == null) {
				ingredients.map { it.copy(shoppingIngredient = null) }
			} else {
				ingredients.map { it.copy(shoppingIngredient = findIngredient(selected, it.ingredient)) }
			}
		_ingredients.value = newValue
	}

	private fun findIngredient(
		selected: ShoppingListModel,
		ingredient: Ingredient
	): Ingredient? {
		if (ingredient.unitCategory == IngredientCategory.HEADER) return null

		return selected.ingredients.firstOrNull {
			it.ingredient.name == ingredient.name && it.ingredient.unitCategory == ingredient.unitCategory
		}?.ingredient
	}

	// Fire-and-forget on viewModelScope: the write goes through suspend Room/file-I/O calls, and this
	// is called directly from a Compose click handler with no result to wait for.
	fun addToShoppingList() {
		val itemsToAdd =
			_ingredients.value?.filter { it.selected && it.ingredient.unitCategory != IngredientCategory.HEADER }
				?: return
		if (itemsToAdd.isEmpty()) return

		_selectedShoppingList.value?.let {
			val newList = it.added(itemsToAdd.map { it.ingredient })
			viewModelScope.launch { shoppingListRepository.save(newList) }
		}
	}
}