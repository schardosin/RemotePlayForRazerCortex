package com.razer.neuron.settings.manualpairing

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.limelight.BuildConfig
import com.limelight.R
import com.limelight.computers.ComputerManagerService
import com.limelight.computers.ComputerManagerService.ComputerManagerBinder
import com.limelight.databinding.RnActivityManualPairingBinding
import com.limelight.databinding.RnLayoutLoadingBinding
import com.razer.neuron.common.BaseActivity
import timber.log.Timber
import com.razer.neuron.common.materialAlertDialogTheme

import com.razer.neuron.exceptions.NeuronPairingException
import com.razer.neuron.extensions.dismissSafely
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.hideKeyboard
import com.razer.neuron.extensions.setHintOnlyWhenFocused
import com.razer.neuron.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RnManualPairingActivity : BaseActivity() {

    companion object {
        const val TAG: String = "RnManualPairingActivity"

        fun getIntent(activity: Activity): Intent {
            return Intent(activity, RnManualPairingActivity::class.java)
        }
    }

    private val viewModel: RnManualPairingViewModel by viewModels()

    private lateinit var binding: RnActivityManualPairingBinding
    private var pinCodeDialog : AlertDialog? = null
    private var isDebugging = BuildConfig.DEBUG// || isTesterBadgeAppInstalled()

    private var managerBinder: ComputerManagerBinder? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            managerBinder = binder as ComputerManagerBinder
        }

        override fun onServiceDisconnected(className: ComponentName) {
            managerBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RnActivityManualPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindComputerService()
        observe()
        setupUI()
    }

    private fun bindComputerService() {
        bindService(
            Intent(this, ComputerManagerService::class.java), serviceConnection, BIND_AUTO_CREATE
        )
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.viewSharedFlow.collect {
                Timber.v("viewStateFlow: $it")
                when (it) {
                    is ManualPairingState.OnPaired -> it.handle()
                    is ManualPairingState.ShowLoading -> it.handle()
                    is ManualPairingState.HideLoading -> it.handle()
                    is ManualPairingState.Error -> { it.handle() }
                    is ManualPairingState.ShowPin -> it.handle()
                    is ManualPairingState.HidePin -> pinCodeDialog.dismissSafely()
                    is ManualPairingState.DismissPinDialog -> it.handle()
                }
            }
        }
    }

    private fun setupUI() {
        binding.etAddressForPairing.requestFocus()
        binding.etAddressForPairing.setHintOnlyWhenFocused("192.168.1.x")
        binding.etAddressForPairing.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                binding.textInputLayout.error = null
                enableAddButton(s.isNotEmpty())
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
        })
        enableAddButton(false)
        binding.btnAdd.isEnabled = false
        binding.btnAdd.setOnClickListener {
            onIPAddressEntered(binding.etAddressForPairing.text.toString())
        }
        binding.etAddressForPairing.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnAdd.performClick()
                true
            } else {
                false
            }
        }
        onBackPressedDispatcher.addCallback {
            finish()
        }

        val toolbar = binding.topToolBar
        toolbar.title = getString(R.string.title_add_pc)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun enableAddButton(enabled: Boolean) {
        binding.btnAdd.isEnabled = enabled
        binding.btnAdd.alpha = if (enabled) 1f else 0.3f
    }

    private fun onIPAddressEntered(address: String) {
        hideKeyboard(binding.root)
        binding.textInputLayout.error = null
        viewModel.onIPAddressEntered(managerBinder?.uniqueId, address) {
            managerBinder?.addComputerBlocking(it) == true
        }
    }

    private fun ManualPairingState.OnPaired.handle() {
        finish()
    }

    private fun ManualPairingState.ShowLoading.handle() {
        binding.loadingLayoutContainer.visible()
        RnLayoutLoadingBinding.bind(binding.root).tvLoadingTitle.text = getString(R.string.rn_connecting)
        with(RnLayoutLoadingBinding.bind(binding.root).tvLoadingSubtitle) {
            text = getString(R.string.rn_manual_pairing_loading_content)
            visible()
        }
    }

    private fun ManualPairingState.HideLoading.handle() {
        hideLoading()
    }

    private fun hideLoading() {
        binding.loadingLayoutContainer.gone()
    }

    private fun ManualPairingState.Error.handle() {
        val message = if (isDebugging) {
            exception.message
        } else {
            getString(R.string.rn_warning_could_not_connect_to_host)
        }
        binding.textInputLayout.error = message
    }

    private fun ManualPairingState.ShowPin.handle() {
        pinCodeDialog.dismissSafely()
        pinCodeDialog = MaterialAlertDialogBuilder(this@RnManualPairingActivity, materialAlertDialogTheme())
            .setCancelable(true)
            .setTitle(getString(R.string.rn_pin_code_title, computerName))
            .setView(LayoutInflater.from(this@RnManualPairingActivity).inflate(R.layout.rn_layout_pin_code, null).apply {
                findViewById<TextView>(R.id.tv_pin_code).text = pinCode
            })
            .setPositiveButton(android.R.string.ok) { _, p ->

            }
            .setOnDismissListener {
                hideLoading()
            }
            .show()
    }

    private fun ManualPairingState.DismissPinDialog.handle() {
        pinCodeDialog.dismissSafely()
    }

    override fun onStop() {
        pinCodeDialog?.dismissSafely()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (managerBinder != null) {
            unbindService(serviceConnection)
        }
    }
}
