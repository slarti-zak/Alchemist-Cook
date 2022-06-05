package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.extension.equalTo
import click.alchemist.cook.extension.isIn
import click.alchemist.cook.model.*
import click.alchemist.cook.service.couchbase.BaseRepository
import click.alchemist.cook.service.couchbase.CouchbaseService
import com.couchbase.lite.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*


class RecipeRepository(couchbase: CouchbaseService) : BaseRepository<Recipe>(couchbase, Recipe::class) {
	private val plannedRecipes = couchbase.observe { db ->
		QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
			.from(DataSource.database(db))
			.where(DatabaseObject::type equalTo PlannedRecipe::class)
	}
		.map(::parsePlanned)
		.map { planned -> planned.distinctBy { it.recipeId } }
		.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

	fun save(recipe: Recipe, image: Blob? = null) {
		recipe.name = recipe.name.trim()
		for (i in recipe.ingredients) {
			i.name = i.name.trim()
		}

		couchbase.save(recipe) {
			image?.let { blob -> it.setBlob("image", blob) }
		}
	}

	fun live(): Flow<List<Recipe>> {
		return couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where(DatabaseObject::type equalTo Recipe::class)
				.orderBy(Ordering.property(Recipe::name.name))
		}.map(::parse)
	}

	fun live(condition: Expression): Flow<List<Recipe>> {
		return couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where((DatabaseObject::type equalTo Recipe::class).and(condition))
				.orderBy(Ordering.property(Recipe::name.name))
		}.map(::parse)
	}

	fun livePlanned(): Flow<List<PlannedRecipe>> {
		return plannedRecipes
	}

	fun livePlanned(condition: Expression): Flow<List<PlannedRecipe>> {
		return couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.database(db))
				.where((DatabaseObject::type equalTo PlannedRecipe::class).and(condition))
		}.map(::parsePlanned)
	}

	fun livePlannedRecipes(condition: Expression? = null): Flow<List<PlannedRecipeJoined>> {
		return (if (condition == null) livePlanned() else livePlanned(condition))
			.flatMapLatest { plannedRecipes ->
				if (plannedRecipes.isEmpty()) flowOf(emptyList())
				else {
					live(Recipe::id isIn plannedRecipes.map { it.recipeId })
						.map { recipes ->
							recipes.map { r ->
								val planned = plannedRecipes.first { it.recipeId == r.id }
								PlannedRecipeJoined(r, planned)
							}
						}
				}
			}
	}

	fun count(): Flow<Long> {
		return plannedRecipes.map { it.count().toLong() }
	}

	private fun parse(change: QueryChange) = parse(change.results)
	private fun parsePlanned(change: QueryChange) = parsePlanned(change.results)

	private fun parse(resultSet: ResultSet?) = couchbase.parse(resultSet, Recipe::class)
	private fun parsePlanned(resultSet: ResultSet?) = couchbase.parse(resultSet, PlannedRecipe::class)

	fun load(recipeId: String): Recipe? {
		return couchbase.load(recipeId, Recipe::class)
	}

	suspend fun loadImage(recipe: Recipe): BlobModel {
		return couchbase.getBlob(recipe.id, "image")
	}

	suspend fun startCooking(recipeId: String, servings: Int) {
		modifyCooking(recipeId) { loaded ->
			couchbase.batch {
				loaded.forEach { couchbase.delete(it.id) }
				couchbase.save(PlannedRecipe(recipeId = recipeId, servings = servings))
			}
		}
	}

	suspend fun stopCooking(recipeId: String) {
		return modifyCooking(recipeId) { loaded ->
			couchbase.batch {
				loaded.forEach { couchbase.delete(it.id) }
			}
		}
	}

	private suspend fun modifyCooking(recipeId: String, action: (List<PlannedRecipe>) -> Unit) {
		try {
			val result = couchbase.query { db ->
				QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
					.from(DataSource.database(db))
					.where(
						(DatabaseObject::type equalTo PlannedRecipe::class)
							.and(PlannedRecipe::recipeId equalTo recipeId)
					)
			}

			val parsed = parsePlanned(result)
			action(parsed)
		} catch (e: Exception) {
		}
	}
}