package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * A folder [WebDavService][click.alchemist.cook.service.store.WebDavService] has already removed
 * from the local mirror (a recipe/shopping list delete, or the stale side of a rename) but that
 * still needs a remote `DELETE` — [SyncEngine][click.alchemist.cook.service.store.SyncEngine] only
 * ever diffs individual files, never collections, so nothing else would notice the now-empty
 * directory left behind on the WebDAV server. Cleared once that `DELETE` succeeds.
 */
@Entity(tableName = "pending_folder_deletions", primaryKeys = ["libraryId", "path"])
data class PendingFolderDeletionEntity(
	val libraryId: String,
	val path: String
)

@Dao
interface PendingFolderDeletionDao {
	@Query("SELECT * FROM pending_folder_deletions WHERE libraryId = :libraryId")
	suspend fun loadAll(libraryId: String): List<PendingFolderDeletionEntity>

	@Upsert
	suspend fun upsert(entity: PendingFolderDeletionEntity)

	@Query("DELETE FROM pending_folder_deletions WHERE libraryId = :libraryId AND path = :path")
	suspend fun delete(libraryId: String, path: String)
}
