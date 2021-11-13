package click.alchemist.cook.ui.recipe.detail

import androidx.lifecycle.viewModelScope
import click.alchemist.cook.extension.equalTo
import click.alchemist.cook.extension.firstElement
import click.alchemist.cook.extension.share
import click.alchemist.cook.extension.withLatestFrom
import click.alchemist.cook.model.*
import click.alchemist.cook.service.couchbase.repository.RecipeRepository
import click.alchemist.cook.service.couchbase.repository.TimerRepository
import click.alchemist.cook.service.recipe.RecipeTimerParser
import click.alchemist.cook.service.time.TimeService
import click.alchemist.cook.service.time.tickWhen
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.IngredientModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.Serving
import click.alchemist.cook.viewmodel.TimerModel
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import kotlinx.coroutines.flow.*


class RecipeDetailViewModel(
	private val recipeRepository: RecipeRepository,
	private val recipeTimerParser: RecipeTimerParser,
	private val timerRepository: TimerRepository,
	timeService: TimeService,
	recipeId: String
) : BaseViewModel() {

	val recipe = recipeRepository.live(Recipe::id equalTo recipeId)
		.firstElement()
		.distinctUntilChanged()
		.share()

	val image = recipe
		.mapLatest { recipeRepository.loadImage(it) }
		.onStart { emit(BlobModel.empty) }

	val servings: Flow<Int>
	val userServings = MutableStateFlow<Int?>(null)

	val ingredients: Flow<List<IngredientModel>>
	val timers: Flow<List<TimerModel>>
	val extraInstructions: Flow<RecipeGraphModel>

	val closeEvent = MutableSharedFlow<Unit>()

	private val togglePlanning = MutableSharedFlow<Unit>()
	val isPlanning: Flow<Boolean>

	init {
		// Servings
		servings = recipe
			.map { it.serves }
			.combine(userServings) { fromRecipe, userOverride -> (userOverride ?: fromRecipe).coerceAtLeast(1) }
		val servingsObs = servings.combine(recipe) { s, r -> Serving(r.serves, s) }

		// Ingredients
		val ingredientObs = recipe.map { it.ingredients }
		ingredients = ingredientObs.combine(servingsObs) { ing, serv -> updateIngredients(ing, serv) }

		// Timers
		val recipeTimers = recipe.map { recipeTimerParser.parse(it) }
		val runningTimers = timerRepository.live(RunningTimer::recipeId equalTo recipeId)
			.distinctUntilChanged()

		val hasRunningTimers = runningTimers
			.map { timers -> timers.isNotEmpty() }
			.distinctUntilChanged()
		val timerUpdate = timeService.tickWhen(hasRunningTimers)

		timers = combine(recipeTimers, runningTimers, timerUpdate, this::onTimersChanged)

		// Planned State
		val plannedObs = recipeRepository.livePlanned(PlannedRecipe::recipeId equalTo recipeId)
		togglePlanning.withLatestFrom(plannedObs, servings) { _, planned, userServ ->
			if (planned.isEmpty()) recipeRepository.startCooking(recipeId, userServ)
			else recipeRepository.stopCooking(recipeId)
		}.launchIn(viewModelScope)

		isPlanning = plannedObs.map { it.isNotEmpty() }

		// Extended Instructions
		extraInstructions = recipe
			.map { RecipeGraphModel.fromNodes("", it.extendedContent?.nodes) }
	}

	private fun onTimersChanged(recipeTimers: List<Timer>, timers: List<RunningTimer>, now: Long): List<TimerModel> {
		val runningTimers = timers.toMutableList()

		val resultTimers = recipeTimers.map { timer ->
			val index = runningTimers.indexOfFirst { it.title == timer.name }
			if (index >= 0)
				TimerModel.fromRunningTimer(runningTimers.removeAt(index), now)
			else {
				TimerModel(timer)
			}
		}

		val overflowTimers = runningTimers.map { TimerModel.fromRunningTimer(it, now) }
		return resultTimers + overflowTimers
	}

	suspend fun delete() {
		val recipe = recipe.first()
		recipeRepository.delete(recipe)
		closeEvent.emit(Unit)
	}

	private fun updateIngredients(
		ingredients: List<Ingredient>,
		servings: Serving
	): List<IngredientModel> {
		return ingredients.mapIndexed { index, ingredient ->
			IngredientModel(ingredient, id = index).apply {
				amount = servings.toAmount(ingredient)
			}
		}
	}

	suspend fun print(): String {
		val text = recipe.first().content
		if (text.isBlank()) return ""

		val options = MutableDataSet()

		// uncomment to set optional extensions
		//options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
		// uncomment to convert soft-breaks to hard breaks
		//options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

		val parser: Parser = Parser.builder(options).build()
		val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

		// You can re-use parser and renderer instances
		val document: Node = parser.parse(text)
		return renderer.render(document)
	}

	suspend fun toggleCooking() {
		togglePlanning.emit(Unit)
	}

	suspend fun toggleTimer(timer: TimerModel) {
		val recipe = recipe.first()
		timerRepository.toggle(recipe, timer.timer)
	}

	fun addTimerMinute(timer: TimerModel) {
		if (timer.runningTimer != null) {
			timerRepository.addMinute(timer.runningTimer)
		}
	}
}
