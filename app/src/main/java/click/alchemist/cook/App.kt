package click.alchemist.cook

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import click.alchemist.cook.coil.CoilBlobFetcherFactory
import click.alchemist.cook.coil.CoilBlobKeyer
import click.alchemist.cook.di.createModule
import coil.Coil
import coil.ImageLoader
import com.couchbase.lite.CouchbaseLite
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
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

		val imageLoader = ImageLoader.Builder(this)
			.components {
				add(CoilBlobKeyer())
				add(CoilBlobFetcherFactory(this@App))
			}
			.build()
		Coil.setImageLoader(imageLoader)

		Firebase.firestore.firestoreSettings = firestoreSettings {
			isPersistenceEnabled = true
			cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
		}

		ProcessLifecycleOwner.get().lifecycle.addObserver(ApplicationObserver())
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(if (base == null) base else LocaleHelper.onAttach(base))
	}

	companion object {
		private var intentRequestId = 0

		fun getIntentRequestId(): Int = intentRequestId++

		const val authority = BuildConfig.APPLICATION_ID + ".fileprovider"
	}
}