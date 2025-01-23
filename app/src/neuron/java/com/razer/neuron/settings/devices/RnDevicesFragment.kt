package com.razer.neuron.settings.devices

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.limelight.R
import com.limelight.computers.ComputerManagerListener
import com.limelight.computers.ComputerManagerListener2
import com.limelight.computers.ComputerManagerService
import com.limelight.databinding.RnFragmentDevicesBinding
import com.limelight.nvstream.http.ComputerDetails
import timber.log.Timber
import com.razer.neuron.common.debugToast
import com.razer.neuron.common.materialAlertDialogTheme
import com.razer.neuron.common.showSingleChoiceItemsDialog
import com.razer.neuron.common.toast


import com.razer.neuron.di.IoDispatcher
import com.razer.neuron.extensions.dismissSafely
import com.razer.neuron.extensions.getUserFacingMessage
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.visible
import com.razer.neuron.model.AppThemeType
import com.razer.neuron.restartApp
import com.razer.neuron.settings.manualpairing.RnManualPairingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.net.UnknownHostException
import javax.inject.Inject


@AndroidEntryPoint
class RnDevicesFragment : Fragment(), DeviceItemAdapter.Listener {

    private lateinit var binding: RnFragmentDevicesBinding

    private val viewModel: RnDevicesViewModel by activityViewModels()

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
        binding.rvComputerItems.apply {
            adapter = DeviceItemAdapter(this@RnDevicesFragment.viewLifecycleOwner)
                .apply {
                    listener = this@RnDevicesFragment
                }
            // the recyclerview changes every 2s, we have to disable animation
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(DeviceItemAdapter.ItemDecoration())
            itemAnimator = null
        }
    }

    private val adapter get() = rv.adapter as DeviceItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RnFragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindComputerService()
        observe()
        viewModel.onViewCreated()
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.viewSharedFlow.collect {
                Timber.v("observe: viewStateFlow: ${it.javaClass.simpleName}")
                when (it) {
                    is DeviceState.StartComputerUpdates -> it.handle()
                    is DeviceState.StopComputerUpdates -> it.handle()
                    is DeviceState.StartComputerPolling -> it.handle()
                    is DeviceState.ShowContent -> it.handle()
                    is DeviceState.ShowPin -> it.handle()
                    is DeviceState.HidePin -> pinCodeDialog.dismissSafely()
                    is DeviceState.ShowLoading -> it.handle()
                    is DeviceState.HideLoading -> it.handle()
                    is DeviceState.ShowMessage -> toast(it.message)
                    is DeviceState.ShowError -> it.handle()
                    is DeviceState.RestartApp -> it.handle()
                    is DeviceState.ShowSelectTheme -> it.handle()
                    is DeviceState.StartManualPairing -> it.handle()
                    is DeviceState.StartStreaming -> startActivity(it.intent)
                    is DeviceState.InvalidateComputer -> it.handle()
                    else -> debugToast("${it.javaClass.simpleName} not handled")
                }
            }
        }

    }
    private fun DeviceState.InvalidateComputer.handle() {
        lifecycleScope.launch {
            withContext(ioDispatcher) {
                updateComputerDetails(computerDetails)
            }
        }
    }

    private fun DeviceState.StartComputerUpdates.handle() = startComputerUpdates()

    private fun DeviceState.StopComputerUpdates.handle() = stopComputerUpdates(wait)

    private fun DeviceState.ShowContent.handle() {
        Timber.v("ShowContent: size=${this.items.size} ${items.joinToString { it.toString() }}")
        adapter.submitList(items)
    }

    private fun DeviceState.StartManualPairing.handle() {
        startActivity(RnManualPairingActivity.getIntent(requireActivity()))
    }


    private fun DeviceState.ShowError.handle() {
        Timber.w(error)
        toast(error.getUserFacingMessage())
    }

    private fun DeviceState.ShowPin.handle() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            activity?.showPinDialog(pinCode, computerDetails.name)
        }
    }

    private fun DeviceState.RestartApp.handle() {
        val activity = activity ?: return
        binding.loadingLayoutContainer.visible()
        tvLoadingTitle.text = getString(R.string.rn_restarting)
        tvLoadingSubtitle.gone()
        lifecycleScope.launch {
            delay(1000)
            activity.restartApp()
        }
    }




    private var appThemeSelectionDialog : AlertDialog? = null

    /**
     * TODO:
     * To be removed
     */
    private fun DeviceState.ShowSelectTheme.handle() {
        val activity = activity ?: return
        appThemeSelectionDialog.dismissSafely()
        appThemeSelectionDialog = activity.showSingleChoiceItemsDialog(
            titleId = R.string.rn_theme_dialog_title,
            messageId = null,
            onSelected = { false } ,
            nameResList = AppThemeType.entries.map { it.title },
            options = AppThemeType.entries,
            defaultOption = defaultOption,
            isCancelable = true,
            positiveButton = getString(R.string.rn_apply_and_restart) to {
                viewModel.onSelectTheme(it)
            },
            negativeButton = getString(android.R.string.cancel) to { Unit }
        )
    }



    private fun DeviceState.ShowLoading.handle() {
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

    private fun DeviceState.HideLoading.handle() = hideLoading()

    private fun hideLoading() {
        binding.loadingLayoutContainer.gone()
    }


    private fun startComputerUpdates() {
        if (!runningPolling && inForeground) {
            freezeUpdates = false
            val managerBinder = managerBinder
            if (managerBinder != null) {
                viewModel.onBeforeStartComputerUpdates();
            }
        }
    }



    private fun DeviceState.StartComputerPolling.handle() {
        managerBinder?.let {
            it.restartPolling(object : ComputerManagerListener2 {
                override fun notifyComputerUpdated(details: ComputerDetails?) {
                    if (!freezeUpdates) {
                        requireActivity().runOnUiThread {
                            viewModel.onComputerDetailsUpdated(fromNeuron = false, details)
                        }
                    }
                }

                override fun notifyComputerRemoved(details: ComputerDetails) {
                    if (!freezeUpdates) {
                        requireActivity().runOnUiThread {
                            viewModel.onComputerDetailsRemoved(fromNeuron = false, details)
                        }
                    }
                }

            })
            runningPolling = true
        }
    }


    private fun bindComputerService() {
        requireActivity().bindService(
            Intent(context, ComputerManagerService::class.java), serviceConnection,
            AppCompatActivity.BIND_AUTO_CREATE
        )
    }

    private fun startPairing(details: ComputerDetails) {
        val managerBinder = managerBinder
        if (managerBinder == null) {
            toast(getString(R.string.error_manager_not_running))
            return
        }
        viewModel.onPair(managerBinder.uniqueId, details)
    }

    /**
     * Need to update the [ComputerDetails] data in the [managerBinder], after [ComputerDetails] is updated.
     */
    private fun updateComputerDetails(computerDetails: ComputerDetails) {
        val managerBinder = managerBinder ?: return
        managerBinder.getComputer(computerDetails.uuid)?.serverCert = computerDetails.serverCert
        managerBinder.invalidateStateForComputer(computerDetails.uuid)
    }

    private var pinCodeDialog : AlertDialog? = null

    private fun Activity.showPinDialog(pinCode: String, computerName : String) {
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

    override fun onComputerActionClicked(computer: ComputerDetails, action: DeviceAction) {
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
                startStream(computer)
            }
            DeviceAction.WOL -> {
                viewModel.sendWakeOnLan(computer)
            }
        }

    }


    override fun onActionClicked(actionItem: DeviceItem.ActionItem) {
        viewModel.onActionClicked(actionItem)
    }

    override fun onSwitchChecked(switchItem: DeviceItem.SwitchItem, isChecked: Boolean) {
        viewModel.onSwitchChecked(switchItem, isChecked)
    }

    override fun onDestroy() {
        super.onDestroy()
        pinCodeDialog.dismissSafely()
        if (managerBinder != null) {
            requireActivity().unbindService(serviceConnection)
        }
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        startComputerUpdates()

    }

    override fun onPause() {
        super.onPause()
        inForeground = false
        stopComputerUpdates(false)
    }

}
