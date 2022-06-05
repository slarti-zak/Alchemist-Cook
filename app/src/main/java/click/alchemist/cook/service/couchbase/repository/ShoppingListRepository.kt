package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.extension.equalTo
import click.alchemist.cook.extension.firstElement
import click.alchemist.cook.model.DatabaseObject
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.couchbase.BaseRepository
import click.alchemist.cook.service.couchbase.CouchbaseService
import click.alchemist.cook.viewmodel.ShoppingListModel
import com.couchbase.lite.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


class ShoppingListRepository(couchbase: CouchbaseService) : BaseRepository<ShoppingList>(couchbase, ShoppingList::class) {
	fun save(shoppingList: ShoppingList) {
		shoppingList.name = shoppingList.name.trim()
		couchbase.save(shoppingList)
	}

	fun save(item: ShoppingListItem) {
		item.ingredient.name = item.ingredient.name.trim()
		couchbase.save(item)
	}

	fun save(items: List<ShoppingListItem>) {
		if (items.isNotEmpty()) {
			couchbase.batch { items.forEach(::save) }
		}
	}

	fun delete(item: ShoppingListItem) {
		delete(item.id)
	}

	fun delete(items: List<ShoppingListItem>) {
		if (items.isNotEmpty()) {
			couchbase.batch { items.forEach(::delete) }
		}
	}

	fun live(): Flow<List<ShoppingListModel>> {
		val shoppingLists = couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where(DatabaseObject::type equalTo ShoppingList::class)
				.orderBy(Ordering.property(ShoppingList::name.name))
		}.map(this::parseOld)

		val shoppingItems = couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where(DatabaseObject::type equalTo ShoppingListItem::class)
		}.map(this::parse)

		return shoppingLists.combine(shoppingItems, this::mergeItems)
	}

	fun liveModel(id: String): Flow<ShoppingListModel> {
		val shoppingLists = couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where(
					(DatabaseObject::type equalTo ShoppingList::class)
						.and(ShoppingList::id equalTo id)
				)
		}.map(this::parseOld)

		val shoppingItems = couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where(
					(DatabaseObject::type equalTo ShoppingListItem::class)
						.and(ShoppingListItem::shoppingListId equalTo id)
				)
		}.map(this::parse)

		return shoppingLists.combine(shoppingItems, this::mergeItems).firstElement()
	}

	private fun parseOld(queryChange: QueryChange): List<ShoppingList> {
		return couchbase.parse(queryChange.results, ShoppingList::class.java)
	}

	private fun parse(queryChange: QueryChange): List<ShoppingListItem> {
		return couchbase.parse(queryChange.results, ShoppingListItem::class.java)
	}

	private fun mergeItems(
		shoppingLists: List<ShoppingList>,
		shoppingItems: List<ShoppingListItem>
	): List<ShoppingListModel> {
		val oldItems = shoppingLists.filter { it.ingredients.isNotEmpty() }
		if (oldItems.isNotEmpty()) {
			couchbase.batch {
				oldItems.forEach { oldList ->
					oldList.ingredients.forEach { couchbase.save(ShoppingListItem(oldList.id, it.ingredient, it.finished)) }
					save(oldList.copy(ingredients = emptyList()))
				}
			}
		}

		val grouped = shoppingItems.groupBy { it.shoppingListId }
		return shoppingLists.map { list ->
			val items = grouped[list.id] ?: emptyList()
			ShoppingListModel(list, items)
		}
	}
}