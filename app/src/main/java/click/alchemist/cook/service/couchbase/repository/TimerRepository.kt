package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.Timer
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

class TimerRepository(private val webDavService: WebDavService) {

	fun save(timer: RunningTimer) = runBlocking { webDavService.saveTimer(timer) }

	fun live(): Flow<List<RunningTimer>> = webDavService.liveTimers()

	fun live(recipeId: String): Flow<List<RunningTimer>> =
		webDavService.liveTimers().map { timers -> timers.filter { it.recipeId == recipeId } }

	fun load(timerId: String): RunningTimer? = runBlocking { webDavService.loadTimer(timerId) }

	suspend fun load(recipeId: String, timerName: String): List<RunningTimer> =
		webDavService.loadTimer(recipeId, timerName)

	suspend fun loadFromNode(nodeIds: List<String>): List<RunningTimer> = webDavService.loadTimersFromNodes(nodeIds)

	fun delete(id: String) = runBlocking { webDavService.deleteTimer(id) }

	fun delete(timer: RunningTimer) = delete(timer.id)

	fun delete(timers: List<RunningTimer>) {
		timers.forEach { delete(it) }
	}

	suspend fun toggle(recipe: Recipe, timer: Timer) {
		val existingTimer = load(recipe.id, timer.name)
		if (existingTimer.isEmpty()) {
			webDavService.saveTimer(
				RunningTimer(
					recipeId = recipe.id,
					title = timer.name,
					content = recipe.name,
					duration = timer.duration,
					startedAt = System.currentTimeMillis()
				)
			)
		} else {
			existingTimer.forEach { delete(it) }
		}
	}

	suspend fun toggle(node: RecipeGraphNode) {
		val existingTimer = loadFromNode(listOf(node.id))
		if (existingTimer.isEmpty()) {
			webDavService.saveTimer(
				RunningTimer(
					graphNodeId = node.id,
					title = node.text.chunkedSequence(10).take(1).firstOrNull() ?: "",
					duration = node.duration,
					startedAt = System.currentTimeMillis()
				)
			)
		} else {
			existingTimer.forEach { delete(it) }
		}
	}

	suspend fun stop(node: RecipeGraphNode) = stop(listOf(node))

	suspend fun stop(nodes: List<RecipeGraphNode>) {
		val existingTimer = loadFromNode(nodes.map { it.id })
		existingTimer.forEach { delete(it) }
	}

	fun addMinute(timer: RunningTimer) {
		val timeToAdd = 1.minutes
		save(timer.copy(duration = DbDuration(timer.duration.dbDuration + timeToAdd)))
	}
}
