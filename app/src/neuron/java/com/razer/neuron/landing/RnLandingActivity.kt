package com.razer.neuron.landing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.limelight.R
import com.limelight.computers.ComputerManagerListener
import com.limelight.computers.ComputerManagerListener2
import com.limelight.computers.ComputerManagerService
import com.limelight.databinding.RnActivityLandingBinding
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.NvApp
import com.razer.neuron.common.BaseControllerActivity
import com.razer.neuron.common.materialAlertDialogTheme
import com.razer.neuron.common.toast
import com.razer.neuron.di.IoDispatcher
import com.razer.neuron.extensions.dismissSafely
import com.razer.neuron.extensions.getUserFacingMessage
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.setDescendantsUnfocusableUntilLayout
import com.razer.neuron.extensions.hasGenericController
import com.razer.neuron.extensions.visible
import com.razer.neuron.extensions.visibleIf
import com.razer.neuron.model.ButtonHint
import com.razer.neuron.model.ControllerInput
import com.razer.neuron.oobe.ButtonHintsBarHelper
import com.razer.neuron.settings.RnSettingsActivity
import com.razer.neuron.settings.devices.DeviceAction
import com.razer.neuron.settings.devices.RnDevicesViewModel
import com.razer.neuron.settings.manualpairing.RnManualPairingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException
import java.net.UnknownHostException
import javax.inject.Inject


@AndroidEntryPoint
class RnLandingActivity : BaseControllerActivity(), LandingItemAdapter.Listener, ButtonHintsBarHelper {

    companion object {
        fun createIntent(context: Context) = Intent(context, RnLandingActivity::class.java)
    }

    private lateinit var binding: RnActivityLandingBinding

    private val viewModel: RnLandingViewModel by viewModels()

    private var managerBinder: ComputerManagerService.ComputerManagerBinder? = null
    private var freezeUpdates = false
    private var runningPolling = false
    private var inForeground = false

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher : CoroutineDispatcher

    private val tvLoadingTitle by lazy {
        binding.loadingLayoutContainer.findViewById<TextView>(R.id.tv_loading_title)
    }

    private val tvLoadingSubtitle by lazy {
        binding.loadingLayoutContainer.findViewById<TextView>(R.id.tv_loading_subtitle)
    }

