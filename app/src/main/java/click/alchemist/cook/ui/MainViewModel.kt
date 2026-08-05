package click.alchemist.cook.ui

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.service.recipe.TimerService
import click.alchemist.cook.service.store.SyncStatus
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes


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

	/**
	 * Keeps syncing once a minute for as long as the app stays in the foreground. Meant to be driven
	 * by `repeatOnLifecycle(Lifecycle.State.RESUMED)`, which cancels this (via coroutine cancellation)
	 * the moment the app backgrounds — background/closed sync is [syncOnResume] plus the coarser
	 * periodic WorkManager job (`WebDavSyncWork`), not this loop.
	 */
	suspend fun syncPeriodically() {
		while (true) {
			delay(1.minutes)
			webDavService.syncNow()
		}
	}

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