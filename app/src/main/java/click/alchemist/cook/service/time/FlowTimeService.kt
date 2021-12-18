package click.alchemist.cook.service.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration.Companion.milliseconds

class FlowTimeService : TimeService {
    private val ticker = flow {
        while (true) {
            delay(500.milliseconds)
            emit(System.currentTimeMillis())
        }
    }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

    override fun tick(): Flow<Long> = ticker
}