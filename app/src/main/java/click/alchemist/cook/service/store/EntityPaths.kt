package click.alchemist.cook.service.store

import java.util.Locale

/**
 * Path conventions for the WebDAV file tree.
 *
 * Recipes and shopping lists are top-level, persistent, user-facing content, each getting its own
 * `<slug>-<id>` folder for human browsability; since a folder name only carries a slug plus a short
 * id, not the full record, their id lives inside the file content instead (see [RecipeFileFormat],
 * [ShoppingListFileFormat]).
 *
 * Everything under `.state/` is transient, device/session-local "currently cooking" state (planned
 * and active recipes, running timers) with no reason to be browsed — those ids are never embedded
 * in the YAML body (mirroring [click.alchemist.cook.model.DatabaseObject.id]'s `@JsonIgnore`
 * convention from the Couchbase days, where the id lived outside the document as the Couchbase doc
 * ID); the id *is* the filename instead.
 */
object EntityPaths {
	const val STATE_DIR = ".state"
	const val RECIPES_DIR = "recipes"
	const val SHOPPING_LISTS_DIR = "shopping-lists"

	private const val ID_LENGTH = 10
	private val idChars = "0123456789abcdefghijklmnopqrstuvwxyz".toList()

	fun newId(): String = (1..ID_LENGTH).map { idChars.random() }.joinToString("")

	fun slugify(name: String): String {
		val slug = name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
		return slug.take(40)
	}

	/** A human-browsable folder name: `<slug-of-name>-<id>`, or just `<id>` if the name has no usable slug. */
	fun slugFolder(name: String, id: String): String {
		val slug = slugify(name)
		return if (slug.isBlank()) id else "$slug-$id"
	}

	fun recipeMarkdownPath(folder: String) = "$RECIPES_DIR/$folder/recipe.md"
	fun recipeFilePath(folder: String, fileName: String) = "$RECIPES_DIR/$folder/$fileName"

	fun shoppingListPath(folder: String) = "$SHOPPING_LISTS_DIR/$folder/list.yaml"
	fun shoppingListItemPath(folder: String, itemId: String) = "$SHOPPING_LISTS_DIR/$folder/items/$itemId.yaml"

	fun plannedRecipePath(id: String) = "$STATE_DIR/planned-recipes/$id.yaml"
	fun activeRecipesPath(id: String) = "$STATE_DIR/active-recipes/$id.yaml"
	fun timerPath(id: String) = "$STATE_DIR/timers/$id.yaml"

	fun idFromStateFileName(path: String): String = path.substringAfterLast('/').removeSuffix(".yaml")
}
