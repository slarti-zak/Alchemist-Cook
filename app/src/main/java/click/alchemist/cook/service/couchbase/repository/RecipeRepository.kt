package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.model.PlannedRecipe
import click.alchemist.cook.model.PlannedRecipeJoined
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File

class RecipeRepository(private val webDavService: WebDavService) {

	fun save(recipe: Recipe, image: ByteArray? = null) {
		recipe.name = recipe.name.trim()
		for (i in recipe.ingredients) {
			i.name = i.name.trim()
		}

		kotlinx.coroutines.runBlocking { webDavService.saveRecipe(recipe, image = image) }
	}

	fun live(): Flow<List<Recipe>> = webDavService.liveRecipes()

	fun live(id: String): Flow<Recipe> = webDavService.liveRecipe(id).filterNotNull()

	fun livePlanned(recipeId: String): Flow<List<PlannedRecipe>> = webDavService.livePlannedRecipes(recipeId)

	fun livePlannedRecipes(): Flow<List<PlannedRecipeJoined>> = joinPlanned(webDavService.livePlannedRecipes())

	fun livePlannedRecipes(recipeId: String): Flow<List<PlannedRecipeJoined>> =
		joinPlanned(webDavService.livePlannedRecipes(recipeId))

	private fun joinPlanned(planned: Flow<List<PlannedRecipe>>): Flow<List<PlannedRecipeJoined>> {
		return planned.map { it.distinctBy(PlannedRecipe::recipeId) }
			.flatMapLatest { plannedRecipes ->
				if (plannedRecipes.isEmpty()) flowOf(emptyList())
				else webDavService.liveRecipes(plannedRecipes.map { it.recipeId })
					.map { recipes ->
						recipes.mapNotNull { r ->
							plannedRecipes.firstOrNull { it.recipeId == r.id }?.let { PlannedRecipeJoined(r, it) }
						}
					}
			}
	}

	fun count(): Flow<Long> = webDavService.livePlannedRecipes()
		.map { it.distinctBy(PlannedRecipe::recipeId).count().toLong() }

	fun load(recipeId: String): Recipe? = kotlinx.coroutines.runBlocking { webDavService.loadRecipe(recipeId) }

	suspend fun loadImage(recipe: Recipe): File? = webDavService.loadRecipeImage(recipe.id)

	fun delete(recipe: Recipe) = kotlinx.coroutines.runBlocking { webDavService.deleteRecipe(recipe.id) }

	suspend fun share(recipeId: String, libraryId: String) = webDavService.moveRecipeToLibrary(recipeId, libraryId)

	suspend fun startCooking(recipeId: String, servings: Int) {
		webDavService.deletePlannedRecipesForRecipe(recipeId)
		webDavService.savePlannedRecipe(PlannedRecipe(recipeId = recipeId, servings = servings))
	}

	suspend fun stopCooking(recipeId: String) {
		webDavService.deletePlannedRecipesForRecipe(recipeId)
	}
}
