package click.alchemist.cook.ui.cooking.list

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.extension.isNotNullOrBlank
import click.alchemist.cook.extension.withLatestFrom
import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.PlannedRecipeJoined
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.service.couchbase.repository.ActiveRecipeRepository
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.service.couchbase.repository.TimerRepository
import click.alchemist.cook.service.time.TimeService
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
class CookingListExtendedItemViewModel(
    recipeRepository: RecipeRepository,
    private val activeRecipeRepository: ActiveRecipeRepository,
    private val timerRepository: TimerRepository,
    private val timeService: TimeService
) : BaseViewModel() {

    val extendedGraph: Flow<CookingListExtendedItem>
    private val onCookingItemFinished = MutableSharedFlow<RecipeGraphNodeModel>()

    init {
        val databaseRecipes = recipeRepository.livePlannedRecipes()
            .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)
        val databaseActiveGraphs = activeRecipeRepository.live()
            .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)
        val databaseTimers = timerRepository.live()
            .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

        onCookingItemFinished.withLatestFrom(databaseActiveGraphs, this::handleCookingItemFinished)
            .launchIn(viewModelScope)

        val cookingUpdateTrigger = databaseActiveGraphs.combine(databaseRecipes)
        { a, r ->
            a.isNotEmpty() || r.any { it.recipe.extendedContent?.nodes?.isNotEmpty() ?: false }
        }
            .distinctUntilChanged()
            .flatMapLatest { hasGraph ->
                if (hasGraph) timeService.tick()
                else emptyFlow()
            }
            .onStart { emit(System.currentTimeMillis()) }

        val graphTimers = databaseTimers.map { timers ->
            timers.filter { it.graphNodeId.isNotNullOrBlank() }.associateBy { it.graphNodeId }
        }

        extendedGraph = combine(
            databaseActiveGraphs,
            databaseRecipes,
            graphTimers,
            cookingUpdateTrigger,
            this::createExtendedGraph
        )
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

    fun onAddMinute(item: RecipeGraphNodeModel) {
        val timer = item.timer?.runningTimer
        if (timer != null) {
            timerRepository.addMinute(timer)
        }
    }

    private fun handleCookingItemFinished(graphNodeModel: RecipeGraphNodeModel, actives: List<ActiveRecipes>) {
        val active = actives.firstOrNull() ?: return

        val newGraph = active.graph.copy(nodes = active.graph.nodes.map {
            if (it.node.id == graphNodeModel.node.id) {
                it.copy(finishedAtPoint = DbDuration(Duration.milliseconds((System.currentTimeMillis() - active.startedAt))))
            } else {
                it
            }
        })
        val newActive = active.copy(graph = newGraph)
        activeRecipeRepository.save(newActive)
    }

    private fun createExtendedGraph(
        activeGraphs: List<ActiveRecipes>,
        recipes: List<PlannedRecipeJoined>,
        timers: Map<String, RunningTimer>,
        now: Long
    ): CookingListExtendedItem {
        val activeGraph = activeGraphs.firstOrNull()
        if (activeGraph != null) {
            return CookingListExtendedItem(
                RecipeGraphModel.fromActiveGraph(activeGraph, timers, now),
                activeGraph.id,
                activeGraph.startedAt
            )
        }

        return CookingListExtendedItem(
            if (recipes.isNullOrEmpty()) RecipeGraphModel(isPreview = true)
            else RecipeGraphModel.fromRecipe(recipes.map { it.recipe }, now)
        )
    }
}