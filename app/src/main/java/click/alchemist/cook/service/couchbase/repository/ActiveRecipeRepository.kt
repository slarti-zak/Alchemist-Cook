package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActiveRecipeRepository(private val webDavService: WebDavService) {

	suspend fun save(recipe: ActiveRecipes) = webDavService.saveActiveRecipes(recipe)

	fun live(): Flow<List<ActiveRecipes>> = webDavService.liveActiveRecipes().map(::listOfNotNull)

	suspend fun delete(id: String) = webDavService.deleteActiveRecipes(id)
}
