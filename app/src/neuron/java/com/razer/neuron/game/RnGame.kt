package com.razer.neuron.game

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.limelight.BuildConfig
import com.limelight.Game
import com.limelight.NeuronBridge
import com.limelight.R
import com.limelight.nvstream.http.toResolution
import com.limelight.utils.SpinnerDialog
import com.razer.neuron.common.ComputerMeta
import com.razer.neuron.common.debugToast
import com.razer.neuron.di.GlobalCoroutineScope
import com.razer.neuron.extensions.applyTransition
import com.razer.neuron.extensions.isStandardResolution
import com.razer.neuron.extensions.setDrawIntoSafeArea
import com.razer.neuron.extensions.visibleIf
import com.razer.neuron.game.RnGameError.Companion.EXTRA_WAS_RESTARTED
import com.razer.neuron.game.RnGameError.Companion.createAlertIntent
import com.razer.neuron.game.helpers.RnGameIntentHelper
import com.razer.neuron.game.helpers.RnGameView
import com.razer.neuron.game.helpers.getConnectionErrors
import com.razer.neuron.model.DisplayModeOption
import com.razer.neuron.model.SessionStats
import com.razer.neuron.nexus.NexusContentProvider
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.utils.now
import com.razer.neuron.utils.toISO8601String
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


/**
 * TODO: subclass [Game] instead. Right now it is just a blue page from [R.layout.rn_activity_test1] for ease of testing
 */
@AndroidEntryPoint
class RnGame : Game() {
    companion object : RnGameIntentHelper {
        private const val EXTRA_CREATE_COUNT = "EXTRA_CREATE_COUNT"
    }
    @GlobalCoroutineScope
    @Inject
    lateinit var globalScope: CoroutineScope
    private val wasRestarted by lazy { intent.getBooleanExtra(EXTRA_WAS_RESTARTED, false) }
    private val notificationOverlayView: TextView by lazy { findViewById(R.id.notificationOverlay) }
    private val view by lazy { RnGameView(this) }
    private val isCropToSafeArea by lazy { NexusContentProvider.settingsSource.isVirtualDisplayCropToSafeArea(this) }
    private val isVirtualDisplayMode by lazy { NexusContentProvider.settingsSource.isVirtualDisplayMode(this) }

    /**
     * Keep track of whether [handleUngracefulTermination] was called once already
     */
    private var wasUngracefulTerminationHandled = false
    private var wasUngracefulTermination = false


    private var connectionTerminatedAt : Long? = null
        set(value) {
            field = if(field == null) value else field
        }
    private var surfaceDestroyedAt : Long? = null
        set(value) {
            field = if(field == null) value else field
        }
    private var finishAt : Long? = null
        set(value) {
            field = if(field == null) value else field
        }
    private var firstHomePressedAt : Long? = null
    private var lastHomePressedAt : Long? = null
        set(value) {
            field = value
            if(firstHomePressedAt == null) {
                firstHomePressedAt = value
            }
        }
    private var firstBackPressedAt : Long? = null
    private var lastBackPressedAt : Long? = null
        set(value) {
            field = value
            if(firstBackPressedAt == null) {
                firstBackPressedAt = value
            }
        }

    private fun isUserWantsToLeave() : Boolean {
        val didUserPressBack = lastBackPressedAt?.let { now() > it } ?: false
        val didUserPressHome = lastHomePressedAt?.let { now() > it } ?: false
        return didUserPressBack || didUserPressHome
    }

