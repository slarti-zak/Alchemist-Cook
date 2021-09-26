package click.alchemist.cook.service.recipe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.service.couchbase.repository.TimerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@ExperimentalCoroutinesApi
@FlowPreview
class TimerBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                actionDelete -> deleteTimer(intent)
                actionAddMinute -> addMinute(intent)
                actionTimeout -> timeoutTimer(context, intent)
            }
        } catch (e: Exception) {
            logError("TimerBroadcastReceiver", "Error handling intent ${intent.action}", e)
        }
    }

    private fun deleteTimer(intent: Intent) {
        val id = intent.getStringExtra(timerEntityId) ?: return
        logInfo("TimerBroadcastReceiver", "Delete timer for $id")
        val timerRepository: TimerRepository by inject()

        timerRepository.delete(id)
    }

    private fun addMinute(intent: Intent) {
        val id = intent.getStringExtra(timerEntityId) ?: return
        logInfo("TimerBroadcastReceiver", "Add minute for $id")
        val timerRepository: TimerRepository by inject()

        val timer = timerRepository.load(id) ?: return
        timerRepository.addMinute(timer)
    }

    private fun timeoutTimer(context: Context, intent: Intent) {
        val id = intent.getStringExtra(timerEntityId) ?: return
        logInfo("TimerBroadcastReceiver", "Timeout for id $id")

        val timerRepository: TimerRepository by inject()
        val timer = timerRepository.load(id) ?: return

        createNotification(context, timer)
        logInfo("TimerBroadcastReceiver", "Created elapsed notification for $id")
    }

    private fun createNotification(context: Context, timer: RunningTimer) {
        val notification = TimerNotificationHelper(context).createElapsedNotification(timer)

        NotificationManagerCompat.from(context).notify(
            timer.id,
            AlarmManagerTimerService.timerNotificationId,
            notification
        )
    }

    companion object {
        const val broadcastId: Int = 1
        const val actionAddMinute = "click.alchemist.cook.AddMinute"
        const val actionDelete = "click.alchemist.cook.DeleteTimer"
        const val actionTimeout = "click.alchemist.cook.Timeout"

        const val timerEntityId = "id"
    }
}