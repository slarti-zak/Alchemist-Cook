package click.alchemist.cook.ui.settings

import androidx.lifecycle.ViewModel
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import click.alchemist.cook.service.migration.CouchbaseToWebDavMigrator
import click.alchemist.cook.service.migration.MigrationResult
import click.alchemist.cook.service.store.LibraryConfig
import click.alchemist.cook.service.store.LibraryManager
import click.alchemist.cook.service.store.LibraryRole
import click.alchemist.cook.service.store.SyncStatus
import click.alchemist.cook.service.store.WebDavService
import click.alchemist.cook.service.webdav.WebDavConfig
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

	fun personalLibrary(): LibraryConfig? = libraryManager.personalLibrary()

	fun updatePersonalLibrary(url: String, username: String, password: String) {
		if (url.isBlank() || username.isBlank()) return
		libraryManager.setPersonalLibrary("Personal", WebDavConfig(url, username, password))
	}

	fun addSharedLibrary(label: String, url: String, username: String, password: String) {
		if (label.isBlank() || url.isBlank() || username.isBlank()) return
		libraryManager.addSharedLibrary(label, WebDavConfig(url, username, password))
	}

	fun removeSharedLibrary(id: String) = libraryManager.removeLibrary(id)

	fun syncNow() = webDavService.syncNow()

	suspend fun migrateFromCouchbase(): MigrationResult? {
		val library = libraryManager.personalLibrary() ?: return null
		return migrator.migrate(library.id)
	}
}
