package com.razer.neuron.utils

fun <T, K> Result<T>.transformSuccess(transform: (T) -> K): Result<K> {
    val result = this.getOrNull()
    return if(this.isSuccess) {
        result?.let { Result.success(transform(it)) } ?: Result.failure(IllegalArgumentException("No result"))
    } else {
        Result.failure(exceptionOrNull() ?: IllegalArgumentException("No error"))
    }
}