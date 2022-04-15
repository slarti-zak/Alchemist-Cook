package click.alchemist.cook

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ApplicationObserver : DefaultLifecycleObserver, KoinComponent {
	private val database: CouchbaseAccountListener by inject()
	private var firstCall = true

	override fun onResume(owner: LifecycleOwner) {
		if (firstCall) {
			firstCall = false
		} else {
			database.database?.refreshReplicator()
		}
	}

	override fun onDestroy(owner: LifecycleOwner) {
		database.database?.stop()
	}
}
