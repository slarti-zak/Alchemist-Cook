package click.alchemist.cook.service.store

import click.alchemist.cook.service.store.EntityPaths.ID_LENGTH
import click.alchemist.cook.service.store.EntityPaths.idChars
import click.alchemist.cook.service.store.EntityPaths.idFromFolder
import click.alchemist.cook.service.store.EntityPaths.newId
import click.alchemist.cook.service.store.EntityPaths.shoppingListIdFromItemPath
import click.alchemist.cook.service.store.EntityPaths.slugFolder
import java.util.Locale

/**
 * Path conventions for the WebDAV file tree.
 *
 * Recipes and shopping lists are top-level, persistent, user-facing content, each getting its own
 * `<slug>-<id>` folder for human browsability. The folder name alone already pins down the id (see
 * [idFromFolder]), so it's never duplicated inside the file content itself (see [RecipeFileFormat],
 * [ShoppingListFileFormat]); the same goes for a shopping list item's parent-list id, since an item's
 * path is always nested inside its list's own folder (see [shoppingListIdFromItemPath]).
 *
 * Everything under `.state/` is transient, device/session-local "currently cooking" state (planned
 * and active recipes) with no reason to be browsed — those ids are never embedded in the YAML body
 * (mirroring [click.alchemist.cook.model.DatabaseObject.id]'s `@JsonIgnore` convention from the
 * Couchbase days, where the id lived outside the document as the Couchbase doc ID); the id *is* the
 * filename instead. Running timers are more transient still — they never touch the file tree at all,
 * living purely in the local Room index (see [click.alchemist.cook.service.store.WebDavService]).
 */
object EntityPaths {
	const val STATE_DIR = ".state"
	const val RECIPES_DIR = "recipes"
	const val SHOPPING_LISTS_DIR = "shopping-lists"

	private const val ID_LENGTH = 10
	private val idChars = "0123456789abcdefghijklmnopqrstuvwxyz".toList()

	fun newId(): String = (1..ID_LENGTH).map { idChars.random() }.joinToString("")

	/**
	 * Deterministically derives a valid id (same alphabet/length as [newId]) from an arbitrary external
	 * key, rather than minting a random one. Used by [click.alchemist.cook.service.migration.CouchbaseToWebDavMigrator]
	 * so a Couchbase document's id — a UUID, the wrong shape for a `<slug>-<id>` folder — maps to a
	 * compliant id, and re-running the migration maps the same old document to the same new id instead
	 * of creating a duplicate.
	 */
	fun stableId(source: String): String =
		java.util.UUID.nameUUIDFromBytes(source.toByteArray()).toString().replace("-", "").take(ID_LENGTH)

	fun slugify(name: String): String {
		val slug = name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
		return slug.take(40)
	}

	/** A human-browsable folder name: `<slug-of-name>-<id>`, or just `<id>` if the name has no usable slug. */
	fun slugFolder(name: String, id: String): String {
		val slug = slugify(name)
		return if (slug.isBlank()) id else "$slug-$id"
	}

	/**
	 * Recovers an entity's id from its `<slug>-<id>` folder name (or bare `<id>`, see [slugFolder]) —
	 * since [idChars] never includes `-`, the trailing [ID_LENGTH] characters are always exactly the id.
	 */
	fun idFromFolder(folder: String): String = folder.takeLast(ID_LENGTH)

	fun recipeMarkdownPath(folder: String) = "$RECIPES_DIR/$folder/recipe.md"
	fun recipeFilePath(folder: String, fileName: String) = "$RECIPES_DIR/$folder/$fileName"
	fun recipeIdFromPath(path: String): String =
		idFromFolder(path.removePrefix("$RECIPES_DIR/").removeSuffix("/recipe.md"))

	fun shoppingListPath(folder: String) = "$SHOPPING_LISTS_DIR/$folder/list.yaml"
	fun shoppingListItemPath(folder: String, itemId: String) = "$SHOPPING_LISTS_DIR/$folder/items/$itemId.yaml"

	fun shoppingListIdFromPath(path: String): String =
		idFromFolder(path.removePrefix("$SHOPPING_LISTS_DIR/").removeSuffix("/list.yaml"))

	/** Recovers a shopping list's id from an item's own path, without needing the list already indexed. */
	fun shoppingListIdFromItemPath(path: String): String =
		idFromFolder(path.removePrefix("$SHOPPING_LISTS_DIR/").substringBefore("/items/"))

	fun plannedRecipePath(id: String) = "$STATE_DIR/planned-recipes/$id.yaml"
	fun activeRecipesPath(id: String) = "$STATE_DIR/active-recipes/$id.yaml"

	fun idFromStateFileName(path: String): String = path.substringAfterLast('/').removeSuffix(".yaml")

	private val unsyncedStatePrefixes = listOf("$STATE_DIR/active-recipes/")

	/**
	 * Recipes, shopping lists, and which recipes are planned/marked for cooking are worth syncing
	 * across devices. In-progress cooking-graph state is per-device, in-the-moment (like Couchbase's
	 * `notForSync` documents were) — [SyncEngine] never pushes, pulls, or propagates deletions for it,
	 * so it stays purely local. Running timers don't need an entry here at all: they're never written
	 * to the file tree in the first place.
	 */
	fun isSynced(path: String): Boolean = unsyncedStatePrefixes.none { path.startsWith(it) }
}
