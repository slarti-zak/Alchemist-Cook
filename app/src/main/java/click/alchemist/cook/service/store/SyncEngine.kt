package click.alchemist.cook.service.store

import click.alchemist.cook.logDebug
import click.alchemist.cook.logError
import click.alchemist.cook.service.store.index.AppDatabase
import click.alchemist.cook.service.store.index.SyncFileStateEntity
import click.alchemist.cook.service.webdav.WebDavClient
import click.alchemist.cook.service.webdav.WebDavResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

sealed class SyncStatus {
	data object Idle : SyncStatus()
	data object Syncing : SyncStatus()
	data class Error(val message: String) : SyncStatus()
}

/**
 * Reconciles each library's local mirror with its WebDAV remote: pulls new/changed remote files,
 * pushes new/changed local files, propagates deletions, and keeps both sides of a conflicting
 * change by writing the losing version to a sibling `*.conflict-<timestamp>` file rather than
 * discarding it. WebDAV has no push notifications, so this is invoked periodically/on write/on
 * resume rather than running continuously (see `WebDavSyncWork`).
 */
class SyncEngine(
	private val localMirror: LocalMirror,
	private val database: AppDatabase,
	private val indexer: FileIndexer,
	private val clientFactory: (LibraryConfig) -> WebDavClient = { WebDavClient(it.webDav) }
) {
	private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
	val status: StateFlow<SyncStatus> = _status.asStateFlow()

	/**
	 * "Sync now", app-resume, the once-a-minute foreground loop, and the periodic WorkManager job can
	 * all reach [sync]/[syncAll] independently, with no guarantee any two are mutually exclusive in
	 * time. Without a lock here, two of them racing the same library would both read the same
	 * knownState/local files and could interleave pulls/pushes/deletes against it. One [Mutex] per
	 * library (not a single global one) so unrelated libraries still sync concurrently.
	 */
	private val libraryLocks = ConcurrentHashMap<String, Mutex>()

	private fun lockFor(libraryId: String): Mutex = libraryLocks.getOrPut(libraryId) { Mutex() }

	/** Syncs every library, never throwing. Returns true iff all of them synced without error. */
	suspend fun syncAll(libraries: List<LibraryConfig>): Boolean {
		_status.value = SyncStatus.Syncing
		var error: String? = null
		for (library in libraries) {
			error = syncLibrary(library) ?: error
		}
		_status.value = if (error == null) SyncStatus.Idle else SyncStatus.Error(error)
		return error == null
	}

	/** Syncs a single library, never throwing — a WebDAV/server failure surfaces via [status], not an exception. */
	suspend fun sync(library: LibraryConfig) {
		_status.value = SyncStatus.Syncing
		val error = syncLibrary(library)
		_status.value = if (error == null) SyncStatus.Idle else SyncStatus.Error(error)
	}

	/**
	 * Returns an error message on failure, or null on success. Callers rely on this never throwing.
	 * Serialized per-library via [lockFor]: a second call for the same library blocks until the first
	 * finishes instead of running concurrently against it (see [libraryLocks]).
	 */
	private suspend fun syncLibrary(library: LibraryConfig): String? = lockFor(library.id).withLock {
		try {
			val client = clientFactory(library)
			deletePendingFolders(library, client)
			val remoteFiles = client.propfindRecursive().filterNot { it.isCollection }
				.filter { EntityPaths.isSynced(it.path) }
				.associateBy { it.path }
			val localPaths = localMirror.listFiles(library.id).filter { EntityPaths.isSynced(it) }.toSet()
			val knownState = database.syncFileStateDao().loadAll(library.id).associateBy { it.path }

			val allPaths = remoteFiles.keys + localPaths + knownState.keys
			for (path in allPaths) {
				reconcile(library, client, path, remoteFiles[path], localPaths.contains(path), knownState[path])
			}
			null
		} catch (e: Exception) {
			logError(TAG, "Sync failed for library ${library.id}", e)
			e.message ?: "Sync failed"
		}
	}

	/**
	 * Removes collections [WebDavService.removeFolder][click.alchemist.cook.service.store.WebDavService]
	 * already dropped locally (a delete, or the stale side of a rename) — [reconcile] below only ever
	 * diffs individual files, so without this the now-empty directory would stay on the server forever.
	 * Run before the per-file diff so it doesn't waste round trips reconciling files about to vanish
	 * anyway. A failed delete is logged and left pending, retried on the next sync, rather than failing
	 * the whole library sync over one stale folder.
	 */
	private suspend fun deletePendingFolders(library: LibraryConfig, client: WebDavClient) {
		for (pending in database.pendingFolderDeletionDao().loadAll(library.id)) {
			try {
				client.delete(pending.path)
				database.pendingFolderDeletionDao().delete(library.id, pending.path)
			} catch (e: Exception) {
				logError(TAG, "Could not delete remote folder ${pending.path}", e)
			}
		}
	}

	private suspend fun reconcile(
		library: LibraryConfig,
		client: WebDavClient,
		path: String,
		remote: WebDavResource?,
		localExists: Boolean,
		known: SyncFileStateEntity?
	) {
		val remoteChanged = remote != null && remote.etag != known?.remoteEtag
		val localMtime = if (localExists) localMirror.mtime(library.id, path) else null
		val localChanged = localExists && localMtime != known?.localMtime

		logDebug(TAG, "reconcile $path: remote=${remote != null} local=$localExists remoteChanged=$remoteChanged localChanged=$localChanged")

		when {
			remote == null && !localExists ->
				known?.let { database.syncFileStateDao().delete(library.id, path) }

			// Deleted remotely, untouched locally since the last sync -> follow the deletion.
			remote == null && localExists && !localChanged -> {
				localMirror.delete(library.id, path)
				indexer.onFileRemoved(library.id, path)
				database.syncFileStateDao().delete(library.id, path)
			}

			// Deleted remotely but edited locally since -> local edit wins, re-push it.
			remote == null && localExists && localChanged -> push(library, client, path)

			// Brand new remote file we've never seen -> pull.
			remote != null && !localExists && known == null -> pull(library, client, path, remote)

			// We knew about it before and it's gone locally now -> it was deleted locally, propagate.
			remote != null && !localExists && known != null -> {
				client.delete(path)
				indexer.onFileRemoved(library.id, path)
				database.syncFileStateDao().delete(library.id, path)
			}

			remote != null && localExists && !remoteChanged && !localChanged -> Unit // Nothing to do.
			remote != null && localExists && remoteChanged && !localChanged -> pull(library, client, path, remote)
			remote != null && localExists && !remoteChanged && localChanged -> push(library, client, path)
			remote != null && localExists && remoteChanged && localChanged -> resolveConflict(library, client, path)
		}
	}

	private suspend fun pull(library: LibraryConfig, client: WebDavClient, path: String, remote: WebDavResource) {
		val bytes = client.get(path)
		localMirror.write(library.id, path, bytes)
		rememberState(library, path, remote.etag, remote.lastModified)
		indexer.onFileChanged(library.id, path, bytes)
	}

	private suspend fun push(library: LibraryConfig, client: WebDavClient, path: String) {
		val bytes = localMirror.read(library.id, path) ?: return
		parentPath(path).takeIf { it.isNotBlank() }?.let { client.mkcolRecursive(it) }
		client.put(path, bytes, contentTypeFor(path))
		val remote = client.propfind(parentPath(path), depth = 1).firstOrNull { it.path == path }
		rememberState(library, path, remote?.etag, remote?.lastModified)
		indexer.onFileChanged(library.id, path, bytes)
	}

	private suspend fun resolveConflict(library: LibraryConfig, client: WebDavClient, path: String) {
		val remoteBytes = client.get(path)
		val conflictPath = conflictPathFor(path)

		localMirror.write(library.id, conflictPath, remoteBytes)
		parentPath(conflictPath).takeIf { it.isNotBlank() }?.let { client.mkcolRecursive(it) }
		client.put(conflictPath, remoteBytes, contentTypeFor(conflictPath))
		val conflictRemote = client.propfind(parentPath(conflictPath), depth = 1).firstOrNull { it.path == conflictPath }
		rememberState(library, conflictPath, conflictRemote?.etag, conflictRemote?.lastModified)
		indexer.onFileChanged(library.id, conflictPath, remoteBytes)

		// The local edit stays canonical at `path`; push it so the remote catches up.
		push(library, client, path)
	}

	private suspend fun rememberState(library: LibraryConfig, path: String, remoteEtag: String?, remoteLastModified: Long?) {
		val localMtime = localMirror.mtime(library.id, path) ?: System.currentTimeMillis()
		database.syncFileStateDao().upsert(
			SyncFileStateEntity(library.id, path, remoteEtag, remoteLastModified, localMtime, isDirectory = false)
		)
	}

	private fun conflictPathFor(path: String): String {
		val timestamp = System.currentTimeMillis()
		val dot = path.lastIndexOf('.')
		val slash = path.lastIndexOf('/')
		return if (dot > slash) "${path.substring(0, dot)}.conflict-$timestamp${path.substring(dot)}"
		else "$path.conflict-$timestamp"
	}

	private fun parentPath(path: String): String = path.substringBeforeLast('/', missingDelimiterValue = "")

	private fun contentTypeFor(path: String): String = when {
		path.endsWith(".md") -> "text/markdown"
		path.endsWith(".yaml") || path.endsWith(".yml") -> "application/yaml"
		path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
		path.endsWith(".png") -> "image/png"
		else -> "application/octet-stream"
	}

	companion object {
		private const val TAG = "SyncEngine"
	}
}
