package com.razer.neuron.di

import timber.log.Timber

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Global handler for any unexpected exception
 */
val globalUnexpectedExceptionHandler = CoroutineExceptionHandler { _, t ->
    Timber.e(t)
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @UnexpectedExceptionHandler
    @Provides
    fun providesUnexpectedExceptionHandler(): CoroutineExceptionHandler {
        return globalUnexpectedExceptionHandler
    }

    @GlobalCoroutineScope
    @Singleton
    @Provides
    fun providesGlobalScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher + globalUnexpectedExceptionHandler)

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

}