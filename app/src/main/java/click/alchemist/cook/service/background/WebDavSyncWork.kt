package click.alchemist.cook.service.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import click.alchemist.cook.logInfo
import click.alchemist.cook.service.store.LibraryManager
import click.alchemist.cook.service.store.SyncEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Periodic WorkManager job that reconciles every configured library with its WebDAV server (see [SyncEngine]). */
class WebDavSyncWork(
	appContext: Context,
	workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

	private val libraryManager: LibraryManager by inject()
	private val syncEngine: SyncEngine by inject()

	override suspend fun doWork(): Result {
		val libraries = libraryManager.current()
		if (libraries.isEmpty()) {
			logInfo(TAG, "Not syncing, no libraries configured")
			return Result.success()
		}

		// syncAll() never throws — a per-library failure is logged and reflected in its return value/SyncStatus instead.
		val allOk = syncEngine.syncAll(libraries)
		logInfo(TAG, "Synced ${libraries.size} librar${if (libraries.size == 1) "y" else "ies"}, allOk=$allOk")
		return if (allOk) Result.success() else Result.retry()
	}

	companion object {
		private const val TAG = "WebDavSync"
	}
}
