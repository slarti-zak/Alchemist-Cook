package click.alchemist.cook

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import click.alchemist.cook.di.createModule
import com.couchbase.lite.CouchbaseLite
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {
	override fun onCreate() {
		super.onCreate()

		// Initialize the Couchbase Lite system
		CouchbaseLite.init(this)

		startKoin {
			// Until Koin supports kotlin 1.6: https://github.com/InsertKoinIO/koin/issues/1188
			androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
			// androidLogger(Level.DEBUG)

			androidContext(this@App)
			modules(createModule(this@App))
		}

		ProcessLifecycleOwner.get().lifecycle.addObserver(ApplicationObserver())
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(if (base == null) null else LocaleHelper.onAttach(base))
	}

	companion object {
		private var intentRequestId = 0

		fun getIntentRequestId(): Int = intentRequestId++

		const val authority = BuildConfig.APPLICATION_ID + ".fileprovider"
	}
}