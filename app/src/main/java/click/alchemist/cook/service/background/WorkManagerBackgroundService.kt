package click.alchemist.cook.service.background

import android.content.Context
import androidx.work.*
import click.alchemist.cook.BuildConfig
import click.alchemist.cook.logInfo
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class WorkManagerBackgroundService(
	context: Context
) : BackgroundService {
	private val workManager: WorkManager = WorkManager.getInstance(context)
	private val periodicSyncTimer = Duration.days(1)

	override fun cancelSyncWorker() {
		workManager.cancelUniqueWork(workName)
	}

	override fun startSyncWorker() {
		if (BuildConfig.couchbaseSyncUrl.isBlank()) {
			logInfo("Not starting couchbase sync. No URL given.")
		}

		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val interval = periodicSyncTimer.inWholeMilliseconds.coerceAtLeast(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
		val work = PeriodicWorkRequestBuilder<SyncWork>(interval, TimeUnit.MILLISECONDS)
			.setConstraints(constraints)
			.setInitialDelay(10, TimeUnit.MINUTES)
			.build()

		workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, work)
	}

	companion object {
		private const val workName = "BackgroundSync"
	}
}