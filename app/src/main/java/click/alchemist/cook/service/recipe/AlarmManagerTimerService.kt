package click.alchemist.cook.service.recipe

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import click.alchemist.cook.App
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.service.settings.AndroidSettings
import click.alchemist.cook.service.store.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn


class AlarmManagerTimerService(
	private val context: Context,
	private val timerRepository: TimerRepository,
	private val androidSettings: AndroidSettings
) : TimerService {
	private val alarmManager = context.getSystemService<AlarmManager>() ?: throw Exception()
	private val notificationManager = NotificationManagerCompat.from(context)
	private val notificationHelper = TimerNotificationHelper(context)
	private val checkTimers = MutableStateFlow(TimerTrigger())

	init {
		checkTimers.combine(timerRepository.live()) { _, timers ->
			onTimersUpdated(timers)
		}.launchIn(CoroutineScope(Dispatchers.IO))

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			notificationManager.deleteNotificationChannel(notificationChannelRunningTimerId)
			notificationManager.deleteNotificationChannel(notificationChannelFinishedTimerId)

			notificationManager.createNotificationChannel(
				NotificationChannel(
					notificationChannelRunningTimerId,
					notificationChannelRunningTimerId,
					NotificationManager.IMPORTANCE_LOW
				)
			)
			notificationManager.createNotificationChannel(
				NotificationChannel(
					notificationChannelFinishedTimerId,
					notificationChannelFinishedTimerId,
					NotificationManager.IMPORTANCE_HIGH
				).apply {
					getNotificationSound()?.let {
						val audioAttributes = AudioAttributes.Builder()
							.setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
							.setUsage(AudioAttributes.USAGE_ALARM)
							.build()
						setSound(it, audioAttributes)
					}
				}
			)
		}
	}

	override fun checkTimers() {
		checkTimers.tryEmit(TimerTrigger())
	}

	private suspend fun onTimersUpdated(results: List<RunningTimer>) {
		val activeNotifications = androidSettings.getStringSet(notificationSettings, mutableSetOf()) ?: setOf()
		activeNotifications.forEach(this::cancelTimer)

		val newNotifications = mutableSetOf<String>()
		val toDelete = mutableListOf<RunningTimer>()
		val now = System.currentTimeMillis()
		for (timer in results) {
			val requestId = start(timer, now)
			if (requestId != null) {
				newNotifications.add(requestId.toString())
			} else {
				toDelete.add(timer)
			}
		}

		androidSettings.putStringSet(notificationSettings, newNotifications)
		timerRepository.delete(toDelete)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms() && newNotifications.isNotEmpty()) {
			val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			context.startActivity(intent)
		}
	}

	private fun start(timer: RunningTimer, now: Long): Int? {
		val toExpire = timer.startedAt + timer.duration.dbDuration.inWholeMilliseconds
		if (toExpire <= now) return null

		val delay = toExpire - now

		val requestId = App.getIntentRequestId()
		createNotification(requestId, timer, delay)
		setTimer(requestId, timer, toExpire)
		return requestId
	}

	@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
	private fun createNotification(requestId: Int, timer: RunningTimer, delayMillis: Long) {
		val notification = notificationHelper.createRunningNotification(timer, delayMillis)
		notificationManager.notify(timerNotificationTag, requestId, notification)
	}

	private fun setTimer(requestId: Int, timer: RunningTimer, triggerTime: Long) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
			&& !alarmManager.canScheduleExactAlarms()) {
			return
		}
		val pendingIntent = getTimerIntent(requestId, timer)
		AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
	}

	private fun getTimerIntent(requestId: Int, timer: RunningTimer? = null): PendingIntent {
		val intent = Intent(context, TimerBroadcastReceiver::class.java).apply {
			action = TimerBroadcastReceiver.actionTimeout
			if (timer != null) {
				putExtra(TimerBroadcastReceiver.timerEntityId, timer.id)
				putExtra(TimerBroadcastReceiver.timerNotificationId, requestId)
			}
		}
		val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.FLAG_IMMUTABLE
		} else {
			0
		}

		return PendingIntent.getBroadcast(context, requestId, intent, flags)
	}

	private fun cancelTimer(requestIdString: String) {
		val requestId = requestIdString.toIntOrNull() ?: return
		val intent = getTimerIntent(requestId)

		alarmManager.cancel(intent)
		notificationManager.cancel(timerNotificationTag, requestId)
	}

	companion object {
		const val timerNotificationTag = "Timer"

		const val notificationChannelRunningTimerId = "RecipeTimersRunning"
		const val notificationChannelFinishedTimerId = "RecipeTimersFinished"
		const val notificationSettings = "Settings.Notification.Active"

		fun getNotificationSound(): Uri? {
			return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
				?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
				?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
		}
	}

	class TimerTrigger
}