package click.alchemist.cook.ui

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.service.recipe.TimerService
import click.alchemist.cook.service.store.SyncStatus
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MainViewModel(
	private val couchbaseAccountListener: CouchbaseAccountListener,
	@Suppress("unused") private val timerService: TimerService, // to initialize the timer handling
	webDavService: WebDavService,
	recipeRepository: RecipeRepository
) : BaseViewModel() {
	val databaseChanged: Flow<Unit> get() = couchbaseAccountListener.databaseFlow.map { }
	val syncStatus: StateFlow<SyncStatus> = webDavService.syncStatus
	val cookingCount = recipeRepository.count()

	init {
		viewModelScope.launch {
			couchbaseAccountListener.databaseFlow.collect {
				try {
					if (it.runMaintenance()) {
						logInfo("Ran maintenance!")
					}
				} catch (e: Exception) {
					logError("Could not run maintenance!", e)
				}
			}
		}
	}
}