package click.alchemist.cook.service.migration

import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.couchbase.CouchbaseService
import click.alchemist.cook.service.store.WebDavService
import com.couchbase.lite.DataSource
import com.couchbase.lite.Document
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult

data class MigrationResult(val recipes: Int, val shoppingLists: Int, val shoppingListItems: Int)

/**
 * One-time import of a user's existing Couchbase data into a WebDAV library. Only persistent
 * content (recipes with images, shopping lists) is carried over — transient "currently cooking"
 * state (active recipes, planned recipes, running timers) is intentionally left behind, since it's
 * tied to a specific device/session and safe to lose across a storage migration. The Couchbase
 * database itself is left untouched, so this can be re-run or ignored without risk.
 */
class CouchbaseToWebDavMigrator(
	private val couchbaseService: CouchbaseService,
	private val webDavService: WebDavService
) {
	suspend fun migrate(libraryId: String): MigrationResult {
		var recipes = 0
		for (doc in queryAll(Recipe::class.simpleName!!)) {
			val recipe = couchbaseService.parse(doc.toMap(), Recipe::class.java).also { it.id = doc.id }
			val image = doc.getBlob("image")?.content
			webDavService.saveRecipe(recipe, libraryId = libraryId, image = image)
			recipes++
		}

		var shoppingLists = 0
		for (doc in queryAll(ShoppingList::class.simpleName!!)) {
			val list = couchbaseService.parse(doc.toMap(), ShoppingList::class.java).also { it.id = doc.id }
			webDavService.saveShoppingList(list, libraryId = libraryId)
			shoppingLists++
		}

		var shoppingListItems = 0
		for (doc in queryAll(ShoppingListItem::class.simpleName!!)) {
			val item = couchbaseService.parse(doc.toMap(), ShoppingListItem::class.java).also { it.id = doc.id }
			webDavService.saveShoppingListItem(item)
			shoppingListItems++
		}

		return MigrationResult(recipes, shoppingLists, shoppingListItems)
	}

	private suspend fun queryAll(type: String): List<Document> {
		val resultSet = couchbaseService.query { db ->
			QueryBuilder.select(SelectResult.expression(Meta.id))
				.from(DataSource.collection(db.defaultCollection))
				.where(Expression.property("type").equalTo(Expression.string(type)))
		}
		return resultSet.mapNotNull { row -> row.getString("id")?.let { couchbaseService.getDocument(it) } }
	}
}
