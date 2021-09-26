package click.alchemist.cook.service.recipe

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import click.alchemist.cook.App
import click.alchemist.cook.MainComposeActivity
import click.alchemist.cook.R
import click.alchemist.cook.model.RunningTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class TimerNotificationHelper(private val context: Context) {
    fun createRunningNotification(timer: RunningTimer, delayMillis: Long): Notification {
        val chronometerTime = SystemClock.elapsedRealtime() + delayMillis

        val notificationView = RemoteViews(context.packageName, R.layout.notification_timer).apply {
            setTextViewText(R.id.title, "${timer.content} - ${timer.title}")

            setOnClickPendingIntent(R.id.button, createAddMinuteIntent(timer))
            setTextViewText(R.id.button, context.getString(R.string.add_minute))

            setChronometer(R.id.chronometer, chronometerTime, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.chronometer, true)
            }
        }

        return NotificationCompat.Builder(
            context,
            AlarmManagerTimerService.notificationChannelRunningTimerId
        )
            .setContentTitle(timer.title)
            .setCustomContentView(notificationView)
            .setCustomBigContentView(notificationView)
            .setSmallIcon(R.drawable.ic_timer_sand_empty)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setDeleteIntent(createDeleteIntent(timer))
            .setContentIntent(createNavigationIntent())
            .build()
    }

    fun createElapsedNotification(timer: RunningTimer): Notification {
        val notification = NotificationCompat.Builder(
            context,
            AlarmManagerTimerService.notificationChannelFinishedTimerId
        )
            .setContentTitle("${timer.content} - ${timer.title} Done!")
            .setSmallIcon(R.drawable.ic_timer_sand_empty)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(AlarmManagerTimerService.getNotificationSound(), AudioManager.STREAM_ALARM)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(createNavigationIntent())
            .setDeleteIntent(createDeleteIntent(timer))
            .setAutoCancel(true)
//			.setFullScreenIntent(createNavigationIntent(), true)
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        return notification
    }

    private fun createNavigationIntent(): PendingIntent {
//		return NavDeepLinkBuilder(context)
//			.setGraph(R.navigation.cooking_navigation)
//			.setDestination(R.id.cooking_navigation)
//			.createPendingIntent()
        val intent = Intent(context, MainComposeActivity::class.java).apply {
            putExtra("opencook", true)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun createAddMinuteIntent(timer: RunningTimer): PendingIntent =
        createPendingIntentForTimer(timer, TimerBroadcastReceiver.actionAddMinute)

    private fun createDeleteIntent(timer: RunningTimer): PendingIntent =
        createPendingIntentForTimer(timer, TimerBroadcastReceiver.actionDelete)

    private fun createPendingIntentForTimer(
        timer: RunningTimer,
        intentAction: String
    ): PendingIntent {
        val intent = Intent(context, TimerBroadcastReceiver::class.java).apply {
            action = intentAction
            putExtra(TimerBroadcastReceiver.timerEntityId, timer.id)
        }
        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getBroadcast(context, App.getIntentRequestId(), intent, flags)
    }
}