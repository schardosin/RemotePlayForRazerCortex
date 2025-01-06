package com.razer.neuron.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.channels.Channel

/**
 * https://medium.com/@wind.orca.pe/handling-android-runtime-permissions-with-coroutines-and-suspend-functions-5b4aa4e74ee5
 */
class RequestMultiplePermissions(componentActivity: ComponentActivity) : PermissionChecker {
    private val activityResultLauncher = with(componentActivity) {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val m = result.mapValues { (key, value) ->
                if (value) {
                    PermissionChecker.State.Granted
                } else {
                    PermissionChecker.State.Denied(shouldShowRequestPermissionRationale(key))
                }
            }

            channel.trySend(PermissionChecker.Result(m))
        }
    }

    private val channel = Channel<PermissionChecker.Result>(1)

    suspend fun request(permissions: Set<String>): PermissionChecker.Result {
        activityResultLauncher.launch(permissions.toTypedArray())
        return channel.receive()
    }
}


interface PermissionChecker {
    class Result(m: Map<String, State>) : Map<String, State> by HashMap(m) {
        operator fun component1(): Set<String> = granted()
        operator fun component2(): Set<String> = denied()

        val areAllGranted get() = values.all { it == State.Granted }

        val deniedPermissions get() = entries.mapNotNull { if(it.value.isDenied()) it.key else null }

        private fun denied() = filterValues(State::isDenied).keys
        private fun granted() = filterValues(State::isGranted).keys
    }

    sealed interface State {
        val shouldShowRequestPermissionRationale: Boolean

        fun isGranted() = this is Granted
        fun isDenied() = this is Denied

        object Granted : State {
            override val shouldShowRequestPermissionRationale: Boolean = false
        }

        data class Denied(
            override val shouldShowRequestPermissionRationale: Boolean
        ) : State
    }
}

fun Context.checkSelfMultiplePermissions(vararg permissions: String): Boolean {
    return permissions.all {
        checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.checkSelfSinglePermission(permission: String): Boolean {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}