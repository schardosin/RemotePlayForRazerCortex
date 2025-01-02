package com.razer.neuron.main

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limelight.Game
import com.limelight.R
import com.limelight.binding.PlatformBinding
import com.limelight.computers.ComputerDatabaseManager
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.NvApp
import com.limelight.nvstream.http.NvHTTP
import com.razer.neuron.common.toast
import timber.log.Timber


import com.razer.neuron.di.IoDispatcher
import com.razer.neuron.di.UnexpectedExceptionHandler
import com.razer.neuron.extensions.hasNexusContentProviderPermission
import com.razer.neuron.game.helpers.RnGameIntentHelper
import com.razer.neuron.model.isDesktop
import com.razer.neuron.nexus.NexusContentProvider
import com.razer.neuron.nexus.NexusPackageStatus
import com.razer.neuron.nexus.getNexusPackageStatus
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.settings.StreamingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.LinkedList
import javax.inject.Inject


@HiltViewModel
class RnMainViewModel
@Inject constructor(
    private val application: Application,
    @UnexpectedExceptionHandler val unexpectedExceptionHandler: CoroutineExceptionHandler,
    @IoDispatcher val ioDispatcher : CoroutineDispatcher,
    private val computerDatabaseManager: ComputerDatabaseManager,
    private val streamingManager: StreamingManager
) : ViewModel(), RnGameIntentHelper {
    private val appContext by lazy { application }
    private val _navigation = MutableSharedFlow<RnMainModel.Navigation>()
    val navigation = _navigation.asSharedFlow()

    private val _state = MutableStateFlow<RnMainModel.State>(RnMainModel.State.Empty)
    val state = _state.asStateFlow()

    private fun RnMainModel.State.emit() = viewModelScope.launch { _state.emit(this@emit) }
    private fun RnMainModel.Navigation.emit() =
        viewModelScope.launch { _navigation.emit(this@emit) }

    private var launchIntent: Intent? = null

    private var wasSyncCompleted = false

    private val syncMutex = Mutex()




    fun onCreate(intent: Intent) {
        Timber.v("onCreate")
        launchIntent = intent
        wasSyncCompleted = false
        viewModelScope.launch { nextNavigation().emit() }
    }

    fun onNewIntent(intent: Intent) {
        launchIntent = intent
        wasSyncCompleted = false
        viewModelScope.launch { nextNavigation().emit() }
    }

    fun onUpdateNexusRejected() {
        RemotePlaySettingsPref.hasUserRejectedNexusUpdate = true
        viewModelScope.launch { nextNavigation().emit() }
    }

    private suspend fun nextNavigation(): RnMainModel.Navigation {
        val intent = launchIntent
        val nexusPackageStatus = appContext.getNexusPackageStatus()
        val isOobeCompleted =  RemotePlaySettingsPref.isOobeCompleted
        val isTosAccepted = RemotePlaySettingsPref.isTosAccepted
        val hasStartStreamIntent = intent != null && hasStartStreamIntent(intent)

        suspend fun startStream() : RnMainModel.Navigation {
            requireNotNull(intent)
            check(hasStartStreamIntent(intent))
            return wrapWithLoading {
                // last opportunity to sync before starting game
                if(!wasSyncCompleted && appContext.hasNexusContentProviderPermission()) {
                    appContext.sync()
                }
                withContext(ioDispatcher) {
                    val gameIntent = createStartStreamIntent(appContext, intent)
                    askBeforeStartStream(gameIntent)
                }
            }
        }

        return when {
            !isTosAccepted -> {
                RnMainModel.Navigation.Oobe(intent)
            }
            !isOobeCompleted -> {
                if(hasStartStreamIntent) {
                    startStream()
                } else {
                    RnMainModel.Navigation.Oobe(intent)
                }
            }
//            !RemotePlaySettingsPref.hasUserRejectedNexusUpdate && nexusPackageStatus == NexusPackageStatus.InvalidVersion && !appContext.hasNexusContentProviderPermission() -> {
//                RnMainModel.Navigation.UpdateNexus(nexusPackageStatus)
//            }
            !wasSyncCompleted -> {
                if(appContext.hasNexusContentProviderPermission()) {
                    wrapWithLoading { appContext.sync() }
                } else {
                    wasSyncCompleted = true
                }
                nextNavigation() // repeat again
            }
            hasStartStreamIntent -> startStream()
            else -> RnMainModel.Navigation.Landing
        }
    }

    private fun askBeforeStartStream(gameIntent : Intent) : RnMainModel.Navigation {
        val tag = "askBeforeStartStream"
        val computerDetailsUuid = requireNotNull(gameIntent.getStringExtra(Game.EXTRA_PC_UUID))
        val appId = requireNotNull(gameIntent.getIntExtra(Game.EXTRA_APP_ID, 0).takeIf { it > 0 })
        var appName = gameIntent.getStringExtra(Game.EXTRA_APP_NAME)
        val computerDetails = requireNotNull(computerDatabaseManager.getComputerByUUID(computerDetailsUuid)) {
            "Computer not found"
        }
        Timber.v("$tag: computerDetailsUuid $computerDetailsUuid")
        Timber.v("$tag: $appId ($appName)")
        val result = computerDetails.findActiveAddress(appContext, isUpdateThis = true)
        val activeAddress = computerDetails.activeAddress
        val runningGameId = computerDetails.runningGameId
        val httpsPort = computerDetails.httpsPort
        var runningGame : NvApp? = null
        var app : NvApp? = null
        if(activeAddress != null && runningGameId != 0 && httpsPort != 0) {
            runCatching {
                val http = NvHTTP(
                    activeAddress,
                    httpsPort,
                    null,
                    computerDetails.serverCert,
                    PlatformBinding.getCryptoProvider(appContext))
                val list = (http.appList ?: LinkedList())
                runningGame = list.firstOrNull { it.appId == runningGameId }
                app = list.firstOrNull { it.appId == appId }
            }.exceptionOrNull()?.let {
                Timber.w(it)
                Timber.w("$tag: error ${it.message}")
            } // can ignore error here since runningGameName is optional
        }

        appName = app?.appName ?: appName ?: "game"
        return if(result.isSuccess) {
            Timber.v("$tag: runningGameId=${runningGameId},runningGameName=${runningGame?.appName}, appId=${appId}")
            Timber.v("$tag: gameIntent=${gameIntent}")
            // NEUR-22, NEUR-75, NEUR-104
            val _runningGame = runningGame
            val runningGameIsDesktop = _runningGame?.isDesktop() == true
            val targetGameIsDesktop = app?.isDesktop() == true
            val hasRunningGame = runningGameId != 0

            if(hasRunningGame && !runningGameIsDesktop) {
                if(targetGameIsDesktop && _runningGame != null) {
                    // NEUR-104
                    RnMainModel.Navigation.Stream(Intent(gameIntent).apply {
                        putExtra(Game.EXTRA_APP_ID, _runningGame.appId)
                        putExtra(Game.EXTRA_APP_NAME, _runningGame.appName)
                    })
                } else {
                    if (runningGameId == appId) {
                        RnMainModel.Navigation.StartSameGameOrQuit(
                            startGameName = appName,
                            computerDetails = computerDetails,
                            gameIntent = gameIntent
                        )
                    } else {
                        RnMainModel.Navigation.ConfirmQuitThenStartDifferentGame(
                            runningGameName = runningGame?.appName,
                            startGameName = appName,
                            computerDetails = computerDetails,
                            gameIntent = gameIntent
                        )
                    }
                }
            } else {
                RnMainModel.Navigation.Stream(gameIntent)
            }
        } else {
            RnMainModel.Navigation.Stream(gameIntent)
        }
    }

    /**
     * For [RnMainModel.Navigation.StartSameGameOrQuit]
     */
    fun onStartGame(gameIntent : Intent) = viewModelScope.launch {
        Timber.v("onStartGame: gameIntent=${gameIntent}")
        RnMainModel.Navigation.Stream(gameIntent).emit()
    }

    /**
     * For [RnMainModel.Navigation.StartSameGameOrQuit]
     */
    fun onQuitGame(computerDetails: ComputerDetails) = viewModelScope.launch {
        wrapWithLoading {
            try {
                computerDetails.quitApp()
            } catch (t : Throwable) {
                toast(appContext.getString(R.string.rn_unable_quit_game_error, t.message))
                Timber.w("onQuitGame: ${t.message}")
                Timber.w(t)
                RnMainModel.Navigation.Error(t).emit()
            }
            RnMainModel.Navigation.Finish.emit()
        }
    }


    /**
     * For [RnMainModel.Navigation.ConfirmQuitThenStartDifferentGame]
     */
    fun onQuitThenStart(computerDetails : ComputerDetails, gameIntent : Intent, isIgnoreQuitAppError : Boolean = true) = viewModelScope.launch {
        wrapWithLoading {
            try {
                // Calling Game (i.e RnGame) will quit the game automatically
                // no need to call computerDetails.quitApp()
                RnMainModel.Navigation.Stream(gameIntent).emit()
            } catch (t : Throwable) {
                toast(appContext.getString(R.string.rn_unable_quit_game_error, t.message))
                Timber.w("onQuitGameConfirmed: ${t.message}")
                Timber.w(t)
                if(isIgnoreQuitAppError) {
                    RnMainModel.Navigation.Stream(gameIntent).emit()
                } else {
                    RnMainModel.Navigation.Error(t).emit()
                }
            }
        }
    }

    private suspend fun ComputerDetails.quitApp() {
        val activeAddress = activeAddress
        Timber.v("quitApp: activeAddress=${activeAddress}")
        check(activeAddress != null) { "No active address" }
        withContext(ioDispatcher) {
            val http = NvHTTP(
                activeAddress,
                httpsPort,
                null,
                serverCert,
                PlatformBinding.getCryptoProvider(appContext))
            Timber.v("quitApp: starting quitApp")
            http.quitApp()
        }
    }


    private suspend fun <T> wrapWithLoading(task : suspend () -> T) : T{
        return try {
            RnMainModel.State.ShowLoading.emit()
            task()
        } finally {
            RnMainModel.State.HideLoading.emit()
        }
    }

    private suspend fun Context.sync() {
        try {
            syncMutex.withLock {
                check(!wasSyncCompleted)
                NexusContentProvider.sync(this@sync)
            }
        } catch (t : Exception) {
            Timber.w(t.message)
        } finally {
            wasSyncCompleted = true
        }
    }

    companion object {
        val TAG = "RnMainViewModel"
    }
}

class RnMainModel {
    sealed class Navigation(val id: String) {
        object Landing : Navigation("landing")

        object Settings : Navigation("settings")

        class Oobe(val launchIntent : Intent?) : Navigation("oobe")

        @Deprecated("Neuron can function by itself")
        class UpdateNexus(val nexusPackageStatus: NexusPackageStatus) : Navigation("update_nexus")

        class Stream(val gameIntent: Intent) : Navigation("stream")

        /**
         * Only for cases when existing game is the same game as [startGameName]
         */
        class StartSameGameOrQuit(val startGameName : String, val computerDetails : ComputerDetails, val gameIntent : Intent) : Navigation("start_or_quit")

        /**
         * Only for cases when existing game is not the same game as [startGameName]
         */
        class ConfirmQuitThenStartDifferentGame(val runningGameName: String?, val startGameName : String, val computerDetails : ComputerDetails, val gameIntent : Intent) : Navigation("confirm-quit")

        class Error(val throwable: Throwable) : Navigation("error")

        object Finish : Navigation("finish")
    }

    sealed class State(val id: String) {
        object Empty : State("empty")

        object ShowLoading : State("show_loading")

        object HideLoading : State("hide_loading")
    }
}
