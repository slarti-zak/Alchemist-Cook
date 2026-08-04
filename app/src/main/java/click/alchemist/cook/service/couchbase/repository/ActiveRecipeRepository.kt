package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class ActiveRecipeRepository(private val webDavService: WebDavService) {

	fun save(recipe: ActiveRecipes) = runBlocking { webDavService.saveActiveRecipes(recipe) }

	fun live(): Flow<List<ActiveRecipes>> = webDavService.liveActiveRecipes().map(::listOfNotNull)

	fun delete(id: String) = runBlocking { webDavService.deleteActiveRecipes(id) }
}
