package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recipes")
data class RecipeEntity(
	@PrimaryKey val id: String,
	val libraryId: String,
	val path: String,
	val name: String,
	val content: String,
	val serves: Int,
	/** YAML-serialized `List<Ingredient>` (order matters, so this isn't a relational table). */
	val ingredientsJson: String,
	/** YAML-serialized `RecipeGraph`, null when the recipe has no extended/graph content. */
	val extendedContentJson: String?,
	val imageFileName: String?,
	val updatedAt: Long
)

/** Flattened, queryable index of ingredient names for autocomplete, derived from [RecipeEntity.ingredientsJson]. */
@Entity(tableName = "recipe_ingredient_names", primaryKeys = ["recipeId", "name"])
data class RecipeIngredientNameEntity(
	val recipeId: String,
	val name: String
)

@Dao
interface RecipeDao {
	@Query("SELECT * FROM recipes WHERE libraryId IN (:libraryIds) ORDER BY name COLLATE NOCASE")
	fun live(libraryIds: List<String>): Flow<List<RecipeEntity>>

	@Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
	fun live(id: String): Flow<RecipeEntity?>

	@Query("SELECT * FROM recipes WHERE id IN (:ids) ORDER BY name COLLATE NOCASE")
	fun liveByIds(ids: List<String>): Flow<List<RecipeEntity>>

	@Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
	suspend fun load(id: String): RecipeEntity?

	@Query("SELECT id FROM recipes WHERE path = :path LIMIT 1")
	suspend fun idForPath(path: String): String?

	@Query("SELECT * FROM recipes WHERE libraryId = :libraryId")
	suspend fun loadAllForLibrary(libraryId: String): List<RecipeEntity>

	@Query("SELECT DISTINCT name FROM recipe_ingredient_names ORDER BY name COLLATE NOCASE")
	fun liveIngredientNames(): Flow<List<String>>

	@Upsert
	suspend fun upsert(entity: RecipeEntity)

	@Query("DELETE FROM recipes WHERE id = :id")
	suspend fun delete(id: String)

	@Query("DELETE FROM recipe_ingredient_names WHERE recipeId = :recipeId")
	suspend fun deleteIngredientNames(recipeId: String)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertIngredientNames(names: List<RecipeIngredientNameEntity>)

	suspend fun replaceIngredientNames(recipeId: String, names: List<String>) {
		deleteIngredientNames(recipeId)
		if (names.isNotEmpty()) {
			insertIngredientNames(names.distinct().map { RecipeIngredientNameEntity(recipeId, it) })
		}
	}
}
