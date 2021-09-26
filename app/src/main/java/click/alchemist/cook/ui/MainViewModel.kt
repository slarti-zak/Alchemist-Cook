package click.alchemist.cook.ui

import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.service.recipe.TimerService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
class MainViewModel(
    private val couchbaseAccountListener: CouchbaseAccountListener,
    @Suppress("unused") private val timerService: TimerService, // to initialize the timer handling
    recipeRepository: RecipeRepository
) : BaseViewModel() {
    val databaseChanged: Flow<Unit> get() = couchbaseAccountListener.databaseFlow.map { }
    val databaseState: Flow<CouchbaseState> get() = couchbaseAccountListener.databaseFlow.flatMapLatest { it.replicatorChanges }
    val cookingCount = recipeRepository.count()
}