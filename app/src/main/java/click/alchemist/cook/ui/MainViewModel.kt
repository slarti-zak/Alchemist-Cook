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
	private val webDavService: WebDavService,
	recipeRepository: RecipeRepository
) : BaseViewModel() {
	val databaseChanged: Flow<Unit> get() = couchbaseAccountListener.databaseFlow.map { }
	val syncStatus: StateFlow<SyncStatus> = webDavService.syncStatus
	val cookingCount = recipeRepository.count()

	/**
	 * Pulls in any changes from other devices — WebDAV has no push notifications, so call this on
	 * resume. Debounced to once a minute since `onResume` fires on every trivial return to the
	 * foreground (dismissing a dialog, returning from the camera picker, ...), not just real app opens.
	 */
	fun syncOnResume() = webDavService.syncIfStale()

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