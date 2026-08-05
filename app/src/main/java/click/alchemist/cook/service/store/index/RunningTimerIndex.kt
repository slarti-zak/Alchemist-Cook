package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Unlike the other entities here, this has no [path][ShoppingListEntity.path] — timers live only in Room, never as files. */
@Entity(tableName = "running_timers")
data class RunningTimerEntity(
	@PrimaryKey val id: String,
	val libraryId: String,
	val recipeId: String,
	val graphNodeId: String,
	val title: String,
	val content: String,
	/** Duration in milliseconds; `Double.MAX_VALUE` represents "infinite", matching [click.alchemist.cook.model.DbDuration]'s wire format. */
	val durationMillis: Double,
	val startedAt: Long
)

@Dao
interface RunningTimerDao {
	@Query("SELECT * FROM running_timers WHERE libraryId IN (:libraryIds) ORDER BY title COLLATE NOCASE")
	fun live(libraryIds: List<String>): Flow<List<RunningTimerEntity>>

	@Query("SELECT * FROM running_timers WHERE id = :id LIMIT 1")
	suspend fun load(id: String): RunningTimerEntity?

	@Query("SELECT * FROM running_timers WHERE recipeId = :recipeId AND title = :title")
	suspend fun load(recipeId: String, title: String): List<RunningTimerEntity>

	@Query("SELECT * FROM running_timers WHERE graphNodeId IN (:graphNodeIds)")
	suspend fun loadFromNodes(graphNodeIds: List<String>): List<RunningTimerEntity>

	@Upsert
	suspend fun upsert(entity: RunningTimerEntity)

	@Query("DELETE FROM running_timers WHERE id = :id")
	suspend fun delete(id: String)
}
