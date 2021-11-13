package click.alchemist.cook.ui.settings

import androidx.lifecycle.ViewModel
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest


class SettingsViewModel(val couchbase: CouchbaseAccountListener) : ViewModel() {
	val syncState: Flow<CouchbaseState> = couchbase.databaseFlow.flatMapLatest { it.replicatorChanges }
}