    override val buttonHintsBar by lazy { binding.buttonHintsBar }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            val localBinder = binder as ComputerManagerService.ComputerManagerBinder
            Thread {
                localBinder.waitForReady()
                managerBinder = localBinder
                startComputerUpdates()
            }.start()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            managerBinder = null
        }
    }



    private val rv by lazy {
        binding.rvLandingItems.apply {
            adapter = LandingItemAdapter()
                .apply {
                    listener = this@RnLandingActivity
                }
            itemAnimator = null
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(LandingItemAdapter.ItemDecoration())
        }
    }

    private val adapter get() = rv.adapter as LandingItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RnActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTopToolBar()
        bindComputerService()
        observe()
        viewModel.onViewCreated()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val controllerInput = ControllerInput.entries.firstOrNull { it.keyCode == event?.keyCode }
        return if(controllerInput != null) {
            viewModel.onControllerInput(controllerInput, event?.action == KeyEvent.ACTION_UP)
            true
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rn_landing_top_bar_menu, menu)
        Handler(Looper.getMainLooper()).post {
            val menuItemView = findViewById<View>(R.id.menu_top_bar_settings)
            menuItemView?.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    viewModel.handleFocus(FocusItem.Settings)
                }
            }
        }
        return true
    }

    private fun setupTopToolBar() {
        setSupportActionBar(binding.topToolBar)
        binding.topToolBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top_bar_settings -> {
                    startActivity(RnSettingsActivity.createIntent(this))
                    true
                }
                else -> false
            }
        }
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.topToolBar.setDescendantsUnfocusableUntilLayout()
    }

    private fun bindComputerService() {
        bindService(Intent(baseContext, ComputerManagerService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }

    private fun startPairing(details: ComputerDetails) {
        val managerBinder = managerBinder
        if (managerBinder == null) {
            toast(getString(R.string.error_manager_not_running))
            return
        }
        viewModel.onPair(managerBinder.uniqueId, details)
    }

    private fun startStream(details: ComputerDetails) {
        managerBinder?.let { binder ->
            viewModel.onStartStream(
                binder.uniqueId,
                details
            )
        } ?: run {
            toast(getString(R.string.error_manager_not_running))
        }
    }

    private fun startComputerUpdates() {
        if (!runningPolling && inForeground) {
            freezeUpdates = false
            val managerBinder = managerBinder
            if (managerBinder != null) {
                viewModel.onBeforeStartComputerUpdates()
            }
        }
    }

    /**
     * Need to update the [ComputerDetails] data in the [managerBinder], after [ComputerDetails] is updated.
     */
    private fun updateComputerDetails(computerDetails: ComputerDetails) {
        val managerBinder = managerBinder ?: return
        managerBinder.getComputer(computerDetails.uuid)?.serverCert = computerDetails.serverCert
        managerBinder.invalidateStateForComputer(computerDetails.uuid)
    }

    private fun stopComputerUpdates(wait: Boolean) {
        if (!runningPolling) {
            return
        }
        freezeUpdates = true
        managerBinder?.stopPolling()
        if (wait) {
            managerBinder?.waitForPollingStopped()
        }
        runningPolling = false
    }

    private var pinCodeDialog : AlertDialog? = null

    private fun showPinDialog(pinCode: String, computerName : String) {
        pinCodeDialog.dismissSafely()
        pinCodeDialog = MaterialAlertDialogBuilder(this, materialAlertDialogTheme())
            .setCancelable(true)
            .setTitle(getString(R.string.rn_pin_code_title, computerName))
            .setView(LayoutInflater.from(this).inflate(R.layout.rn_layout_pin_code, null).apply {
                findViewById<TextView>(R.id.tv_pin_code).text = pinCode
            })
            .setPositiveButton(android.R.string.ok) { _, p ->

            }
            .setOnDismissListener {
                hideLoading()
            }
            .show()
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.viewSharedFlow.collect {
                Timber.v("observe: viewStateFlow: ${it.javaClass.simpleName}")
                when (it) {
                    is LandingState.StartComputerUpdates -> it.handle()
                    is LandingState.StopComputerUpdates -> it.handle()
                    is LandingState.StartComputerPolling -> it.handle()
                    is LandingState.ShowContent -> it.handle()
                    is LandingState.ShowPin -> it.handle()
                    is LandingState.HidePin -> pinCodeDialog.dismissSafely()
                    is LandingState.ShowLoading -> it.handle()
                    is LandingState.HideLoading -> it.handle()
                    is LandingState.ShowMessage -> toast(it.message)
                    is LandingState.ShowError -> it.handle()
                    is LandingState.StartManualPairing -> it.handle()
                    is LandingState.StartStreaming -> startActivity(it.intent)
                    is LandingState.InvalidateComputer -> it.handle()
                    is LandingState.Action -> it.handle()
                    else -> com.razer.neuron.common.debugToast("${it.javaClass.simpleName} not handled")
                }
            }
        }
        lifecycleScope.launch {
            viewModel.focusStateFlow.collect { focusItem ->
                updateButtonHintsBarVisibility()
                val buttonHints = focusItem.buttonHints
                if (buttonHints != null) {
                    updateButtonHints(buttonHints)
                } else {
                    buttonHintsBar.gone()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.appListFlow.collect { appList ->
                showAppSelectionDialog(appList)
            }
        }
    }

    private fun LandingState.StartComputerPolling.handle() {
        managerBinder?.let {
            it.restartPolling(object : ComputerManagerListener2 {
                override fun notifyComputerUpdated(details: ComputerDetails?) {
                    if (!freezeUpdates) {
                        runOnUiThread {
                            viewModel.onComputerDetailsUpdated(fromNeuron = false, details)
                        }
                    }
                }

                override fun notifyComputerRemoved(details: ComputerDetails) {
                    if (!freezeUpdates) {
                        runOnUiThread {
                            viewModel.onComputerDetailsRemoved(fromNeuron = false, details)
                        }
                    }
                }
            })
            runningPolling = true
        }
    }

    private fun LandingState.InvalidateComputer.handle() {
        lifecycleScope.launch {
            withContext(ioDispatcher) {
                updateComputerDetails(computerDetails)
            }
        }
    }

    private fun LandingState.Action.handle() {
        when(action) {
            is LandingAction.Pair -> {
                startPairing(action.computerDetails)
            }
            is LandingAction.Unpair -> {
                managerBinder?.uniqueId?.let {
                    viewModel.onUnpair(it, action.computerDetails)
                }
            }
            is LandingAction.Retry -> {
                viewModel.sendWakeOnLan(action.computerDetails)
            }
            is LandingAction.StartStreaming -> {
                startStream(action.computerDetails)
            }
            is LandingAction.ManualPairing -> {
                startActivity(RnManualPairingActivity.getIntent(this@RnLandingActivity))
            }
            is LandingAction.Settings -> {
                startActivity(RnSettingsActivity.createIntent(this@RnLandingActivity))
            }
        }
    }

    private fun LandingState.StartComputerUpdates.handle() = startComputerUpdates()

    private fun LandingState.StopComputerUpdates.handle() = stopComputerUpdates(wait)

    private fun LandingState.ShowContent.handle() {
        Timber.v("ShowContent: size=${this.items.size} ${items.joinToString { it.toString() }}")
        adapter.submitList(items, rv)
    }



    private fun LandingState.StartManualPairing.handle() {
        startActivity(RnManualPairingActivity.getIntent(this@RnLandingActivity))
    }


    private fun LandingState.ShowError.handle() {
        Timber.w(error)
        toast(error.getUserFacingMessage())
    }

    private fun LandingState.ShowPin.handle() {
        val isInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (isInForeground) {
            showPinDialog(pinCode, computerDetails.name)
        }
    }

    private fun LandingState.ShowLoading.handle() {
        binding.loadingLayoutContainer.visible()
        when(tag) {
            RnDevicesViewModel.LOADING_TAG_PAIR -> {
                tvLoadingTitle.text = getString(R.string.pairing)
                with(tvLoadingSubtitle) {
                    text = getString(R.string.rn_pairing_hint)
                    visible()
                }
            }
            else -> {
                tvLoadingTitle.text = getString(R.string.rn_please_wait)
                with(tvLoadingSubtitle) {
                    gone()
                }
            }
        }
    }

    private fun LandingState.HideLoading.handle() = hideLoading()

    private fun hideLoading() {
        binding.loadingLayoutContainer.gone()
    }

    override fun onButtonHintsBarButtonHintClicked(buttonHint: ButtonHint) {
        viewModel.onButtonHintClicked(buttonHint)
    }

    override fun onActionFocus(actionItem: LandingItem.ActionItem) {
        when (actionItem) {
            is LandingItem.AddManually -> viewModel.handleFocus(FocusItem.AddPcManually)
            else -> Unit
        }
    }

    override fun onComputerFocus(computerDetails: ComputerDetails) {
        viewModel.handleFocus(FocusItem.Computer(computerDetails))
    }

    override fun onComputerClicked(computer: ComputerDetails, action: DeviceAction) {
        when (action) {
            DeviceAction.UNPAIR -> {
                managerBinder?.let { binder ->
                    viewModel.onUnpair(binder.uniqueId, computer)
                }
            }
            DeviceAction.PAIR -> {
                startPairing(computer)
            }
            DeviceAction.STREAM -> {
                viewModel.onStartStream(managerBinder?.uniqueId ?: "", computer)
            }
            DeviceAction.WOL -> {
                viewModel.sendWakeOnLan(computer)
            }
        }
    }

    override fun onActionClicked(actionItem: LandingItem.ActionItem) {
        viewModel.onActionClicked(actionItem)
    }

    private fun updateButtonHintsBarVisibility() = buttonHintsBar.visibleIf { hasGenericController() }

    override fun onUsbDeviceAttached() {
        viewModel.refreshFocusItems()
        updateButtonHintsBarVisibility()
    }

    override fun onUsbDeviceDetached() {
        viewModel.refreshFocusItems()
        updateButtonHintsBarVisibility()
    }

    override fun onActionBootCompleted() {
        viewModel.refreshFocusItems()
        updateButtonHintsBarVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()
        pinCodeDialog.dismissSafely()
        if (managerBinder != null) {
            unbindService(serviceConnection)
        }
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        startComputerUpdates()
        updateButtonHintsBarVisibility()
    }

    override fun onPause() {
        super.onPause()
        inForeground = false
        stopComputerUpdates(false)
    }

    private fun showAppSelectionDialog(appList: List<NvApp>) {
        val appNames = appList.map { it.appName }.toTypedArray()
        MaterialAlertDialogBuilder(this, materialAlertDialogTheme())
            .setTitle(R.string.title_select_app)
            .setItems(appNames) { _, which ->
                val selectedApp = appList[which]
                managerBinder?.let { binder ->
                    viewModel.startStreamWithApp(binder.uniqueId, null, selectedApp)
                }
            }
            .setNegativeButton(R.string.rn_cancel, null)
            .show()
    }
}
