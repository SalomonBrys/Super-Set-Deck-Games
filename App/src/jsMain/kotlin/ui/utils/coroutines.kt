package ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import react.*
import utils.isolatedScope


fun useCoroutineScope(): CoroutineScope {
    val coroutineScope = useMemo(Unit) { isolatedScope() }
    useEffectWithCleanup(Unit) {
        onCleanup { coroutineScope.cancel() }
    }
    return coroutineScope
}

fun <T> useFlow(
    vararg dependencies: Any?,
    getFlow: suspend () -> Flow<T>?
): T? {
    var result: T? by useState()
    useEffect(*dependencies) {
        getFlow()?.collect {
            result = it
        }
    }
    return result
}

fun <T> useSuspend(
    vararg dependencies: Any?,
    get: suspend () -> T
): T? {
    var result: T? by useState()
    useEffect(dependencies) {
        result = get()
    }
    return result
}
