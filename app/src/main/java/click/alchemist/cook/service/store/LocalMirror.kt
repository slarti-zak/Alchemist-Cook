package click.alchemist.cook.service.store

import android.content.Context
import java.io.File

/** Per-library, app-private directory tree mirroring a library's remote WebDAV tree. */
class LocalMirror(private val context: Context) {
	fun root(libraryId: String): File =
		File(context.filesDir, "webdav/$libraryId").apply { mkdirs() }

	fun file(libraryId: String, path: String): File = File(root(libraryId), path)

	fun read(libraryId: String, path: String): ByteArray? {
		val file = file(libraryId, path)
		return if (file.isFile) file.readBytes() else null
	}

	fun write(libraryId: String, path: String, content: ByteArray) {
		val file = file(libraryId, path)
		file.parentFile?.mkdirs()
		file.writeBytes(content)
	}

	fun delete(libraryId: String, path: String) {
		val file = file(libraryId, path)
		if (file.isDirectory) file.deleteRecursively() else file.delete()
	}

	fun exists(libraryId: String, path: String): Boolean = file(libraryId, path).exists()

	fun mtime(libraryId: String, path: String): Long? {
		val file = file(libraryId, path)
		return if (file.exists()) file.lastModified() else null
	}

	/** Every regular file under [libraryId]'s root, as slash-separated paths relative to it. */
	fun listFiles(libraryId: String): List<String> {
		val rootDir = root(libraryId)
		return rootDir.walkTopDown()
			.filter { it.isFile }
			.map { it.relativeTo(rootDir).path.replace(File.separatorChar, '/') }
			.toList()
	}
}
