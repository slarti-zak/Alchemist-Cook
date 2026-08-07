package click.alchemist.cook.service.store

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * A [LocalFolder][LibraryConnection.LocalFolder] library's canonical storage: the user-picked SAF
 * tree itself, addressed through [DocumentFile] rather than `java.io.File` (a `content://` tree has
 * no path on disk). Unlike [PrivateLocalMirror] this isn't a staging copy — every call here reads or
 * writes the real folder directly, so a write is already "synced" with nothing further to reconcile
 * beyond noticing edits made from outside the app (see `SyncEngine`'s local-folder branch).
 */
class SafLocalMirror(
	private val context: Context,
	private val libraryManager: LibraryManager
) : LocalMirror {

	private fun root(libraryId: String): DocumentFile {
		val connection = libraryManager.current().firstOrNull { it.id == libraryId }?.connection
		val treeUri = (connection as? LibraryConnection.LocalFolder)?.treeUri
			?: error("Library $libraryId has no local folder configured")
		return DocumentFile.fromTreeUri(context, treeUri.toUri())
			?: error("Cannot open local folder for library $libraryId (permission revoked?)")
	}

	private fun segments(path: String) = path.split('/').filter { it.isNotBlank() }

	/** Walks to the [DocumentFile] at [path], or null if any segment along the way doesn't exist. */
	private fun resolve(libraryId: String, path: String): DocumentFile? {
		var current = root(libraryId)
		for (segment in segments(path)) {
			current = current.findFile(segment) ?: return null
		}
		return current
	}

	private fun resolveParent(libraryId: String, path: String): DocumentFile {
		var current = root(libraryId)
		val parentSegments = segments(path).dropLast(1)
		for (segment in parentSegments) {
			current = current.findFile(segment) ?: current.createDirectory(segment)
				?: error("Cannot create folder $segment")
		}
		return current
	}

	override fun read(libraryId: String, path: String): ByteArray? {
		val doc = resolve(libraryId, path)?.takeIf { it.isFile } ?: return null
		return context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() }
	}

	override fun write(libraryId: String, path: String, content: ByteArray) {
		val fileName = segments(path).last()
		val parent = resolveParent(libraryId, path)
		val target = parent.findFile(fileName)
			?: parent.createFile("application/octet-stream", fileName)
			?: error("Cannot create $path")
		context.contentResolver.openOutputStream(target.uri, "wt")?.use { it.write(content) }
			?: error("Cannot open $path for writing")
	}

	override fun delete(libraryId: String, path: String) {
		resolve(libraryId, path)?.delete()
	}

	override fun exists(libraryId: String, path: String): Boolean = resolve(libraryId, path) != null

	override fun mtime(libraryId: String, path: String): Long? = resolve(libraryId, path)?.lastModified()

	override fun listFiles(libraryId: String): List<String> {
		val result = mutableListOf<String>()
		fun walk(dir: DocumentFile, prefix: String) {
			for (child in dir.listFiles()) {
				val name = child.name ?: continue
				val childPath = if (prefix.isEmpty()) name else "$prefix/$name"
				if (child.isDirectory) walk(child, childPath) else if (child.isFile) result.add(childPath)
			}
		}
		walk(root(libraryId), "")
		return result
	}

	/** No `java.io.File` backs a SAF tree, so this materializes an on-demand cache copy instead. */
	override fun file(libraryId: String, path: String): File? {
		val bytes = read(libraryId, path) ?: return null
		val cacheDir = File(context.cacheDir, "local-library/$libraryId").apply { mkdirs() }
		val cacheFile = File(cacheDir, segments(path).last())
		cacheFile.writeBytes(bytes)
		return cacheFile
	}
}
