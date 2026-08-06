package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.store.WebDavService
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/**
 * All writes here are suspend, not blocking: they go through [WebDavService]'s suspend Room/file-I/O
 * calls, and a `runBlocking` wrapper would freeze whatever thread calls this — often the UI thread,
 * via a Compose click handler.
 */
class ShoppingListRepository(private val webDavService: WebDavService) {

	suspend fun save(shoppingList: ShoppingList) {
		shoppingList.name = shoppingList.name.trim()
		webDavService.saveShoppingList(shoppingList)
	}

	suspend fun save(item: ShoppingListItem) {
		item.ingredient.name = item.ingredient.name.trim()
		webDavService.saveShoppingListItem(item)
	}

	suspend fun save(items: List<ShoppingListItem>) {
		items.forEach { save(it) }
	}

	suspend fun delete(shoppingList: ShoppingList) = webDavService.deleteShoppingList(shoppingList.id)

	suspend fun delete(item: ShoppingListItem) = webDavService.deleteShoppingListItem(item.id)

	suspend fun delete(items: List<ShoppingListItem>) {
		items.forEach { delete(it) }
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
