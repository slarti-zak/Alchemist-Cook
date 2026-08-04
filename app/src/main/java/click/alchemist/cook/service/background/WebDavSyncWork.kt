package click.alchemist.cook.service.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import click.alchemist.cook.logError
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

		return try {
			syncEngine.syncAll(libraries)
			logInfo(TAG, "Synced ${libraries.size} librar${if (libraries.size == 1) "y" else "ies"}")
			Result.success()
		} catch (e: Exception) {
			logError(TAG, "Sync failed", e)
			Result.retry()
		}
	}

	companion object {
		private const val TAG = "WebDavSync"
	}
}
