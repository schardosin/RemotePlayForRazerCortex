package com.razer.neuron.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.razer.bianca.IControllerAIDLService
import com.razer.neuron.RnApp
import com.razer.neuron.common.debugToast
import com.razer.neuron.extensions.globalOnUnexpectedError
import com.razer.neuron.nexus.NexusContentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

object AIDLManager {

    const val NEXUS_AIDL_VERSION_CODE = 3800024
    private const val NEXUS_PACKAGE_NAME = "com.razer.bianca"
    private const val CONTROLLER_SERVICE_CLASS_NAME = "com.razer.bianca.ControllerForegroundService"
    private val globalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + globalOnUnexpectedError)

    private var controllerAIDLService: IControllerAIDLService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            controllerAIDLService = IControllerAIDLService.Stub.asInterface(service)
            try {
                controllerAIDLService?.asBinder()?.linkToDeath(deathRecipient, 0)
            } catch (e: Exception) {
                debugToast("AIDL linkToDeath error: ${e.message ?: "unknown binding error"}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            controllerAIDLService = null
        }
    }

    /**
     * The callback will be called if the binding service is unbind unexpectedly.
     */
    private val deathRecipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            try {
                controllerAIDLService?.asBinder()?.unlinkToDeath(this, 0)
                controllerAIDLService = null
            } catch (e: Exception) {
                debugToast("AIDL unlinkToDeath error: ${e.message ?: "unknown binding error"}")
            }

            // Try to bind the service again if the host binder is dead
            globalScope.launch {
                bindService(RnApp.appContext)
            }
        }
    }

    /**
     * Call [context.bindService] to bind the service,
     * which will invoke [onBind] method on [com.razer.bianca.ControllerForegroundService] to bind the stub.
     *
     * @return true if there is [Context.bindService] was called, false if the binding already exist
     * error if there is no Razer controller attached
     *
     * We must also check that there is a controller attached, since [CONTROLLER_SERVICE_CLASS_NAME]
     * should only be start if there is a controller attached.
     */
    suspend fun bindService(context: Context) : Result<Boolean> {
        return kotlin.runCatching {
            val isControllerForegroundServiceRunning = NexusContentProvider.isControllerForegroundServiceRunning(context) ?: false
            if (controllerAIDLService == null && isControllerForegroundServiceRunning) {
                Intent().apply {
                    setComponent(ComponentName(NEXUS_PACKAGE_NAME, CONTROLLER_SERVICE_CLASS_NAME))
                    try {
                        context.bindService(
                            this,
                            serviceConnection,
                            AppCompatActivity.BIND_AUTO_CREATE
                        )
                    } catch (e: Exception) {
                        debugToast("AIDL bind error: ${e.message ?: "unknown binding error"}")
                    }
                }
                true
            } else {
                false
            }
        }
    }

    fun unbindService(context: Context) {
        kotlin.runCatching {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                val msg = "AIDL unbind error: ${e.message ?: "unknown unbinding error"}"
                Timber.w(msg)
            }
        }
    }

    fun doVibrate(lowFreqMotor: Int, highFreqMotor: Int) {
        runCatching {
            if (controllerAIDLService?.asBinder()?.isBinderAlive == true) {
                controllerAIDLService?.doVibrate(lowFreqMotor, highFreqMotor)
            }
        }
    }

    fun onStartNeuronGame() {
        globalScope.launch {
            runCatching {
                if (controllerAIDLService?.asBinder()?.isBinderAlive == true) {
                    controllerAIDLService?.onStartNeuronGame()
                }
            }
        }
    }


    fun onStopNeuronGame() {
        globalScope.launch {
            runCatching {
                if (controllerAIDLService?.asBinder()?.isBinderAlive == true) {
                    controllerAIDLService?.onStopNeuronGame()
                }
            }
        }
    }
}
