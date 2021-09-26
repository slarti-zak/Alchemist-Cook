package click.alchemist.cook.ui.shoppinglist.overview

import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.service.couchbase.repository.ShoppingListRepository
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class ShoppingListOverviewViewModel(private val shoppingListRepository: ShoppingListRepository) : BaseViewModel() {

    val shoppingLists = shoppingListRepository.live()

    fun saveShoppingList(name: String) = saveShoppingList(ShoppingList(name))

    fun saveShoppingList(list: ShoppingList) = shoppingListRepository.save(list)

    fun editShoppingList(shoppingList: ShoppingListModel, newName: String) =
        saveShoppingList(shoppingList.shoppingList.copy(name = newName))

    fun delete(list: ShoppingListModel) = shoppingListRepository.delete(list.shoppingList)
}