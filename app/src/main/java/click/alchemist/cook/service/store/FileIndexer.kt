package click.alchemist.cook.service.store

import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.PlannedRecipe
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.store.index.ActiveRecipesEntity
import click.alchemist.cook.service.store.index.AppDatabase
import click.alchemist.cook.service.store.index.PlannedRecipeEntity
import click.alchemist.cook.service.store.index.RecipeEntity
import click.alchemist.cook.service.store.index.ShoppingListEntity
import click.alchemist.cook.service.store.index.ShoppingListItemEntity

/**
 * Keeps the Room index in sync with the file tree: whenever [SyncEngine] or [WebDavService] writes
 * or removes a file in a library's local mirror, it calls through here to parse it and update the
 * corresponding index row(s). Files under `.state/` are simple YAML with no markdown body: their id
 * is the filename itself (see [EntityPaths]), never part of the serialized content. Running timers
 * never reach here at all — they're written straight to Room by [WebDavService], with no file behind
 * them.
 */
class FileIndexer(private val database: AppDatabase) {

	suspend fun onFileChanged(libraryId: String, path: String, content: ByteArray) {
		if (path.contains(".conflict-")) return
		val text = String(content, Charsets.UTF_8)

		when {
			path.endsWith("/recipe.md") -> indexRecipe(libraryId, path, text)
			path.endsWith("/list.yaml") -> indexShoppingList(libraryId, path, text)
			isShoppingListItemPath(path) -> indexShoppingListItem(libraryId, path, text)
			path.startsWith("${EntityPaths.STATE_DIR}/planned-recipes/") -> indexPlannedRecipe(libraryId, path, text)
			path.startsWith("${EntityPaths.STATE_DIR}/active-recipes/") -> indexActiveRecipes(libraryId, path, text)
			else -> Unit // Image files and anything else aren't separately indexed.
		}
	}

	suspend fun onFileRemoved(libraryId: String, path: String) {
		if (path.contains(".conflict-")) return

		when {
			path.endsWith("/recipe.md") -> {
				val id = database.recipeDao().idForPath(path) ?: return
				database.recipeDao().delete(id)
				database.recipeDao().deleteIngredientNames(id)
			}

			path.endsWith("/list.yaml") -> {
				val id = database.shoppingListDao().idForListPath(path) ?: return
				database.shoppingListDao().deleteListWithItems(id)
			}

			isShoppingListItemPath(path) ->
				database.shoppingListDao().deleteItem(EntityPaths.idFromStateFileName(path))

			path.startsWith("${EntityPaths.STATE_DIR}/planned-recipes/") ->
				database.plannedRecipeDao().delete(EntityPaths.idFromStateFileName(path))

			path.startsWith("${EntityPaths.STATE_DIR}/active-recipes/") ->
				database.activeRecipeDao().delete(EntityPaths.idFromStateFileName(path))
		}
	}

	private fun isShoppingListItemPath(path: String) =
		path.startsWith("${EntityPaths.SHOPPING_LISTS_DIR}/") && path.contains("/items/")

	private suspend fun indexRecipe(libraryId: String, path: String, text: String) {
		val parsed = RecipeFileFormat.parse(text, EntityPaths.recipeIdFromPath(path))
		val recipe = parsed.recipe

		database.recipeDao().upsert(
			RecipeEntity(
				id = recipe.id,
				libraryId = libraryId,
				path = path,
				name = recipe.name,
				content = recipe.content,
				serves = recipe.serves,
				ingredientsJson = YamlMapper.instance.writeValueAsString(recipe.ingredients),
				extendedContentJson = recipe.extendedContent?.let { YamlMapper.instance.writeValueAsString(it) },
				imageFileName = parsed.imageFileName,
				updatedAt = parsed.updatedAt
			)
		)

		val names = recipe.ingredients
			.filter { it.unitCategory != IngredientCategory.HEADER && it.name.isNotBlank() }
			.map { it.name.trim() }
		database.recipeDao().replaceIngredientNames(recipe.id, names)
	}

	private suspend fun indexShoppingList(libraryId: String, path: String, text: String) {
		val list = ShoppingListFileFormat.parse(text, EntityPaths.shoppingListIdFromPath(path))
		database.shoppingListDao().upsert(ShoppingListEntity(list.id, libraryId, path, list.name))
	}

	private suspend fun indexShoppingListItem(libraryId: String, path: String, text: String) {
		val id = EntityPaths.idFromStateFileName(path)
		val item = StateFileFormat.parse(text, ShoppingListItem::class.java)
		database.shoppingListDao().upsert(
			ShoppingListItemEntity(
				id = id,
				libraryId = libraryId,
				path = path,
				shoppingListId = EntityPaths.shoppingListIdFromItemPath(path),
				ingredientName = item.ingredient.name,
				ingredientAmount = item.ingredient.amount.toString(),
				ingredientUnitCategory = item.ingredient.unitCategory.name,
				finished = item.finished
			)
		)
	}

	private suspend fun indexPlannedRecipe(libraryId: String, path: String, text: String) {
		val id = EntityPaths.idFromStateFileName(path)
		val planned = StateFileFormat.parse(text, PlannedRecipe::class.java)
		database.plannedRecipeDao().upsert(PlannedRecipeEntity(id, libraryId, path, planned.recipeId, planned.servings))
	}

	private suspend fun indexActiveRecipes(libraryId: String, path: String, text: String) {
		val id = EntityPaths.idFromStateFileName(path)
		val active = StateFileFormat.parse(text, ActiveRecipes::class.java)
		database.activeRecipeDao().upsert(
			ActiveRecipesEntity(id, libraryId, path, YamlMapper.instance.writeValueAsString(active.graph), active.startedAt)
		)
	}
}
