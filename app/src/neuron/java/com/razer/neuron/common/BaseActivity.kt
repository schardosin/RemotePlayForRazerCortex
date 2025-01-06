package com.razer.neuron.common

import android.os.Build
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import com.razer.neuron.extensions.getContentRootView
import com.razer.neuron.extensions.globalOnUnexpectedError
import com.razer.neuron.extensions.isRazerDevice
import com.razer.neuron.extensions.usbManager
import com.razer.neuron.utils.PermissionChecker
import com.razer.neuron.utils.RequestMultiplePermissions
import com.razer.neuron.utils.checkSelfSinglePermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    sealed interface ActivityWindowInsets {
        val boundingRectLeftWidth: Int?
        val boundingRectRightWidth: Int?
        val boundingRectTopHeight: Int?
        val boundingRectBottomHeight: Int?

        @RequiresApi(Build.VERSION_CODES.Q)
        class DetailInsets(val windowInset: WindowInsets) : ActivityWindowInsets {
            override val boundingRectLeftWidth get() = windowInset.displayCutout?.boundingRectLeft?.width()
            override val boundingRectRightWidth get() = windowInset.displayCutout?.boundingRectRight?.width()
            override val boundingRectTopHeight get() = windowInset.displayCutout?.boundingRectTop?.height()
            override val boundingRectBottomHeight get() = windowInset.displayCutout?.boundingRectBottom?.height()
            override fun toString() =
                "DetailInsets($boundingRectLeftWidth,$boundingRectTopHeight,$boundingRectRightWidth,$boundingRectBottomHeight)"
        }

        class BasicInsets(val windowInset: WindowInsetsCompat) : ActivityWindowInsets {
            override val boundingRectLeftWidth get() = windowInset.systemWindowInsets.left
            override val boundingRectRightWidth get() = windowInset.systemWindowInsets.right
            override val boundingRectTopHeight get() = windowInset.systemWindowInsets.top
            override val boundingRectBottomHeight get() = windowInset.systemWindowInsets.bottom
            override fun toString() =
                "BasicInsets($boundingRectLeftWidth,$boundingRectTopHeight,$boundingRectRightWidth,$boundingRectBottomHeight)"
        }
    }

    open val onUnexpectedError = globalOnUnexpectedError

    private val requestMultiplePermissions = RequestMultiplePermissions(this)

    private var onGlobalLayoutListener: InnerOnGlobalLayoutListener? = null

    inner class InnerOnGlobalLayoutListener : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val rootView = getContentRootView() ?: return
            val windowInsets = rootView.rootWindowInsets
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsets?.let { onInsetsChange(ActivityWindowInsets.DetailInsets(it)) }
            } else {
                val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
                onInsetsChange(ActivityWindowInsets.BasicInsets(windowInsetsCompat))
            }
            if (onGlobalLayoutListener == this) {
                rootView.viewTreeObserver?.removeOnGlobalLayoutListenerSafely(this)
                onGlobalLayoutListener = null
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onGlobalLayoutListener = InnerOnGlobalLayoutListener().apply {
            getContentRootView()?.viewTreeObserver?.addOnGlobalLayoutListener(this)
        }
    }

    override fun onStop() {
        super.onStop()
        onGlobalLayoutListener?.let {
            getContentRootView()?.viewTreeObserver?.removeOnGlobalLayoutListenerSafely(it)
            onGlobalLayoutListener = null
        }
    }

    /**
     * When the [ActivityWindowInsets] is first calculated or when there is a change
     */
    open fun onInsetsChange(insets: ActivityWindowInsets) = Unit


    /**
     * Request permissions if not already granted.
     * [task] will be invoked with the [PermissionChecker.Result] of the permission request
     *
     * @param permissions permissions that you need (even though it might already be granted)
     * @param task task that you want to execute afterward
     *
     */
    suspend fun maybeRequestPermissions(
        vararg permissions: String,
    ): PermissionChecker.Result {
        val missing = permissions.toMutableList().filter { !checkSelfSinglePermission(it) }
        val permissionResult = if (missing.isEmpty()) {
            PermissionChecker.Result((permissions.map { it to PermissionChecker.State.Granted }).toMap())
        } else {
            requestMultiplePermissions.request(missing.toSet())
        }
        return permissionResult
    }
}


fun ViewTreeObserver.removeOnGlobalLayoutListenerSafely(victim: OnGlobalLayoutListener) =
    runCatching {
        removeOnGlobalLayoutListener(victim)
    }