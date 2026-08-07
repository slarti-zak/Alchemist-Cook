package click.alchemist.cook.service.store

import click.alchemist.cook.service.settings.AndroidSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val PERSONAL_LIBRARY_ID = "personal"
private const val LIBRARIES_KEY = "webdav_libraries"

/** CRUD over configured [LibraryConfig]s (the one personal library, plus any shared ones), persisted via [AndroidSettings]. */
class LibraryManager(private val settings: AndroidSettings) {

	val libraries: Flow<List<LibraryConfig>> =
		settings.register(LIBRARIES_KEY, LibraryConfigSerializer.EMPTY).map(LibraryConfigSerializer::deserialize)

	fun current(): List<LibraryConfig> =
		LibraryConfigSerializer.deserialize(settings.getString(LIBRARIES_KEY, LibraryConfigSerializer.EMPTY) ?: LibraryConfigSerializer.EMPTY)

	fun personalLibrary(): LibraryConfig? = current().firstOrNull { it.role == LibraryRole.PERSONAL }

	fun setPersonalLibrary(label: String, connection: LibraryConnection) {
		val others = current().filterNot { it.role == LibraryRole.PERSONAL }
		save(listOf(LibraryConfig(PERSONAL_LIBRARY_ID, label, LibraryRole.PERSONAL, connection)) + others)
	}

	fun addSharedLibrary(label: String, connection: LibraryConnection): LibraryConfig {
		val library = LibraryConfig(id = EntityPaths.newId(), label = label, role = LibraryRole.SHARED, connection = connection)
		save(current() + library)
		return library
	}

	fun removeLibrary(id: String) {
		save(current().filterNot { it.id == id })
	}

	private fun save(libraries: List<LibraryConfig>) {
		settings.putString(LIBRARIES_KEY, LibraryConfigSerializer.serialize(libraries))
	}
}
