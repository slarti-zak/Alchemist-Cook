package click.alchemist.cook.service.store

import click.alchemist.cook.service.settings.AndroidSettings
import click.alchemist.cook.service.webdav.WebDavConfig
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val PERSONAL_LIBRARY_ID = "personal"
private const val LIBRARIES_KEY = "webdav_libraries"
private const val EMPTY = "[]"

/** CRUD over configured [LibraryConfig]s (the one personal library, plus any shared ones), persisted via [AndroidSettings]. */
class LibraryManager(private val settings: AndroidSettings) {

	val libraries: Flow<List<LibraryConfig>> = settings.register(LIBRARIES_KEY, EMPTY).map(::deserialize)

	fun current(): List<LibraryConfig> = deserialize(settings.getString(LIBRARIES_KEY, EMPTY) ?: EMPTY)

	fun personalLibrary(): LibraryConfig? = current().firstOrNull { it.role == LibraryRole.PERSONAL }

	fun setPersonalLibrary(label: String, webDav: WebDavConfig) {
		val others = current().filterNot { it.role == LibraryRole.PERSONAL }
		save(listOf(LibraryConfig(PERSONAL_LIBRARY_ID, label, LibraryRole.PERSONAL, webDav)) + others)
	}

	fun addSharedLibrary(label: String, webDav: WebDavConfig): LibraryConfig {
		val library = LibraryConfig(id = EntityPaths.newId(), label = label, role = LibraryRole.SHARED, webDav = webDav)
		save(current() + library)
		return library
	}

	fun removeLibrary(id: String) {
		save(current().filterNot { it.id == id })
	}

	private fun save(libraries: List<LibraryConfig>) {
		settings.putString(LIBRARIES_KEY, YamlMapper.instance.writeValueAsString(libraries))
	}

	private fun deserialize(raw: String): List<LibraryConfig> {
		if (raw.isBlank() || raw == EMPTY) return emptyList()
		return try {
			YamlMapper.instance.readValue(raw)
		} catch (e: Exception) {
			emptyList()
		}
	}
}
