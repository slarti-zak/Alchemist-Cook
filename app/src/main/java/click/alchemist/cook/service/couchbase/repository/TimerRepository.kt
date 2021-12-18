package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.extension.equalTo
import click.alchemist.cook.extension.isIn
import click.alchemist.cook.model.*
import click.alchemist.cook.service.couchbase.CouchbaseService
import com.couchbase.lite.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration.Companion.minutes


class TimerRepository(private val couchbase: CouchbaseService) {
    private var _timers: Flow<List<RunningTimer>>

    init {
        _timers = liveInternal((DatabaseObject::type equalTo RunningTimer::class.simpleName))
            .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)
    }

    fun save(timer: RunningTimer) {
        couchbase.save(timer)
    }

    fun live(): Flow<List<RunningTimer>> {
        return _timers
    }

    fun live(condition: Expression): Flow<List<RunningTimer>> {
        return liveInternal((DatabaseObject::type equalTo RunningTimer::class.simpleName).and(condition))
    }

    private fun liveInternal(condition: Expression): Flow<List<RunningTimer>> {
        return couchbase.observe { db ->
            QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(db))
                .where(condition)
                .orderBy(Ordering.property(RunningTimer::title.name))
        }.map(this::parse)
    }

    private fun parse(queryChange: QueryChange): List<RunningTimer> =
        parse(queryChange.results)

    fun parse(resultSet: ResultSet?): List<RunningTimer> {
        return couchbase.parse(resultSet, RunningTimer::class.java)
    }

    fun load(timerId: String): RunningTimer? {
        return couchbase.load(timerId, RunningTimer::class.java)
    }

    suspend fun load(recipeId: String, timerName: String): List<RunningTimer> {
        val result = couchbase.query { db ->
            QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(db))
                .where(
                    (DatabaseObject::type equalTo RunningTimer::class.simpleName)
                        .and(RunningTimer::recipeId equalTo recipeId)
                        .and(RunningTimer::title equalTo timerName)
                )
        }
        return parse(result)
    }

    suspend fun loadFromNode(nodeIds: List<String>): List<RunningTimer> {
        val result = couchbase.query { db ->
            QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(db))
                .where(
                    (DatabaseObject::type equalTo RunningTimer::class.simpleName)
                        .and(RunningTimer::graphNodeId isIn nodeIds)
                )
        }
        return parse(result)
    }

    fun delete(id: String) {
        load(id)?.let { delete(it) }
    }

    fun delete(timer: RunningTimer) {
        couchbase.delete(timer.id)
    }

    fun delete(timers: MutableList<RunningTimer>) {
        if (timers.isNotEmpty()) {
            couchbase.batch { timers.forEach(this::delete) }
        }
    }

    suspend fun toggle(recipe: Recipe, timer: Timer) {
        val existingTimer = load(recipe.id, timer.name)
        if (existingTimer.isEmpty()) {
            save(
                RunningTimer(
                    recipeId = recipe.id,
                    title = timer.name,
                    content = recipe.name,
                    duration = timer.duration,
                    startedAt = System.currentTimeMillis()
                )
            )
        } else {
            existingTimer.forEach(this::delete)
        }
    }

    suspend fun toggle(node: RecipeGraphNode) {
        val existingTimer = loadFromNode(listOf(node.id))
        if (existingTimer.isEmpty()) {
            save(
                RunningTimer(
                    graphNodeId = node.id,
                    title = node.text.chunkedSequence(10).take(1).firstOrNull() ?: "",
                    duration = node.duration,
                    startedAt = System.currentTimeMillis()
                )
            )
        } else {
            existingTimer.forEach(this::delete)
        }
    }

    suspend fun stop(node: RecipeGraphNode) = stop(listOf(node))

    suspend fun stop(nodes: List<RecipeGraphNode>) {
        val existingTimer = loadFromNode(nodes.map { it.id })
        existingTimer.forEach(this::delete)
    }

	fun addMinute(timer: RunningTimer) {
		val timeToAdd = 1.minutes
		save(timer.copy(duration = DbDuration(timer.duration.dbDuration + timeToAdd)))
	}
}