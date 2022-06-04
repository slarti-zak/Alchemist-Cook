package click.alchemist.cook

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ApplicationObserver : DefaultLifecycleObserver, KoinComponent {
	private val database: CouchbaseAccountListener by inject()

	override fun onPause(owner: LifecycleOwner) {
		database.database?.pause()
	}

	override fun onResume(owner: LifecycleOwner) {
		database.database?.resume()
	}

	override fun onDestroy(owner: LifecycleOwner) {
		database.database?.stop()
	}
}
