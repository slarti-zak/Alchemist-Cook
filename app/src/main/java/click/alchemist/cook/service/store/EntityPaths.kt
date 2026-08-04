package click.alchemist.cook.service.store

import java.util.Locale

/**
 * Path conventions for the WebDAV file tree. IDs are never embedded in state-entity YAML bodies
 * (mirroring [click.alchemist.cook.model.DatabaseObject.id]'s `@JsonIgnore` convention from the
 * Couchbase days, where the id lived outside the document body as the Couchbase doc ID) — instead
 * the id *is* the filename. Recipes are the one exception: their folder name is a human-friendly
 * slug the id is appended to, so recipe ids live in the markdown front matter instead (see
 * [RecipeFileFormat]).
 */
object EntityPaths {
	const val STATE_DIR = ".state"
	const val RECIPES_DIR = "recipes"

	private const val ID_LENGTH = 10
	private val idChars = "0123456789abcdefghijklmnopqrstuvwxyz".toList()

	fun newId(): String = (1..ID_LENGTH).map { idChars.random() }.joinToString("")

	fun slugify(name: String): String {
		val slug = name.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
		return slug.take(40)
	}

	fun recipeFolder(name: String, id: String): String {
		val slug = slugify(name)
		return if (slug.isBlank()) id else "$slug-$id"
	}

	fun recipeMarkdownPath(folder: String) = "$RECIPES_DIR/$folder/recipe.md"
	fun recipeFilePath(folder: String, fileName: String) = "$RECIPES_DIR/$folder/$fileName"

	fun shoppingListPath(id: String) = "$STATE_DIR/shopping-lists/$id.yaml"
	fun shoppingListItemPath(id: String) = "$STATE_DIR/shopping-list-items/$id.yaml"
	fun plannedRecipePath(id: String) = "$STATE_DIR/planned-recipes/$id.yaml"
	fun activeRecipesPath(id: String) = "$STATE_DIR/active-recipes/$id.yaml"
	fun timerPath(id: String) = "$STATE_DIR/timers/$id.yaml"

	fun idFromStateFileName(path: String): String = path.substringAfterLast('/').removeSuffix(".yaml")
}
