package com.razer.neuron.nexus

import android.content.Context
import timber.log.Timber

import com.razer.neuron.shared.SharedContentException
import com.razer.neuron.utils.checkSelfSinglePermission
import kotlin.jvm.Throws

interface NexusContentSource {

    val path: String

    open fun logger(line : String) = Timber.d(line)
    /**
     * TODO:
     * check if Nexus is installed, if not throw a different exception
     */
    @Throws(SharedContentException::class)
    suspend fun <K> checkOrThrowPermission(context : Context, task: suspend () -> K) = checkContentProviderPermissionOrThrow(context, task)

    /**
     * This gives a chance for neuron to run data-sync as soon as possible.
     *
     * Whether it is to always read from Nexus and write it to Neuron
     * or always read from Neuron and write it back to the Nexus.
     *
     * [NexusContentSource.sync] will be called when app starts with the permission [SHARED_CONTENT_PROVIDER_DATA_ACCESS_PERMISSION]
     */
    @Throws(SharedContentException::class)
    suspend fun sync(context : Context) = Unit
}


/**
 * TODO:
 * check if Nexus is installed, if not throw a different exception
 */
@Throws(SharedContentException::class)
suspend fun <K> checkContentProviderPermissionOrThrow(context : Context, task: suspend () -> K): K {
    if (!context.checkSelfSinglePermission(SHARED_CONTENT_PROVIDER_DATA_ACCESS_PERMISSION))
        throw SharedContentException("Data access permission required")
    else
        return task()
}
