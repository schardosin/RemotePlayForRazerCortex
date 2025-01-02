package com.razer.neuron.main

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.limelight.R
import com.limelight.databinding.RnActivityMainBinding
import com.razer.neuron.RnConstants
import com.razer.neuron.settings.RnSettingsActivity
import com.razer.neuron.common.BaseActivity
import timber.log.Timber
import com.razer.neuron.common.debugToast
import com.razer.neuron.common.materialAlertDialogTheme

import com.razer.neuron.extensions.applyTransition
import com.razer.neuron.extensions.dismissSafely
import com.razer.neuron.extensions.fadeOnStart
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.openInBrowser
import com.razer.neuron.extensions.toUriOrNull
import com.razer.neuron.extensions.visible
import com.razer.neuron.isShowVerboseErrorMessage
import com.razer.neuron.landing.RnLandingActivity
import com.razer.neuron.nexus.NexusPackageStatus
import com.razer.neuron.oobe.RnOobeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RnMainActivity : BaseActivity() {

    private var _binding: RnActivityMainBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val EXTRA_START_STREAMING = "StartStreaming"
        fun startMain(activity: Activity, intent: Intent? = null) {
            activity.startActivity(Intent(activity, RnMainActivity::class.java).apply {
                putExtras(intent ?: activity.intent)
            })
        }
    }

    private fun log(line: Any?) = Timber.v(line.toString())
    private val viewModel: RnMainViewModel by viewModels()

    override val onUnexpectedError = CoroutineExceptionHandler { cctx, e ->
        log("onUnexpectedError: ${e.message}")
        super.onUnexpectedError.handleException(cctx, e)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        applyTransition()
        super.onCreate(savedInstanceState)
        _binding = RnActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observe()

        viewModel.onCreate(intent ?: Intent())
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.navigation.collect {
                when (it) {
                    is RnMainModel.Navigation.Stream -> it.handle()
                    is RnMainModel.Navigation.UpdateNexus -> it.handle()
                    is RnMainModel.Navigation.Oobe -> it.handle()
                    is RnMainModel.Navigation.Settings -> it.handle()
                    is RnMainModel.Navigation.Landing -> it.handle()
                    is RnMainModel.Navigation.StartSameGameOrQuit -> it.handle()
                    is RnMainModel.Navigation.ConfirmQuitThenStartDifferentGame -> it.handle()
                    is RnMainModel.Navigation.Error -> it.handle()
                    is RnMainModel.Navigation.Finish -> finish()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.state.collect {
                when (it) {
                    is RnMainModel.State.ShowLoading -> it.handle()
                    is RnMainModel.State.HideLoading -> it.handle()
                    is RnMainModel.State.Empty -> it.handle()
                }
            }
        }

    }


    private var fadeAnimator: Animator? = null

    private fun RnMainModel.Navigation.StartSameGameOrQuit.handle() {
        alertDialog.dismissSafely()
        val title = getString(R.string.rn_start_game)
        val message = getString(R.string.rn_resume_or_quit_msg, startGameName)
        Timber.v("StartOrQuit.handle: $message")
        alertDialog = MaterialAlertDialogBuilder(this@RnMainActivity, materialAlertDialogTheme())
            .setCancelable(false)
            .setTitle(title)
            .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(R.string.applist_menu_resume) { _, p ->
                viewModel.onStartGame(gameIntent)
            }
            .setNegativeButton(R.string.rn_quit_and_dismiss) { _, p ->
                viewModel.onQuitGame(computerDetails = computerDetails)
            }
            .show()
    }



    private fun RnMainModel.Navigation.ConfirmQuitThenStartDifferentGame.handle() {
        alertDialog.dismissSafely()
        val title = getString(R.string.rn_start_game)
        val message = getString(R.string.rn_confirm_quit_then_start_msg, runningGameName ?: getString(R.string.rn_currently_running_game), startGameName)
        Timber.v("AskToQuitRunningGame.handle: $message")
        alertDialog = MaterialAlertDialogBuilder(this@RnMainActivity, materialAlertDialogTheme())
            .setCancelable(false)
            .setTitle(title)
            .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(R.string.rn_quit_and_start) { _, p ->
                viewModel.onQuitThenStart(computerDetails, gameIntent, true)
            }
            .setNegativeButton(R.string.rn_cancel) { _, p ->
                finish()
            }
            .show()
    }

    private fun RnMainModel.Navigation.Error.handle() {
        alertDialog.dismissSafely()
        var message = getString(R.string.conn_error_msg)
        if(isShowVerboseErrorMessage()) {
            message += "\n\n"
            message += throwable.message
        }
        alertDialog = MaterialAlertDialogBuilder(this@RnMainActivity, materialAlertDialogTheme())
            .setCancelable(false)
            .setTitle(getString(R.string.conn_error_title))
            .setMessage(message)
            .setPositiveButton(R.string.rn_dismiss) { _, p ->
                finish()
            }
            .show()
    }




    private fun RnMainModel.State.ShowLoading.handle() {
        fadeAnimator?.cancel()
        fadeAnimator = with(binding.layoutLoading) {
            if (isGone) {
                alpha = 0f
                visible()
            }
            fadeOnStart(1f).apply { start() }
        }
    }


    private fun RnMainModel.State.HideLoading.handle() {
        fadeAnimator?.cancel()
        fadeAnimator = with(binding.layoutLoading) {
            fadeOnStart(0f, View.GONE)
        }
    }

    private fun RnMainModel.State.Empty.handle() {
        binding.layoutLoading.gone()
    }

    private fun RnMainModel.Navigation.Landing.handle() {
        startActivity(RnLandingActivity.createIntent(this@RnMainActivity))
        finish()
    }


    private fun RnMainModel.Navigation.Settings.handle() {
        startActivity(RnSettingsActivity.createIntent(this@RnMainActivity))
        finish()
    }


    private fun RnMainModel.Navigation.Oobe.handle() {
        RnOobeActivity.startOobe(this@RnMainActivity, launchIntent)
        finish()
    }


    private fun RnMainModel.Navigation.UpdateNexus.handle() {
        when {
            nexusPackageStatus == NexusPackageStatus.InvalidVersion -> {
                showGetNexusDialog(
                    getString(R.string.rn_update_razer_nexus),
                    getString(R.string.rn_razer_nexus_needs_update_message),
                    getString(R.string.rn_update)
                )
            }
            nexusPackageStatus == NexusPackageStatus.NotInstalled -> {
                showGetNexusDialog(
                    getString(R.string.rn_download_razer_nexus),
                    getString(R.string.rn_razer_nexus_required_message),
                    getString(R.string.rn_download)
                )
            }
            else -> {
                debugToast("Why Update nexus")
                viewModel.onUpdateNexusRejected()
            }
        }
    }

    private var alertDialog: AlertDialog? = null


    private fun showGetNexusDialog(title : String, message: String, buttonText : String = getString(R.string.rn_download)) {
        alertDialog.dismissSafely()
        alertDialog = MaterialAlertDialogBuilder(this, materialAlertDialogTheme())
            .setCancelable(false)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { _, p ->
                redirectToRazerNexusPlayStore()
                finish()
            }
            .setNegativeButton(R.string.rn_cancel) { _, p ->
                viewModel.onUpdateNexusRejected()
            }
            .show()
    }

    private fun RnMainModel.Navigation.Stream.handle() {
        startGame(gameIntent)
        finish()
    }


    override fun onDestroy() {
        fadeAnimator?.cancel()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        intent ?: return
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.onNewIntent(intent)
    }


    private fun redirectToRazerNexusPlayStore() {
        RnConstants.NEXUS_DOWNLOAD_URL.toUriOrNull()
            ?.openInBrowser(this)
    }


    private fun startGame(gameIntent: Intent) = startActivity(gameIntent)

}