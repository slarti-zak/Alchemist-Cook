package click.alchemist.cook.service.migration

import click.alchemist.cook.logError
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.couchbase.CouchbaseService
import click.alchemist.cook.service.store.EntityPaths
import click.alchemist.cook.service.store.WebDavService
import com.couchbase.lite.DataSource
import com.couchbase.lite.Document
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult

private const val TAG = "CouchbaseToWebDavMigrator"

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
			// Couchbase doc ids are UUIDs, not the `<slug>-<id>` folder scheme's fixed-length, fixed-
			// alphabet ids (see EntityPaths.stableId) — deriving one instead of reusing doc.id verbatim
			// also keeps a re-run of this migration mapping to the same id rather than duplicating.
			val recipe = couchbaseService.parse(doc.toMap(), Recipe::class.java).also { it.id = EntityPaths.stableId(doc.id) }
			val image = doc.getBlob("image")?.content
			// sync = false: every WebDavService.save* below would otherwise kick off its own full
			// library sync (see WebDavService.requestSync), and a migration can easily write hundreds
			// of entities in a row — that's hundreds of queued, serialized, whole-tree PROPFIND-and-
			// reconcile passes stacking up right after each other. One sync at the end covers it all.
			webDavService.saveRecipe(recipe, libraryId = libraryId, image = image, sync = false)
			recipes++
		}

		var shoppingLists = 0
		for (doc in queryAll(ShoppingList::class.simpleName!!)) {
			val list = couchbaseService.parse(doc.toMap(), ShoppingList::class.java).also { it.id = EntityPaths.stableId(doc.id) }
			webDavService.saveShoppingList(list, libraryId = libraryId, sync = false)
			shoppingLists++
		}

		var shoppingListItems = 0
		for (doc in queryAll(ShoppingListItem::class.simpleName!!)) {
			val item = couchbaseService.parse(doc.toMap(), ShoppingListItem::class.java).also { it.id = EntityPaths.stableId(doc.id) }
			// shoppingListId is @JsonIgnore on ShoppingListItem (its parent is normally recovered from
			// its file's own folder, see EntityPaths.shoppingListIdFromItemPath), so it never survives
			// the parse above — read the old Couchbase-side reference straight off the document instead
			// and remap it through the same derivation the list itself just got.
			val oldListId = doc.getString("shoppingListId") ?: continue
			try {
				// Couchbase never cascade-deleted a list's items when the list itself was deleted (see
				// BaseRepository.delete), so some devices carry items whose parent list is long gone.
				// WebDavService.saveShoppingListItem throws for those (no list to attach them to) — if
				// that were allowed to propagate, it would abort this loop entirely and silently drop
				// every item still to come, across every other (perfectly valid) list too.
				webDavService.saveShoppingListItem(item.copy(shoppingListId = EntityPaths.stableId(oldListId)), sync = false)
				shoppingListItems++
			} catch (e: Exception) {
				logError(TAG, "Skipping shopping list item ${doc.id}: its list could not be found", e)
			}
		}

		// A single sync now reconciles everything just written in one pass, instead of the one-sync-
		// per-entity pile-up described above.
		webDavService.syncNow()

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
