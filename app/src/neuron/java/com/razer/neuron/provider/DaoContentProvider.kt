package com.razer.neuron.provider

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of [ObjectContentSource] to get [T] object, using a single [keyColName] as key
 */
abstract class DaoContentProvider<T, F : NeuronProviderModelHelper<T>>(val keyColName : String) : ObjectContentSource<T, F> {

    private val mutex = Mutex()

    abstract suspend fun getByKey(context : Context, key : String) : T?

    abstract suspend fun getAll(context : Context) : List<T>

    abstract suspend fun deleteByKey(context : Context, key : String) : Boolean

    abstract suspend fun insertOrReplace(context : Context, objects : List<T>)

    override fun query(context : Context, uri: Uri, matchKeyValues: Array<Pair<String, Any?>>?): List<T> {
        val key = (matchKeyValues?.firstOrNull { it.first == keyColName }?.second as? String)
        return runBlocking {
            mutex.withLock {
                if(key != null) {
                    getByKey(context, key)?.let { listOf(it) } ?: emptyList()
                } else {
                    getAll(context)
                }
            }
        }
    }

    override fun delete(context : Context, uri: Uri, matchKeyValues: Array<Pair<String, Any?>>?): Int {
        val key = (matchKeyValues?.firstOrNull { it.first == keyColName }?.second as? String) ?: return 0
        return runBlocking {
            mutex.withLock {
                if(deleteByKey(context, key)) 1 else 0
            }
        }
    }

    override fun insertOrReplace(context : Context, uri: Uri, item: T, matchKeyValues: Array<Pair<String, Any?>>?): Uri? {
        return runBlocking {
            mutex.withLock {
                insertOrReplace(context, listOf(item))
                uri
            }
        }
    }
}

