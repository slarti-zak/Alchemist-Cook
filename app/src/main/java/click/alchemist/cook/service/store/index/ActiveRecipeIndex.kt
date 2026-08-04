package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "active_recipes")
data class ActiveRecipesEntity(
	@PrimaryKey val id: String,
	val libraryId: String,
	val path: String,
	/** YAML-serialized `ActiveRecipeGraph`. */
	val graphJson: String,
	val startedAt: Long
)

@Dao
interface ActiveRecipeDao {
	@Query("SELECT * FROM active_recipes WHERE libraryId IN (:libraryIds) ORDER BY startedAt LIMIT 1")
	fun live(libraryIds: List<String>): Flow<ActiveRecipesEntity?>

	@Query("SELECT * FROM active_recipes WHERE id = :id LIMIT 1")
	suspend fun load(id: String): ActiveRecipesEntity?

	@Upsert
	suspend fun upsert(entity: ActiveRecipesEntity)

	@Query("DELETE FROM active_recipes WHERE id = :id")
	suspend fun delete(id: String)
}
