package click.alchemist.cook.service.store.index

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * Bookkeeping for [click.alchemist.cook.service.store.SyncEngine]: the remote ETag/mtime a given
 * local file was last synced against, so a future PROPFIND diff can tell "changed since last sync"
 * apart from "changed on both sides" (a conflict). Never itself synced to the WebDAV server.
 */
@Entity(tableName = "sync_file_state", primaryKeys = ["libraryId", "path"])
data class SyncFileStateEntity(
	val libraryId: String,
	val path: String,
	val remoteEtag: String?,
	val remoteLastModified: Long?,
	val localMtime: Long,
	val isDirectory: Boolean
)

@Dao
interface SyncFileStateDao {
	@Query("SELECT * FROM sync_file_state WHERE libraryId = :libraryId")
	suspend fun loadAll(libraryId: String): List<SyncFileStateEntity>

	@Query("SELECT * FROM sync_file_state WHERE libraryId = :libraryId AND path = :path LIMIT 1")
	suspend fun load(libraryId: String, path: String): SyncFileStateEntity?

	@Upsert
	suspend fun upsert(entity: SyncFileStateEntity)

	@Query("DELETE FROM sync_file_state WHERE libraryId = :libraryId AND path = :path")
	suspend fun delete(libraryId: String, path: String)
}
