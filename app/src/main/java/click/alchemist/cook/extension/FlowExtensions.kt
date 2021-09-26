package click.alchemist.cook.extension

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

fun <T> Flow<List<T>>.firstElement(): Flow<T> = filter { it.isNotEmpty() }.map { it[0] }

fun <T> Flow<T>.share(): SharedFlow<T> = shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

fun <A, B : Any, R> Flow<A>.withLatestFrom(other: Flow<B>, transform: suspend (A, B) -> R): Flow<R> = flow {
    coroutineScope {
        val latestB = AtomicReference<B?>()
        val outerScope = this
        launch {
            try {
                other.collect { latestB.set(it) }
            } catch (e: CancellationException) {
                outerScope.cancel(e) // cancel outer scope on cancellation exception, too
            }
        }
        collect { a ->
            val b = latestB.get()
            if (b != null) {
                emit(transform(a, b))
            }
        }
    }
}

fun <A, B : Any, C : Any, R> Flow<A>.withLatestFrom(other1: Flow<B>, other2: Flow<C>, transform: suspend (A, B, C) -> R): Flow<R> = flow {
    coroutineScope {
        val latestB = AtomicReference<B?>()
        val latestC = AtomicReference<C?>()

        val outerScope = this
        launch {
            try {
                other1.collect { latestB.set(it) }
            } catch (e: CancellationException) {
                outerScope.cancel(e) // cancel outer scope on cancellation exception, too
            }
        }
        launch {
            try {
                other2.collect { latestC.set(it) }
            } catch (e: CancellationException) {
                outerScope.cancel(e) // cancel outer scope on cancellation exception, too
            }
        }
        collect { a: A ->
            val b = latestB.get()
            val c = latestC.get()
            if (b != null && c != null) {
                emit(transform(a, b, c))
            }
        }
    }
}