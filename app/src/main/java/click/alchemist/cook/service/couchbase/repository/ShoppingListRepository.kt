package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.store.WebDavService
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking

class ShoppingListRepository(private val webDavService: WebDavService) {

	fun save(shoppingList: ShoppingList) {
		shoppingList.name = shoppingList.name.trim()
		runBlocking { webDavService.saveShoppingList(shoppingList) }
	}

	fun save(item: ShoppingListItem) {
		item.ingredient.name = item.ingredient.name.trim()
		runBlocking { webDavService.saveShoppingListItem(item) }
	}

	fun save(items: List<ShoppingListItem>) {
		items.forEach(::save)
	}

	fun delete(shoppingList: ShoppingList) = runBlocking { webDavService.deleteShoppingList(shoppingList.id) }

	fun delete(item: ShoppingListItem) = runBlocking { webDavService.deleteShoppingListItem(item.id) }

	fun delete(items: List<ShoppingListItem>) {
		items.forEach(::delete)
	}

	fun live(): Flow<List<ShoppingListModel>> =
		webDavService.liveShoppingLists().combine(webDavService.liveShoppingListItems(), this::mergeItems)

	fun liveModel(id: String): Flow<ShoppingListModel> =
		webDavService.liveShoppingList(id).combine(webDavService.liveShoppingListItems(id)) { list, items ->
			list?.let { ShoppingListModel(it, items) }
		}.filterNotNull()

	private fun mergeItems(shoppingLists: List<ShoppingList>, shoppingItems: List<ShoppingListItem>): List<ShoppingListModel> {
		val grouped = shoppingItems.groupBy { it.shoppingListId }
		return shoppingLists.map { list -> ShoppingListModel(list, grouped[list.id] ?: emptyList()) }
	}
}
