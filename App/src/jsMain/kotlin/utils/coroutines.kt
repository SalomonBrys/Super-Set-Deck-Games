package utils

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


fun isolatedScope(context: CoroutineContext = EmptyCoroutineContext): CoroutineScope = CoroutineScope(Dispatchers.Unconfined + context)

fun CoroutineScope.launchUndispatched(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context, start = CoroutineStart.UNDISPATCHED, block = block)

fun <T> CoroutineScope.asyncUndispatched(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = async(start = CoroutineStart.UNDISPATCHED, block = block)
