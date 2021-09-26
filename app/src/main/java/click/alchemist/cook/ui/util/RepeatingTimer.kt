package click.alchemist.cook.ui.util

import android.os.Handler
import android.os.Looper


class RepeatingTimer {
    private var currentTimer: TimerInstance? = null

    fun startTimer(delayMillis: Long, callback: () -> Unit) {
        currentTimer?.cancel()
        currentTimer = TimerInstance(delayMillis, callback).apply { start() }
    }

    fun cancelTimer() {
        currentTimer?.cancel()
    }

    class TimerInstance(private val delayMillis: Long, private val callback: () -> Unit) {

        private var running: Boolean = true

        private val handler = Handler(Looper.getMainLooper())

        fun start() {
            handler.postDelayed(this::run, delayMillis)
        }

        private fun run() {
            if (!running) return

            callback()
            handler.postDelayed(this::run, delayMillis)
        }

        fun cancel() {
            running = false
        }
    }
}