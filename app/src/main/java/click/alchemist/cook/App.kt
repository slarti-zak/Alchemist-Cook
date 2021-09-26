package click.alchemist.cook

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import click.alchemist.cook.di.createModule
import com.couchbase.lite.CouchbaseLite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {
    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()

        // Initialize the Couchbase Lite system
        CouchbaseLite.init(this)

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@App)
            modules(createModule(this@App))
        }

        ProcessLifecycleOwner.get().lifecycle
            .addObserver(ApplicationObserver())
    }

    companion object {
        private var intentRequestId = 0

        fun getIntentRequestId(): Int = intentRequestId++

        const val authority = BuildConfig.APPLICATION_ID + ".fileprovider"
    }
}