package click.alchemist.cook.ui.settings

import androidx.lifecycle.ViewModel
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@ExperimentalCoroutinesApi
class SettingsViewModel(val couchbase: CouchbaseAccountListener) : ViewModel() {
	val syncState: Flow<CouchbaseState> = couchbase.databaseFlow.flatMapLatest { it.replicatorChanges }
}