package click.alchemist.cook.service.store

import java.io.File

/**
 * A library's local file tree. For [click.alchemist.cook.service.webdav.WebDavConfig]-backed
 * libraries (WebDAV/Nextcloud) this is an app-private staging copy reconciled against the remote by
 * [SyncEngine] (see [PrivateLocalMirror]); for a [LibraryConnection.LocalFolder] library it's the
 * user-picked folder itself, written to directly (see [SafLocalMirror]) — there's no "remote" to
 * stage against.
 */
interface LocalMirror {
	fun read(libraryId: String, path: String): ByteArray?
	fun write(libraryId: String, path: String, content: ByteArray)
	fun delete(libraryId: String, path: String)
	fun exists(libraryId: String, path: String): Boolean
	fun mtime(libraryId: String, path: String): Long?

	/** Every regular file under [libraryId]'s root, as slash-separated paths relative to it. */
	fun listFiles(libraryId: String): List<String>

	/**
	 * A directly-readable [File] for [path] — used where a plain filesystem path is required (e.g.
	 * Coil image loading), not just bytes. Always available for [PrivateLocalMirror]; for
	 * [SafLocalMirror] this materializes an on-demand cache copy, since a SAF tree has no
	 * `java.io.File` of its own.
	 */
	fun file(libraryId: String, path: String): File?
}

/** App-private directory tree, one per library, used as WebDAV/Nextcloud's local staging copy. */
class PrivateLocalMirror(private val context: android.content.Context) : LocalMirror {
	private fun root(libraryId: String): File =
		File(context.filesDir, "webdav/$libraryId").apply { mkdirs() }

	override fun file(libraryId: String, path: String): File = File(root(libraryId), path)

	override fun read(libraryId: String, path: String): ByteArray? {
		val file = file(libraryId, path)
		return if (file.isFile) file.readBytes() else null
	}

	override fun write(libraryId: String, path: String, content: ByteArray) {
		val file = file(libraryId, path)
		file.parentFile?.mkdirs()
		file.writeBytes(content)
	}

	override fun delete(libraryId: String, path: String) {
		val file = file(libraryId, path)
		if (file.isDirectory) file.deleteRecursively() else file.delete()
	}

	override fun exists(libraryId: String, path: String): Boolean = file(libraryId, path).exists()

	override fun mtime(libraryId: String, path: String): Long? {
		val file = file(libraryId, path)
		return if (file.exists()) file.lastModified() else null
	}

	override fun listFiles(libraryId: String): List<String> {
		val rootDir = root(libraryId)
		return rootDir.walkTopDown()
			.filter { it.isFile }
			.map { it.relativeTo(rootDir).path.replace(File.separatorChar, '/') }
			.toList()
	}
}
