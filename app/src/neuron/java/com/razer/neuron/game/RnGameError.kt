package com.razer.neuron.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.limelight.R
import timber.log.Timber

import com.razer.neuron.common.logAndRecordException
import com.razer.neuron.common.toast

import com.razer.neuron.extensions.applyTransition
import com.razer.neuron.extensions.setDrawIntoSafeArea
import com.razer.neuron.game.helpers.RnGameView
import com.razer.neuron.nexus.NexusContentProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


/**
 * A [android.app.Activity] to restart [RnGame]
 * or just show a message.
 *
 * Using the same splash layout as [RnGame], this activity is intended to be shown after
 * [RnGame] is either forced to close or close by itself, so that we let user know what happened
 * to the original game session.
 */
class RnGameError : ComponentActivity() {

    private val isCropToSafeArea by lazy { NexusContentProvider.settingsSource.isVirtualDisplayCropToSafeArea(this) }
    private val isVirtualDisplayMode by lazy { NexusContentProvider.settingsSource.isVirtualDisplayMode(this) }


    companion object {
        const val MODE_RESTART_GAME = "MODE_RESTART_GAME"
        const val MODE_SHOW_ALERT = "MODE_SHOW_ALERT"

        private const val EXTRA_MODE = "EXTRA_MODE"
        private const val EXTRA_DELAY_MS = "EXTRA_DELAY_MS"
        private const val EXTRA_STAGE_MSG = "EXTRA_STAGE_MSG"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_MESSAGE = "EXTRA_MESSAGE"

        /**
         * For [RnGame] to read when mode is [MODE_RESTART_GAME]
         */
        const val EXTRA_WAS_RESTARTED = "EXTRA_WAS_RESTARTED"


        private var currentInstance : WeakReference<RnGameError>? = null
        /**
         * Call [android.app.Activity.finish] on existing [RnGameError] so that
         * the delay will be cancel and restart will not be launched.
         */
        fun cancelPendingRestart() : Boolean {
            return currentInstance?.get()
                ?.let {
                    it.delayedRestartJob?.cancel()
                    it.finish()
                    true
                } ?: false
        }

        /**
         * Call [android.app.Activity.finish] on existing [RnGameError] so that
         * the message from [MODE_SHOW_ALERT] won't be shown anymore.
         */
        fun finishShownMessage() {
            currentInstance?.get()?.takeIf { it.mode == MODE_SHOW_ALERT }?.finish()
        }


        /**
         * Create an [Intent] to start [RnGameError]
         *
         * [RnGameError] will show the same splash UI as [RnGame] (i.e. [RnGameView])
         * and immediately also show [AlertDialog] via [RnGameView.showAlertDialog]
         */
        fun createAlertIntent(context : Context, title : String, message : String) : Intent {
            return Intent(context, RnGameError::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SHOW_ALERT)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
            }
        }

        /**
         * Create an [Intent] to start [RnGameError]
         *
         * [RnGameError] will show the same splash UI as [RnGame] (i.e. [RnGameView])
         * but after [delayMs] it will [startActivity] of [RnGame] using [originalIntentExtra]
         */
        fun createRestartIntent(context : Context, delayMs : Long = 3000, stageMsg : String?, originalIntentExtra : Bundle) : Intent {
            return Intent(context, RnGameError::class.java).apply {
                putExtra(EXTRA_MODE, MODE_RESTART_GAME)
                putExtra(EXTRA_DELAY_MS, delayMs)
                putExtra(EXTRA_STAGE_MSG, stageMsg)
                putExtras(originalIntentExtra)
            }
        }
    }
    private val view by lazy { RnGameView(this) }

    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: error("Must specify $EXTRA_MODE") }

    private var delayedRestartJob : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTransition()
        currentInstance = WeakReference(this)
        super.onCreate(savedInstanceState)
        val isFullscreenLayout = !isVirtualDisplayMode || !isCropToSafeArea
        setDrawIntoSafeArea(isFullscreenLayout)
        setContentView(R.layout.rn_activity_game_error)
        runCatching {
            when(mode) {
                MODE_RESTART_GAME -> onCreateRestartGameMode()
                MODE_SHOW_ALERT -> onCreateShowAlertMode()
                else -> error("$mode not supported")
            }
        }.exceptionOrNull()?.let {
            toast(it.message)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        if(currentInstance?.get() == this) {
            currentInstance = null
        }
    }


    private fun onCreateShowAlertMode() {
        view.showLoadingProgress("")
        val intent = intent ?: error("Missing intent")
        val title = intent.getStringExtra(EXTRA_TITLE)
        val message = intent.getStringExtra(EXTRA_MESSAGE)
        lifecycleScope.launch {
            delay(500)
            if(title != null && message != null) {
                view.showAlert(title, message)
            } else {
                error("Missing title/message")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        view.onDestroy()
        if(currentInstance?.get() == this) {
            currentInstance = null
        }
    }



    private fun onCreateRestartGameMode() {
        view.showLoadingProgress("")
        view.loadingText.text = intent?.getStringExtra(EXTRA_STAGE_MSG) ?: getString(R.string.rn_please_wait)
        val defaultDelayMs = intent?.getLongExtra(EXTRA_DELAY_MS, -1)?.takeIf { it > 0L } ?: run {
            toast("Must provide EXTRA_DELAY_MS")
            finish()
            return
        }
        delayedRestartJob?.cancel()
        delayedRestartJob = lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            if(throwable is CancellationException && isFinishing) {
                finish()
            }
        }) {
            val restartIntentResult = runCatching { RnGame.createStartStreamIntent(this@RnGameError, intent) }
            restartIntentResult.exceptionOrNull()?.let {
                logAndRecordException(it)
                finish()
                return@launch
            }
            // make sure we can get a restart intent first.
            val restartIntent = restartIntentResult.getOrNull() ?: run {
                finish()
                return@launch
            }
            restartIntent.apply {
                putExtra(EXTRA_WAS_RESTARTED, true)
            }
            Timber.w("onCreate: delay for ${defaultDelayMs}ms")
            delay(defaultDelayMs)
            if(!isActive) {
                Timber.w("onCreate: job cancelled")
                return@launch
            }
            Timber.w("onCreate: starting intent $restartIntent")
            startActivity(restartIntent)
            finish()
        }
    }
}