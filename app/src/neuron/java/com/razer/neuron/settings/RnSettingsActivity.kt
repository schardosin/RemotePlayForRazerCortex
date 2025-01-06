package com.razer.neuron.settings

import SettingsItem
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView
import com.limelight.R
import com.limelight.databinding.RnActivitySettingsBinding
import com.razer.neuron.common.BaseActivity
import com.razer.neuron.common.toast
import com.razer.neuron.extensions.requestFocus
import com.razer.neuron.extensions.setDescendantsUnfocusableUntilLayout
import com.razer.neuron.model.ControllerInput
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.utils.flattenDescendants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AndroidEntryPoint
class RnSettingsActivity : BaseActivity() {


    companion object {
        const val REQUEST_CODE_OPEN_DOCUMENT_TREE = 12346
        const val EXTRA_FILE_DISPLAY_NAME = "EXTRA_FILE_DISPLAY_NAME"
        const val EXTRA_FILE_MIME_TYPE = "EXTRA_FILE_MIME_TYPE"
        fun createIntent(context: Context) = Intent(context, RnSettingsActivity::class.java)
    }

    /**
     * BAA-2220
     */
    private var navigationBehavior : NavigationBehavior = NavigationBehavior.default // NavigationBehavior.NavigateOnFocus
        set(value) {
            field = value
            /**
             * This is needed, as the toggle [RnNavigationView.isRefocusCheckedItem] is made for
             * the behavior [NavigationBehavior.NavigateOnFocus] to avoid navigation change
             */
            navView.isRefocusCheckedItem = (navigationBehavior == NavigationBehavior.NavigateOnFocus)
        }

    private val navigationHandler = Handler(Looper.getMainLooper())

    private lateinit var binding: RnActivitySettingsBinding

    private val navView by lazy { binding.navView }
    private val viewModel: RnSettingsViewModel by viewModels()

    private var globalFocusJob: Job? = null

    /**
     * [RnSettingsActivity] should show navigation bar to match system settings page
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RnActivitySettingsBinding.inflate(layoutInflater)
        navigationBehavior = NavigationBehavior.NavigateOnSelect // BAA-2220 (as per Kevin instruction, we should use standard m3 UX behavior)

        setContentView(binding.root)
        setupNavigationView()
        setupToolbar()
    }

    /**
     * See BAA-2220
     */
    @SuppressLint("RestrictedApi")
    private val activityFocusChangedListener =
        OnGlobalFocusChangeListener { _, newView ->
            if (newView is NavigationMenuItemView) {
                // only the second focus is correct
                if (navigationBehavior == NavigationBehavior.NavigateOnFocus) {
                    val newViewItemId = newView.itemData.itemId
                    globalFocusJob?.cancel()
                    globalFocusJob = lifecycleScope.launch {
                        delay(50)
                        /**
                         * isPerformNavigation is true because focus happens first
                         * and we want to navigate immediately
                         */
                        navView.selectItem(newViewItemId)
                    }
                }
            }
        }


    private fun setupNavigationView() {
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener(
            activityFocusChangedListener
        )
        navView.setNavigationItemSelectedListener {
            when(navigationBehavior){
                NavigationBehavior.NavigateOnSelect -> {
                    viewModel.navigationTo(it.itemId)
                }
                NavigationBehavior.NavigateOnFocus -> {
                    /**
                     * This is true because only touch focus/select
                     * will call here.
                     */
                    if(navView.checkedItem?.itemId != it.itemId) {
                        navView.selectItem(it.itemId)
                    }
                }
            }
            true
        }
        val menu = navView.menu
        viewModel.settingsGroupLiveData.observe(this) { settingsGroup ->
            fun Menu.addMenuItem(groupId: Int, order: Int, item: SettingsItem): MenuItem {
                return add(groupId, item.itemId, order, item.titleId).apply {
                    isCheckable = true
                    setIcon(item.iconId)
                }
            }

            menu.clear()
            settingsGroup.forEachIndexed { groupIndex, group ->
                val subMenu = menu.addSubMenu(group.groupId, Menu.NONE, groupIndex, group.titleId)
                group.items.forEachIndexed { index, item ->
                    subMenu.addMenuItem(group.groupId, index, item)
                }
            }
        }

        viewModel.settingsState.observe(this) {
            state ->
            when(state) {
                SettingsState.Finish -> finish()
                else -> Unit
            }
        }

        viewModel.currentSettingsItemLiveData.observe(this) {
            if (navigationBehavior == NavigationBehavior.NavigateOnSelect) {
                // isPerformNavigation is false because we are already navigating here
                navView.selectItem(it.itemId)
            }
            val targetId = it.navigationId
            val navController = binding.flContainer.getFragment<NavHostFragment>().navController
            if (navController.currentDestination?.id == targetId) return@observe
            navController.popBackStack(navController.currentDestination?.id ?: 0, inclusive = true)
            navController.navigate(targetId)
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.topToolBar
        toolbar.title = getString(R.string.rn_settings)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setDescendantsUnfocusableUntilLayout()
    }


    /**
     * See BAA-2220
     *
     * In com.google.android.material:material library:
     * - Version 1.11.0 or later, the dynamic color will not work with Samsung
     * - Version 1.10.0 or older, it has a bug where if you check a item (call [MenuItem.setChecked])
     *   it will focus on the first item of [NavigationView]. (fixed in 1.12.0+)
     *
     * This means that we can't have a version where dynamic color is supported by samsung AND
     * have this bug fixed by google, so we have to fix it ourselves with [NavigationMenuItemView.requestFocus]
     */
    @SuppressLint("RestrictedApi")
    private fun NavigationView.selectItem(
        /**
         * [SettingsItem.itemId]
         */
        itemId: Int,
        requestFocus: Boolean = true,
        isPerformNavigation : Boolean = navigationBehavior == NavigationBehavior.NavigateOnFocus) {
        navView.setCheckedItem(itemId)
        if (isPerformNavigation) {
            viewModel.navigationTo(itemId)
        }

        if (requestFocus) {
            navigationHandler.post {
                flattenDescendants()
                    .filterIsInstance<NavigationMenuItemView>()
                    .firstOrNull { it.isFocusable && it.itemData.itemId == itemId }
                    ?.requestFocus(true)
            }
        }
    }

    fun toggleDevMode() {
        viewModel.toggleDevMode()
        toast("Dev mode ${if (RemotePlaySettingsPref.isDevModeEnabled) "enabled" else "disabled"}")
    }


    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val controllerInput = ControllerInput.entries.firstOrNull { it.keyCode == event?.keyCode }
        return if(controllerInput != null) {
            viewModel.onControllerInput(controllerInput, event?.action == KeyEvent.ACTION_UP)
        } else {
            super.dispatchKeyEvent(event)
        }
    }
}



