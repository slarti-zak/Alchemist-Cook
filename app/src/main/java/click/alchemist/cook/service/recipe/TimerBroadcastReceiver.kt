package click.alchemist.cook.service.recipe

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.service.couchbase.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class TimerBroadcastReceiver : BroadcastReceiver(), KoinComponent {
	override fun onReceive(context: Context, intent: Intent) {
		// TimerRepository now goes through suspend WebDAV/Room calls, so onReceive (which runs on the
		// main thread and has its own execution-time budget) can't just call it inline anymore.
		// goAsync() keeps the receiver alive past onReceive returning, so the coroutine can finish its
		// work on a background dispatcher instead of blocking here.
		val pendingResult = goAsync()
		CoroutineScope(Dispatchers.IO).launch {
			try {
				when (intent.action) {
					actionDelete -> deleteTimer(intent)
					actionAddMinute -> addMinute(intent)
					actionTimeout -> timeoutTimer(context, intent)
				}
			} catch (e: Exception) {
				logError("TimerBroadcastReceiver", "Error handling intent ${intent.action}", e)
			} finally {
				pendingResult.finish()
			}
		}
	}

	private suspend fun deleteTimer(intent: Intent) {
		val id = intent.getStringExtra(timerEntityId) ?: return
		logInfo("TimerBroadcastReceiver", "Delete timer for $id")
		val timerRepository: TimerRepository by inject()

		timerRepository.delete(id)
	}

	private suspend fun addMinute(intent: Intent) {
		val id = intent.getStringExtra(timerEntityId) ?: return
		logInfo("TimerBroadcastReceiver", "Add minute for $id")
		val timerRepository: TimerRepository by inject()

		val timer = timerRepository.load(id) ?: return
		timerRepository.addMinute(timer)
	}

	private suspend fun timeoutTimer(context: Context, intent: Intent) {
		val id = intent.getStringExtra(timerEntityId) ?: return
		val requestId = intent.getIntExtra(timerNotificationId, -1)
		logInfo("TimerBroadcastReceiver", "Timeout for id $id")

		val timerRepository: TimerRepository by inject()
		val timer = timerRepository.load(id) ?: return

		createNotification(context, timer, requestId)
		logInfo("TimerBroadcastReceiver", "Created elapsed notification for $id")
	}

	private fun createNotification(context: Context, timer: RunningTimer, requestId: Int) {
		val notification = TimerNotificationHelper(context).createElapsedNotification(timer)

		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			logInfo("TimerBroadcastReceiver", "Cannot show notification as no permission granted")
			return
		}
		NotificationManagerCompat.from(context).notify(
			AlarmManagerTimerService.timerNotificationTag,
			requestId,
			notification
		)
	}

	companion object {
		const val actionAddMinute = "click.alchemist.cook.AddMinute"
		const val actionDelete = "click.alchemist.cook.DeleteTimer"
		const val actionTimeout = "click.alchemist.cook.Timeout"

		const val timerEntityId = "id"
		const val timerNotificationId = "notificationId"
	}
}
