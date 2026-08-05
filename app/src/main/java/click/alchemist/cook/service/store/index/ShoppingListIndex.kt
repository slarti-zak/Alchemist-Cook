package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
	@PrimaryKey val id: String,
	val libraryId: String,
	val path: String,
	val name: String
)

@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
	@PrimaryKey val id: String,
	val libraryId: String,
	val path: String,
	val shoppingListId: String,
	val ingredientName: String,
	/** `BigDecimal.toString()`, matching how amounts round-trip through the recipe front matter. */
	val ingredientAmount: String,
	val ingredientUnitCategory: String,
	val finished: Boolean
)

@Dao
interface ShoppingListDao {
	@Query("SELECT * FROM shopping_lists WHERE libraryId IN (:libraryIds) ORDER BY name COLLATE NOCASE")
	fun liveLists(libraryIds: List<String>): Flow<List<ShoppingListEntity>>

	@Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
	fun liveList(id: String): Flow<ShoppingListEntity?>

	@Query("SELECT * FROM shopping_list_items WHERE libraryId IN (:libraryIds)")
	fun liveItems(libraryIds: List<String>): Flow<List<ShoppingListItemEntity>>

	@Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :shoppingListId")
	fun liveItems(shoppingListId: String): Flow<List<ShoppingListItemEntity>>

	@Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
	suspend fun loadList(id: String): ShoppingListEntity?

	@Query("SELECT * FROM shopping_list_items WHERE id = :id LIMIT 1")
	suspend fun loadItem(id: String): ShoppingListItemEntity?

	@Query("SELECT id FROM shopping_lists WHERE path = :path LIMIT 1")
	suspend fun idForListPath(path: String): String?

	@Upsert
	suspend fun upsert(entity: ShoppingListEntity)

	@Upsert
	suspend fun upsert(entity: ShoppingListItemEntity)

	@Query("DELETE FROM shopping_lists WHERE id = :id")
	suspend fun deleteList(id: String)

	@Query("DELETE FROM shopping_list_items WHERE id = :id")
	suspend fun deleteItem(id: String)

	@Query("DELETE FROM shopping_list_items WHERE shoppingListId = :shoppingListId")
	suspend fun deleteItemsForList(shoppingListId: String)

	suspend fun deleteListWithItems(id: String) {
		deleteItemsForList(id)
		deleteList(id)
	}
}
