package com.razer.neuron.extensions

import com.razer.neuron.common.logAndRecordException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

val globalOnUnexpectedError = CoroutineExceptionHandler { _, e ->
    logAndRecordException(e)
}


/**
 * https://stackoverflow.com/a/48976049
 *
 * Simply, wrap this function with try-catch for [kotlinx.coroutines.TimeoutCancellationException],
 * add handling for the exception to remove/unregister the callback used with [suspendCancellableCoroutine]
 */
suspend inline fun <T> suspendCoroutineWithTimeout(
    timeoutMs: Long,
    crossinline block: (CancellableContinuation<T>) -> Unit
) = withTimeout(timeoutMs) {
    suspendCancellableCoroutine(block = block)
}