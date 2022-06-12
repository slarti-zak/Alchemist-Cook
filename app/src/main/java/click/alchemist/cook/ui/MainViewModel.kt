package click.alchemist.cook.ui

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.service.firestore.UserFirestore
import click.alchemist.cook.service.recipe.TimerService
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MainViewModel(
	private val couchbaseAccountListener: CouchbaseAccountListener,
	@Suppress("unused") private val timerService: TimerService, // to initialize the timer handling
	recipeRepository: RecipeRepository,
	userSettings: UserFirestore
) : BaseViewModel() {
	val databaseChanged: Flow<Unit> get() = couchbaseAccountListener.databaseFlow.map { }
	val databaseState: Flow<CouchbaseState> get() = couchbaseAccountListener.databaseFlow.flatMapLatest { it.replicatorChanges }
	val cookingCount = recipeRepository.count()

	init {
		viewModelScope.launch {
			couchbaseAccountListener.databaseFlow.collect {
				try {
					if (it.runMaintenance()) {
						logInfo("Ran maintenance!")
						Analytics.trackEvent("Maintenance")
					}
				} catch (e: Exception) {
					logError("Could not run maintenance!", e)
				}
			}
		}

		viewModelScope.launch {
			userSettings.user.collect({ user ->

			})
		}
	}
}