    private var createCount : Int
        get() = intent?.getIntExtra(EXTRA_CREATE_COUNT, 0) ?: 0
        set(value) {
            intent?.putExtra(EXTRA_CREATE_COUNT, value)
        }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            debugToast("Back pressed")
            lastBackPressedAt = now()
            finish()
        }
    }

    lateinit var tvStreamInfo : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTransition()
        if(isCropToSafeArea) {
            window.statusBarColor = Color.BLACK
        }
        isFullscreenLayout = !isVirtualDisplayMode || !isCropToSafeArea
        setDrawIntoSafeArea(isFullscreenLayout)
        super.onCreate(savedInstanceState)
        if(BuildConfig.DEBUG) {
            Timber.v("onCreate: ${intent.extras}")
            findViewById<View>(R.id.surfaceView).foreground = resources.getDrawable(R.drawable.debug_layout_bound)
        }
        if(createCount > 0) {
            com.razer.neuron.common.debugToast("Game activity restart aborted")
            finish()
            return
        }
        tvStreamInfo = findViewById<TextView?>(R.id.tv_debug_stream_info).apply { visibleIf { RemotePlaySettingsPref.isPerfOverlayEnabled } }
        findViewById<ViewGroup>(R.id.rn_neuron_debug_overlay).visibleIf { RemotePlaySettingsPref.isPerfOverlayEnabled }
        // Inside your activity (if you did not enable transitions in your theme)
        notificationOverlayView.alpha = 0f
        notificationOverlayView.addTextChangedListener {
            if (notificationOverlayView.visibility == View.VISIBLE && it?.isNotEmpty() == true) {
                view.showNotificationText(it.toString())
            } else {
                view.hideNotificationText()
            }
        }
        // close the dialog so that it won't show
        SpinnerDialog.closeDialogs(this)
        RnGameError.finishShownMessage()
        RnGameError.cancelPendingRestart()
        createCount++
        updateComputerMetaTimestamp()
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        debugToast("Home pressed")
        lastHomePressedAt = now()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(if (layoutResID == R.layout.activity_game) R.layout.rn_activity_game else layoutResID)
    }


    override fun stageFailed(stage: String?, portFlags: Int, errorCode: Int) {
        lifecycleScope.launch {
            resources.getConnectionErrors(stage ?: "", portFlags, errorCode = errorCode).forEach {
                    error ->
                handleUngracefulTermination(error.title ?: getString(R.string.conn_error_title), error.message)
                if(!isFinishing) {
                    finish()
                }
            }
        }
    }



    /**
     * Show [stage] on UI
     */
    override fun stageStarting(stage: String?) {
        stage?.let { NeuronBridge.getLocalizedStageName(it) }
            ?.takeIf { it.isNotBlank() }
            ?.let {
                view.showLoadingProgress(it)
            }
    }

    /**
     * Close all custom UI
     */
    override fun connectionStarted() {
        super.connectionStarted()
        lifecycleScope.launch { updateDebugStreamInfo() }
        view.hideLoadingProgress()
    }

    private fun updateDebugStreamInfo(){
        val params = conn?.lastLaunchQuery
            ?.split("&")
            ?.map { it.substringBefore("=") to it.substringAfter("=") } ?: listOf()


        tvStreamInfo.text = params.joinToString("\n") { (name, value) -> "${name}=${value}" }

    }

    override fun onStart() {
        super.onStart()
        onBackPressedCallback.isEnabled = true
        onBackPressedDispatcher.addCallback(this,  onBackPressedCallback.apply { isEnabled = true })
        NeuronBridge.onStartNeuronGame()
    }


    override fun onStop() {
        onBackPressedCallback.isEnabled = false
        val wasFinishing = isFinishing
        // if isFinishing is already true here that means user
        if(RemotePlaySettingsPref.isPerfOverlayEnabled) {
            SessionStats.lastSession?.takeIf { (it.elapsedTimeMs ?: 0) > 2000 }.let {
                RemotePlaySettingsPref.savedLastSessionStats = it
            }
        }
        super.onStop()
        Timber.v("onStop: wasFinishing=$wasFinishing, isFinishing=${isFinishing}")
        Timber.v("onStop: connectionTerminatedAt=${connectionTerminatedAt?.toISO8601String(includeMilliseconds = true, short = true)}")
        Timber.v("onStop: surfaceDestroyedAt=${surfaceDestroyedAt?.toISO8601String(includeMilliseconds = true, short = true)}")
        Timber.v("onStop: finishAt=${finishAt?.toISO8601String(includeMilliseconds = true, short = true)}")

        val connectionTerminatedAt = connectionTerminatedAt
        val surfaceDestroyedAt = surfaceDestroyedAt
        val connectionTerminatedUngracefully =
            wasFinishing // before onStop the activity is already finishing
                    && !isUserWantsToLeave()
                    && connectionTerminatedAt != null
                    && surfaceDestroyedAt != null
                    && wasUngracefulTermination
                    && connectionTerminatedAt < surfaceDestroyedAt // for some reason surface was destroyed before termination
        if(connectionTerminatedUngracefully && !wasUngracefulTerminationHandled) {
            handleUngracefulTermination(
                getString(R.string.conn_error_title),
                getString(R.string.rn_host_ended_connection_msg)
            )
        }
        NeuronBridge.onStopNeuronGame()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.v("onConfigurationChanged: $newConfig")
        super.onConfigurationChanged(newConfig)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.v("surfaceCreated:")
        super.surfaceCreated(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.v("surfaceChanged: $format ${width}x${height}")
        super.surfaceChanged(holder, format, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.v("surfaceDestroyed:")
        surfaceDestroyedAt = now()
        super.surfaceDestroyed(holder)
    }

    override fun connectionTerminated(errorCode: Int) {
        // 0 means graceful (see ML_ERROR_GRACEFUL_TERMINATION)
        wasUngracefulTermination = errorCode != 0
        Timber.v("connectionTerminated: $errorCode (${errorCode.toString(2)})")
        connectionTerminatedAt = now()
        super.connectionTerminated(errorCode)
    }

    override fun finish() {
        Timber.v("finish:")
        finishAt = now()
        super.finish()
    }

    /**
     * Custom handling when we stream with native resolution but connection fail to start
     *
     * In Pixel 7 pro, when 2232x1080 (display scale at 25%) is used, the streaming will fail. It
     * fails like the following:
     * 1. [Game.onCreate] will be called as usual
     * 2. [surfaceDestroyed] will be called shortly while [com.limelight.nvstream.NvConnection.start] is being called
     * 3. [Game.onStop] will be called as a result (and finished)
     * 4. [com.limelight.nvstream.NvConnection] will still have an internal thread running because [NvHTTP] cannot be fully stopped
     * 5. [Game.onCreate] will be called again (Maybe the OS is trying to recreate the activity)
     * 6. [surfaceDestroyed] will be called shortly while [com.limelight.nvstream.NvConnection.start] is being called
     * 7. [Game.onStop] will be called as a result (and finished)
     * 8. [com.limelight.nvstream.NvConnection] will still have an internal thread running because [NvHTTP] cannot be fully stopped
     * 9. No more [Game] being created anymore (maybe OS will not do it)
     *
     * Main problem with this existing flow is that there is no error message shown to user. And given
     * how these classes work together, it is entirely possible for an error existing ([NvHTTP]/[com.limelight.nvstream.NvConnection]/[Game])
     * code to completely kill [Game] and also keep the [com.limelight.nvstream.NvConnection] alive (until it timeout)
     *
     * To avoid this issue, we will use this callback [onSurfaceDestroyedWhileConnecting] in [surfaceDestroyed], then:
     * 1. Disable any virtual display and revert to default video resolution
     * 2. Show a [RnGameError] (activity with same UI) to inform user what happened
     */
    override fun onSurfaceDestroyedWhileConnecting() {
        super.onSurfaceDestroyedWhileConnecting()
        val displayModeOption = RemotePlaySettingsPref.displayMode
        Timber.v("onSurfaceDestroyedWhileConnecting: displayModeOption=$displayModeOption")
        val launchQueryResolution = (when(displayModeOption) {
            DisplayModeOption.DuplicateDisplay -> this.conn.context.streamConfig?.let { "${it.width}x${it.height}x${it.launchRefreshRate}" }
            else -> {
                val lastLaunchQuery = this.conn?.lastLaunchQuery
                // for logging
                val launchQueryParams = lastLaunchQuery?.split('&')
                    ?.mapNotNull {
                            nvp ->
                        nvp.split('=').takeIf { it.size == 2 }?.let { it[0] to it[1] }
                    }?.toMap() ?: emptyMap()

                listOfNotNull(
                    launchQueryParams["virtualDisplayMode"],
                    launchQueryParams["mode"],
                ).firstOrNull()
            }
        })?.toResolution()
        Timber.v("onSurfaceDestroyedWhileConnecting: launchQueryResolution=$launchQueryResolution NeuronBridge.isLaunchWithVirtualDisplay=${NeuronBridge.isLaunchWithVirtualDisplay}")


    
        if(!isUserWantsToLeave()) {
            if(launchQueryResolution != null) {
                // disable virtual display if currently using it
                if(!launchQueryResolution.isStandardResolution() || launchQueryResolution.height > 720) {
                    // disable virtual display
                    NeuronBridge.fallbackToDuplicateDisplay()
                }

                // start a separate activity and show an alert
                handleUngracefulTermination(
                    getString(R.string.rn_unsupported_native_resolution_title),
                    getString(R.string.rn_unsupported_native_resolution_msg, launchQueryResolution),
                )
            } else {
                debugToast("launchQueryResolution missing")
            }
        }
        // finish current activity since we are showing an alert in a separate activity anyway
        if(!isFinishing) {
            finish()
        }
    }

    /**
     * There are situations where [Game.finish] will be called inside [Game.onStop]. The [onStop] would
     * have been called by a [surfaceDestroyed] or other means.
     *
     * This ends up in a situation where the [Game] will get closed without telling user what happened.
     * Even if the code wants to show a dialog, it will not work since it will be bound to the activity (which
     * might be closing also)
     *
     * This function will user [RnGameError] to show another activity just to show a error message to user.
     */
    override fun handleUngracefulTermination(title: String, message: String?) : Boolean {
        message ?: return false
        Timber.v("handleUngracefulTermination: wasUngracefulTerminationHandled=$wasUngracefulTerminationHandled,  $title, $message")
        wasUngracefulTerminationHandled = true
        // start a separate activity and show an alert
        startActivity(createAlertIntent(this,
            title,
            message))
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        view.onDestroy()
    }

    override fun displayTransientMessage(message: String?) {
        message ?: return
        view.showNotificationText(message)
    }

    private fun updateComputerMetaTimestamp() {
        val uuid = intent.getStringExtra(EXTRA_PC_UUID) ?: return
        val existComputerMeta = RemotePlaySettingsPref.getComputerMeta(uuid)
        val newComputerMeta = existComputerMeta?.copy(
            lastUsedTimestamp = now()
        ) ?: ComputerMeta(
            lastUsedTimestamp = now()
        )
        RemotePlaySettingsPref.setComputerMeta(uuid, newComputerMeta)
    }
  
}

