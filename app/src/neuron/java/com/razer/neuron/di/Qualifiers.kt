package com.razer.neuron.di

import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Qualifier


@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher




/**
 * Means the [CoroutineExceptionHandler] is meant for unexpected [Exception]
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UnexpectedExceptionHandler

/**
 * Means the [kotlin.coroutines.CoroutineContext] has a [CoroutineExceptionHandler]
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class GlobalCoroutineScope