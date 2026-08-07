package click.alchemist.cook.service.store

internal sealed class LocalFolderChange {
	/** New or changed since the last rescan — [mtime] is what should be remembered as of this reindex. */
	data class Reindex(val path: String, val mtime: Long) : LocalFolderChange()
	data class Remove(val path: String) : LocalFolderChange()
}

/**
 * Pure diff between what's currently on disk in a [LibraryConnection.LocalFolder] library and what
 * [SyncEngine] last recorded for it, isolated from `SafLocalMirror`/Room/`FileIndexer` so this — the
 * actual "what changed" decision — can be unit-tested without any of them. There's no conflict case
 * here (unlike the WebDAV reconcile): a local folder only ever has one copy of the data.
 */
internal object LocalFolderDiff {
	fun diff(currentMtimes: Map<String, Long>, knownMtimes: Map<String, Long>): List<LocalFolderChange> {
		val changes = mutableListOf<LocalFolderChange>()
		for (path in currentMtimes.keys + knownMtimes.keys) {
			val mtime = currentMtimes[path]
			val knownMtime = knownMtimes[path]
			when {
				mtime == null -> changes += LocalFolderChange.Remove(path)
				mtime != knownMtime -> changes += LocalFolderChange.Reindex(path, mtime)
				else -> Unit // Unchanged since the last rescan.
			}
		}
		return changes
	}
}
