package click.alchemist.cook.ui.shoppinglist.detail

import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.couchbase.repository.ShoppingListRepository
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.math.BigDecimal


class ShoppingListDetailViewModel(
    private val shoppingListRepository: ShoppingListRepository,
    shoppingListId: String
) : BaseViewModel() {
    private val todoHeader = ShoppingListItem(ingredient = Ingredient(unitCategory = IngredientCategory.HEADER), finished = false)
    private val finishedHeader = ShoppingListItem(ingredient = Ingredient(unitCategory = IngredientCategory.HEADER), finished = true)

    val shoppingList = shoppingListRepository.live(shoppingListId)
        .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

    val ingredients = shoppingList.map(this::createShoppingItems)

    private fun createShoppingItems(shoppingList: ShoppingListModel?): List<ShoppingListItem> {
        if (shoppingList == null) return listOf()

        val sorted = shoppingList.ingredients.toMutableList()
        sorted.sortWith(compareBy<ShoppingListItem> { it.finished }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.ingredient.name })

        val finishedIndex = sorted.indexOfFirst { it.finished }
        if (finishedIndex >= 0) {
            sorted.add(finishedIndex, finishedHeader)
        }

        val unfinishedIndex = sorted.indexOfFirst { !it.finished }
        if (unfinishedIndex >= 0) {
            sorted.add(unfinishedIndex, todoHeader)
        }
        return sorted
    }

    fun toggleState(ingredient: ShoppingListItem) {
        if (ingredient.ingredient.unitCategory == IngredientCategory.HEADER) return

        val toSave = ingredient.copy(finished = !ingredient.finished)
        shoppingListRepository.save(toSave)
    }

    suspend fun clearList() {
        val list = shoppingList.first()
        val allFinished = list.ingredients.filter { item -> item.finished }
        shoppingListRepository.delete(allFinished)
    }

    fun remove(item: ShoppingListItem, amountString: String, unit: IngredientUnit) {
        val amount = getAmount(amountString)

        if (amount > BigDecimal.ZERO) {
            val toRemove = unit.toBase(amount)
            val newAmount = item.ingredient.amount - toRemove
            if (newAmount > BigDecimal.ZERO) {
                shoppingListRepository.save(item.copy(ingredient = item.ingredient.copy(amount = newAmount)))
            } else {
                shoppingListRepository.delete(item)
            }
        }
    }

    private fun getAmount(amount: String): BigDecimal {
        return try {
            BigDecimal(amount)
        } catch (e: Exception) {
            BigDecimal.ONE
        }
    }
}