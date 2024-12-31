package click.alchemist.cook.ui.recipe.list

import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.ui.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


class RecipeListViewModel(
    private val recipeRepository: RecipeRepository
) : BaseViewModel() {
    val recipes: Flow<List<RecipeListItem>>

    val search = MutableStateFlow("")

    init {
        val currentSearch = MutableStateFlow(SearchTerm("", emptyList(), emptyList()))

        val allRecipes = recipeRepository.live()
            .map { recipes -> recipes.map { RecipeListItem(it) } }

        recipes = combineTransform(search, allRecipes, currentSearch) { search, allItems, current ->
                if (search == current.search && allItems == current.originalItems) {
                    emit(current)
                    return@combineTransform
                }

                val filtered = if (search.isBlank()) {
                    allItems
                } else if (search.startsWith(current.search) && allItems == current.originalItems) {
                    current.filteredItems.filter { it.recipe.name.contains(search, true) }
                } else {
                    allItems.filter { it.recipe.name.contains(search, true) }
                }
                emit(SearchTerm(search, filtered, allItems))
            }.onEach {
                currentSearch.emit(it)
            }.map { it.filteredItems }
    }

    suspend fun loadImage(recipe: Recipe): BlobModel {
        return recipeRepository.loadImage(recipe)
    }

	data class SearchTerm(val search: String, val filteredItems: List<RecipeListItem>, val originalItems: List<RecipeListItem>)
}