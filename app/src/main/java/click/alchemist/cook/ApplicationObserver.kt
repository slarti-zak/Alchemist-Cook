package click.alchemist.cook

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class ApplicationObserver : LifecycleObserver, KoinComponent {
	private val database: CouchbaseAccountListener by inject()
	private var firstCall = true

	@OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
	fun onResume() {
		if (firstCall) {
			firstCall = false
		} else {
			database.database?.refreshReplicator()
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	fun onDestroy() {
		database.database?.stop()
	}
}
