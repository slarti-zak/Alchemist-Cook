package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "planned_recipes")
data class PlannedRecipeEntity(
	@PrimaryKey val id: String,
	val libraryId: String,
	val path: String,
	val recipeId: String,
	val servings: Int
)

@Dao
interface PlannedRecipeDao {
	@Query("SELECT * FROM planned_recipes WHERE libraryId IN (:libraryIds)")
	fun live(libraryIds: List<String>): Flow<List<PlannedRecipeEntity>>

	@Query("SELECT * FROM planned_recipes WHERE libraryId IN (:libraryIds) AND recipeId = :recipeId")
	fun live(libraryIds: List<String>, recipeId: String): Flow<List<PlannedRecipeEntity>>

	@Query("SELECT * FROM planned_recipes WHERE recipeId = :recipeId")
	suspend fun loadForRecipe(recipeId: String): List<PlannedRecipeEntity>

	@Query("SELECT * FROM planned_recipes WHERE id = :id LIMIT 1")
	suspend fun load(id: String): PlannedRecipeEntity?

	@Upsert
	suspend fun upsert(entity: PlannedRecipeEntity)

	@Query("DELETE FROM planned_recipes WHERE id = :id")
	suspend fun delete(id: String)
}
