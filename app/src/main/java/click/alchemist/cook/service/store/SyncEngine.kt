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
 * Reconciles each library with its remote/canonical storage. For a [LibraryConnection.WebDav]/
 * [LibraryConnection.Nextcloud] library that means [privateMirror] against the WebDAV server: pulls
 * new/changed remote files, pushes new/changed local files, propagates deletions, and keeps both
 * sides of a conflicting change by writing the losing version to a sibling `*.conflict-<timestamp>`
 * file rather than discarding it. WebDAV has no push notifications, so this is invoked
 * periodically/on write/on resume rather than running continuously (see `WebDavSyncWork`).
 *
 * For a [LibraryConnection.LocalFolder] library there's no remote to pull/push against — [safMirror]
 * *is* the canonical storage, already kept current by direct writes (see [WebDavService]) — so this
 * only has to notice edits made to that folder from *outside* the app (another app, a sync client)
 * and reindex accordingly; there's no conflict case, since there's only ever one copy of the data.
 */
class SyncEngine(
	private val privateMirror: LocalMirror,
	private val safMirror: LocalMirror,
	private val database: AppDatabase,
	private val indexer: FileIndexer,
	private val clientFactory: (LibraryConfig) -> WebDavClient = { WebDavClient(requireNotNull(it.connection.webDavConfig)) }
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
			error = syncLibrary(library, scope = null) ?: error
		}
		_status.value = if (error == null) SyncStatus.Idle else SyncStatus.Error(error)
		return error == null
	}

	/**
	 * Syncs a single library, never throwing — a failure surfaces via [status], not an exception.
	 *
	 * [scope], when given, is a file or folder path (e.g. a single recipe's or shopping list's
	 * folder) that limits the reconcile to that subtree instead of walking the whole library — the
	 * normal case, since a single edit only ever touches one entity's files. Pass null (the default)
	 * for a full-library reconcile — "Sync now", the periodic background job, and anything else that
	 * needs to pick up out-of-band remote changes anywhere in the tree.
	 */
	suspend fun sync(library: LibraryConfig, scope: String? = null) {
		_status.value = SyncStatus.Syncing
		val error = syncLibrary(library, scope)
		_status.value = if (error == null) SyncStatus.Idle else SyncStatus.Error(error)
	}

	/** Returns an error message on failure, or null on success. Callers rely on this never throwing. */
	private suspend fun syncLibrary(library: LibraryConfig, scope: String?): String? = when (library.connection) {
		is LibraryConnection.WebDav, is LibraryConnection.Nextcloud -> syncWebDavLibrary(library, scope)
		// A local-folder library is already fully local — there's no remote round trip to scope down,
		// so a "single entity" sync request just does the (cheap, network-free) full rescan.
		is LibraryConnection.LocalFolder -> rescanLocalFolderLibrary(library)
	}

	/**
	 * Serialized per-library via [lockFor]: a second call for the same library blocks until the first
	 * finishes instead of running concurrently against it (see [libraryLocks]).
	 */
	private suspend fun syncWebDavLibrary(library: LibraryConfig, scope: String?): String? = lockFor(library.id).withLock {
		try {
			val client = clientFactory(library)
			deletePendingFolders(library, client)
			val remoteFiles = client.propfindRecursive(scope.orEmpty()).filterNot { it.isCollection }
				.filter { EntityPaths.isSynced(it.path) }
				.associateBy { it.path }
			val localPaths = privateMirror.listFiles(library.id).filter { EntityPaths.isSynced(it) }
				.filter { inScope(it, scope) }.toSet()
			val knownState = database.syncFileStateDao().loadAll(library.id)
				.filter { inScope(it.path, scope) }.associateBy { it.path }

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
	 * Whether [path] falls under [scope] (null meaning "everything"). Checked against the full path
	 * segment, not a raw [String.startsWith] — otherwise folder `recipes/curry-ab1234567d` would wrongly
	 * swallow an unrelated sibling like `recipes/curry-ab1234567d-deluxe-<id>` just because its name
	 * happens to start with the same characters.
	 */
	private fun inScope(path: String, scope: String?): Boolean =
		scope == null || path == scope || path.startsWith("$scope/")

	/**
	 * A SAF folder can change from outside the app, so this walks it and reindexes anything whose
	 * mtime has moved since the last known state (or that's new/gone). There's no push/pull here —
	 * [safMirror] is already the canonical copy, kept current by direct writes elsewhere.
	 */
	private suspend fun rescanLocalFolderLibrary(library: LibraryConfig): String? = lockFor(library.id).withLock {
		try {
			val currentPaths = safMirror.listFiles(library.id).filter { EntityPaths.isSynced(it) }
			val currentMtimes = currentPaths.associateWith { safMirror.mtime(library.id, it) }.filterValues { it != null }.mapValues { it.value!! }
			val knownMtimes = database.syncFileStateDao().loadAll(library.id).associate { it.path to it.localMtime }

			for (change in LocalFolderDiff.diff(currentMtimes, knownMtimes)) {
				when (change) {
					is LocalFolderChange.Remove -> {
						indexer.onFileRemoved(library.id, change.path)
						database.syncFileStateDao().delete(library.id, change.path)
					}

					is LocalFolderChange.Reindex -> {
						val bytes = safMirror.read(library.id, change.path) ?: continue
						indexer.onFileChanged(library.id, change.path, bytes)
						database.syncFileStateDao().upsert(
							SyncFileStateEntity(library.id, change.path, remoteEtag = null, remoteLastModified = null, localMtime = change.mtime, isDirectory = false)
						)
					}
				}
			}
			null
		} catch (e: Exception) {
			logError(TAG, "Local folder rescan failed for library ${library.id}", e)
			e.message ?: "Sync failed"
		}
	}

	/**
	 * Removes collections [WebDavService.removeFolder][click.alchemist.cook.service.store.WebDavService]
	 * already dropped locally (a delete, or the stale side of a rename) — [reconcile] below only ever
	 * diffs individual files, so without this the now-empty directory would stay on the server forever.
	 * Run before the per-file diff so it doesn't waste round trips reconciling files about to vanish
	 * anyway. A failed delete is logged and left pending, retried on the next sync, rather than failing
	 * the whole library sync over one stale folder. Local-folder libraries never queue these — see
	 * [WebDavService.removeFolder], which deletes them synchronously instead.
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
		val localMtime = if (localExists) privateMirror.mtime(library.id, path) else null
		val localChanged = localExists && localMtime != known?.localMtime

		logDebug(TAG, "reconcile $path: remote=${remote != null} local=$localExists remoteChanged=$remoteChanged localChanged=$localChanged")

		when {
			remote == null && !localExists ->
				known?.let { database.syncFileStateDao().delete(library.id, path) }

			// Deleted remotely, untouched locally since the last sync -> follow the deletion.
			remote == null && localExists && !localChanged -> {
				privateMirror.delete(library.id, path)
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
		privateMirror.write(library.id, path, bytes)
		rememberState(library, path, remote.etag, remote.lastModified)
		indexer.onFileChanged(library.id, path, bytes)
	}

	private suspend fun push(library: LibraryConfig, client: WebDavClient, path: String) {
		val bytes = privateMirror.read(library.id, path) ?: return
		parentPath(path).takeIf { it.isNotBlank() }?.let { client.mkcolRecursive(it) }
		client.put(path, bytes, contentTypeFor(path))
		val remote = client.propfind(parentPath(path), depth = 1).firstOrNull { it.path == path }
		rememberState(library, path, remote?.etag, remote?.lastModified)
		indexer.onFileChanged(library.id, path, bytes)
	}

	private suspend fun resolveConflict(library: LibraryConfig, client: WebDavClient, path: String) {
		val remoteBytes = client.get(path)
		val conflictPath = conflictPathFor(path)

		privateMirror.write(library.id, conflictPath, remoteBytes)
		parentPath(conflictPath).takeIf { it.isNotBlank() }?.let { client.mkcolRecursive(it) }
		client.put(conflictPath, remoteBytes, contentTypeFor(conflictPath))
		val conflictRemote = client.propfind(parentPath(conflictPath), depth = 1).firstOrNull { it.path == conflictPath }
		rememberState(library, conflictPath, conflictRemote?.etag, conflictRemote?.lastModified)
		indexer.onFileChanged(library.id, conflictPath, remoteBytes)

		// The local edit stays canonical at `path`; push it so the remote catches up.
		push(library, client, path)
	}

	private suspend fun rememberState(library: LibraryConfig, path: String, remoteEtag: String?, remoteLastModified: Long?) {
		val localMtime = privateMirror.mtime(library.id, path) ?: System.currentTimeMillis()
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
