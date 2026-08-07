package click.alchemist.cook.ui.settings

import androidx.lifecycle.ViewModel
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import click.alchemist.cook.service.migration.CouchbaseToWebDavMigrator
import click.alchemist.cook.service.migration.MigrationResult
import click.alchemist.cook.service.store.LibraryConfig
import click.alchemist.cook.service.store.LibraryConnection
import click.alchemist.cook.service.store.LibraryManager
import click.alchemist.cook.service.store.LibraryRole
import click.alchemist.cook.service.store.SyncStatus
import click.alchemist.cook.service.store.WebDavService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map


class SettingsViewModel(
	val couchbase: CouchbaseAccountListener,
	private val libraryManager: LibraryManager,
	private val webDavService: WebDavService,
	private val migrator: CouchbaseToWebDavMigrator
) : ViewModel() {
	val syncState: Flow<CouchbaseState> = couchbase.databaseFlow.flatMapLatest { it.replicatorChanges }
	val webDavSyncStatus: StateFlow<SyncStatus> = webDavService.syncStatus

	val sharedLibraries: Flow<List<LibraryConfig>> =
		libraryManager.libraries.map { libs -> libs.filter { it.role == LibraryRole.SHARED } }

	val personalLibrary: Flow<LibraryConfig?> =
		libraryManager.libraries.map { libs -> libs.firstOrNull { it.role == LibraryRole.PERSONAL } }

	fun personalLibrary(): LibraryConfig? = libraryManager.personalLibrary()

	fun setPersonalLibrary(label: String, connection: LibraryConnection) {
		if (label.isBlank()) return
		libraryManager.setPersonalLibrary(label, connection)
	}

	fun addSharedLibrary(label: String, connection: LibraryConnection) {
		if (label.isBlank()) return
		libraryManager.addSharedLibrary(label, connection)
	}

	fun removeSharedLibrary(id: String) = libraryManager.removeLibrary(id)

	fun syncNow() = webDavService.syncNow()

	suspend fun migrateFromCouchbase(): MigrationResult? {
		val library = libraryManager.personalLibrary() ?: return null
		return migrator.migrate(library.id)
	}
}
