package click.alchemist.cook.service.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

interface TimeService {
    fun tick(): Flow<Long>
}

@ExperimentalCoroutinesApi
fun TimeService.tickWhen(toggle: Flow<Boolean>) =
    toggle.flatMapLatest {
        if (it)
            tick()
        else
            emptyFlow()
    }
        .onStart { emit(System.currentTimeMillis()) }
        .distinctUntilChanged()