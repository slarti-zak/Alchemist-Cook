package click.alchemist.cook.ui.cooking.list

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.extension.firstElement
import click.alchemist.cook.extension.withLatestFrom
import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.PlannedRecipeJoined
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.service.recipe.RecipeTimerParser
import click.alchemist.cook.service.store.repository.ActiveRecipeRepository
import click.alchemist.cook.service.store.repository.RecipeRepository
import click.alchemist.cook.service.store.repository.TimerRepository
import click.alchemist.cook.service.time.TimeService
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.IngredientModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import click.alchemist.cook.viewmodel.Serving
import click.alchemist.cook.viewmodel.TimerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class CookingListViewModel(
	private val recipeRepository: RecipeRepository,
	private val recipeTimerParser: RecipeTimerParser,
	private val activeRecipeRepository: ActiveRecipeRepository,
	private val timerRepository: TimerRepository,
	private val timeService: TimeService
) : BaseViewModel() {

	private val onCookingItemFinished = MutableSharedFlow<RecipeGraphNodeModel>()

	val hasExtendedGraph: Flow<Boolean>
	val recipes: Flow<List<String>>

	init {
		val databaseRecipes = recipeRepository.livePlannedRecipes()
		val databaseActiveGraphs = activeRecipeRepository.live()
			.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

		onCookingItemFinished.withLatestFrom(databaseActiveGraphs, this::handleCookingItemFinished)
			.launchIn(viewModelScope)

		hasExtendedGraph = databaseActiveGraphs.combine(databaseRecipes)
		{ activeGraph, recipes ->
			activeGraph.isNotEmpty() || recipes.any { it.recipe.extendedContent?.nodes?.isNotEmpty() ?: false }
		}
			.distinctUntilChanged()

		recipes = databaseRecipes.map { plannedRecipes -> plannedRecipes.map { it.recipe.id } }
	}

	private val recipeMap = mutableMapOf<String, Flow<CookingRecipeListItem>>()

	fun subscribeRecipe(recipeId: String): Flow<CookingRecipeListItem> {
		return recipeMap.getOrPut(recipeId) {
			val databaseRecipe = recipeRepository.livePlannedRecipes(recipeId)
				.firstElement()
				.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)
			val databaseTimers = timerRepository.live(recipeId)
				.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

			val timerUpdate = databaseTimers
				.map { timers -> timers.isNotEmpty() }
				.distinctUntilChanged()
				.flatMapLatest {
					if (it) timeService.tick()
					else emptyFlow()
				}
				.onStart { emit(System.currentTimeMillis()) }

			val cookingRecipe = databaseRecipe.map(this::convertRecipe)
			val cookingTimers = databaseTimers.map { timers ->
				timers.associateBy { Pair(it.recipeId, it.title) }
			}
			val cookingRecipesWithTimer = cookingRecipe.combine(cookingTimers, this::combineRecipesAndTimers)

			return cookingRecipesWithTimer.combine(timerUpdate, this::recalculateTimers)
				.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(stopTimeout = 1.seconds), 1)
		}
	}

	suspend fun onTimerClick(recipe: CookingRecipeListItem, timerModel: TimerModel) {
		timerRepository.toggle(recipe.recipe, timerModel.timer)
	}

	suspend fun onFinishRecipeClick(recipe: CookingRecipeListItem) {
		recipeRepository.stopCooking(recipe.recipe.id)
	}

	suspend fun loadImage(recipe: Recipe): File? {
		return recipeRepository.loadImage(recipe)
	}

	suspend fun onStartCooking(item: CookingListExtendedItem) {
		if (item.activeRecipeId.isNotBlank()) {
			activeRecipeRepository.delete(item.activeRecipeId)
			timerRepository.stop(item.graph.nodes.map { it.node })
		} else {
			val activeGraph = item.startCooking()
			val activeRecipes = ActiveRecipes(activeGraph, System.currentTimeMillis())
			activeRecipeRepository.save(activeRecipes)
		}
	}

	suspend fun onCookingItemFinished(item: RecipeGraphNodeModel) {
		onCookingItemFinished.emit(item)
		timerRepository.stop(item.node)
	}

	suspend fun onCookingItemTimer(item: RecipeGraphNodeModel) {
		timerRepository.toggle(item.node)
	}

	// Fire-and-forget on viewModelScope: the write goes through a suspend Room call, and this is
	// called directly from a Compose click handler with no result to wait for.
	fun addMinute(timer: TimerModel) {
		if (timer.runningTimer != null) {
			viewModelScope.launch { timerRepository.addMinute(timer.runningTimer) }
		}
	}

	private suspend fun handleCookingItemFinished(graphNodeModel: RecipeGraphNodeModel, actives: List<ActiveRecipes>) {
		val active = actives.firstOrNull() ?: return

		val newGraph = active.graph.copy(nodes = active.graph.nodes.map {
			if (it.node.id == graphNodeModel.node.id) {
				it.copy(finishedAtPoint = DbDuration((System.currentTimeMillis() - active.startedAt).milliseconds))
			} else {
				it
			}
		})
		val newActive = active.copy(graph = newGraph)
		activeRecipeRepository.save(newActive)
	}

	private fun convertRecipe(plannedRecipe: PlannedRecipeJoined): CookingRecipeListItem {
		val recipe = plannedRecipe.recipe
		val planned = plannedRecipe.planned
		val servings = if (planned.servings > 0) planned.servings else recipe.serves
		val serving = Serving(recipe.serves, servings)

		val recipeTimers = recipeTimerParser.parse(recipe)
		val recipeTimersWithRunning = recipeTimers.map { TimerModel(it) }

		val ingredients = recipe.ingredients.map {
			IngredientModel(it).apply { amount = serving.toAmount(it) }
		}

		return CookingRecipeListItem(
			recipe,
			servings,
			ingredients,
			recipeTimersWithRunning
		)
	}

	private fun combineRecipesAndTimers(
		recipe: CookingRecipeListItem,
		timers: Map<Pair<String, String>, RunningTimer>
	): CookingRecipeListItem {
		val newTimers = recipe.timers.map { recipeTimer ->
			val runningTimer = timers[Pair(recipe.recipe.id, recipeTimer.timer.name)]
			if (runningTimer == null) {
				recipeTimer
			} else {
				TimerModel(recipeTimer.timer, runningTimer)
			}
		}

		return recipe.copy(timers = newTimers)
	}

	private fun recalculateTimers(recipe: CookingRecipeListItem, now: Long): CookingRecipeListItem {
		val hasRunningTimer = recipe.timers.any { it.runningTimer != null }

		return if (!hasRunningTimer) {
			recipe
		} else {
			val newTimers = recipe.timers.map { timer ->
				val runningTimer = timer.runningTimer

				if (runningTimer != null && runningTimer.duration.dbDuration > Duration.ZERO) {
					TimerModel.fromRunningTimer(runningTimer, now)
				} else {
					timer
				}
			}

			recipe.copy(timers = newTimers)
		}
	}
}