package click.alchemist.cook.service.time

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import click.alchemist.cook.logError
import click.alchemist.cook.service.recipe.TimerService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExactAlarmPermissionReceiver : BroadcastReceiver(), KoinComponent {
	override fun onReceive(contenxt: Context, intent: Intent) {
		try {
			when (intent.action) {
				AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> updateTimers()
			}
		} catch (e: Exception) {
			logError("ExactAlarmPermissionReceiver", "Error handling intent ${intent.action}", e)
		}
	}

	private fun updateTimers() {
		val timerRepository: TimerService by inject()
		timerRepository.checkTimers()
	}
}