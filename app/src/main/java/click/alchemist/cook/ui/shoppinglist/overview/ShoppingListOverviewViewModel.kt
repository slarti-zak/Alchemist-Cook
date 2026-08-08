package click.alchemist.cook.ui.shoppinglist.overview

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.service.store.repository.ShoppingListRepository
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.launch


class ShoppingListOverviewViewModel(private val shoppingListRepository: ShoppingListRepository) : BaseViewModel() {

    val shoppingLists = shoppingListRepository.live()

    fun saveShoppingList(name: String) = saveShoppingList(ShoppingList(name))

    // Fire-and-forget on viewModelScope: the write goes through suspend Room/file-I/O calls, and
    // callers here are plain Compose click handlers with no result to wait for — the UI updates once
    // `shoppingLists` (backed by Room) picks up the change.
    fun saveShoppingList(list: ShoppingList) {
        viewModelScope.launch { shoppingListRepository.save(list) }
    }

    fun editShoppingList(shoppingList: ShoppingListModel, newName: String) =
        saveShoppingList(shoppingList.shoppingList.copy(name = newName))

    fun delete(list: ShoppingListModel) {
        viewModelScope.launch { shoppingListRepository.delete(list.shoppingList) }
    }
}
