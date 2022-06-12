package click.alchemist.cook.ui.settings

import androidx.lifecycle.ViewModel
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest


class SettingsViewModel(val couchbase: CouchbaseAccountListener) : ViewModel() {
	val syncState: Flow<CouchbaseState> = couchbase.databaseFlow.flatMapLatest { it.replicatorChanges }

	val account : Flow<FirebaseUser?> = callbackFlow {
		val listener = FirebaseAuth.AuthStateListener {
			this.trySend(it.currentUser)
		}
		FirebaseAuth.getInstance().addAuthStateListener(listener)
		awaitClose {
			FirebaseAuth.getInstance().removeAuthStateListener(listener)
		}
	}


}