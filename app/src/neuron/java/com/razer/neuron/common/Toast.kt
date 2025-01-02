package com.razer.neuron.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.limelight.BuildConfig
import com.razer.neuron.RnApp
import timber.log.Timber
import java.lang.ref.WeakReference


fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()


/**
 * Run something on main thread
 */
@JvmOverloads
fun runOnUiThreadDelayed(runnable: Runnable, delayMs: Long = 0L) {
    Handler(Looper.getMainLooper()).postDelayed(runnable, delayMs)
}

fun runOnUiThread(runnable: Runnable) {
    runOnUiThreadDelayed(runnable, 0L)
}


private var lastToast: WeakReference<Toast>? = null
fun Context.toast(msg: String?, durationLength: Int = Toast.LENGTH_SHORT) {
    msg ?: return
    if (!isMainThread()) {
        runOnUiThread {
            toast(msg = msg, durationLength = durationLength)
        }
        return
    }
    lastToast?.get()?.cancel()
    Timber.d(msg)
    Toast.makeText(this.applicationContext, msg, durationLength)
        .apply {
            lastToast = WeakReference(this)
        }
        .show()
}

fun Context.debugToast(msg: String?) {
    msg ?: return
    Timber.d("debugToast: $msg")
    if (BuildConfig.DEBUG) {
        toast("[DEBUG]${msg}")
    }
}


fun debugToast(msg: String?) = RnApp.appContext.debugToast(msg = msg)
fun toast(msg: String?) = RnApp.appContext.toast(msg = msg)
