package com.razer.neuron.settings.devoptions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.limelight.R
import com.limelight.databinding.RnFragmentDevOptionsBinding
import timber.log.Timber
import com.razer.neuron.common.toast

import com.razer.neuron.di.IoDispatcher
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.setEnabledWithAlpha
import com.razer.neuron.extensions.setNumberDropDown
import com.razer.neuron.extensions.visible
import com.razer.neuron.model.SessionStats
import com.razer.neuron.nexus.NexusContentProvider
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.utils.now
import com.razer.neuron.utils.toDateTimeString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * BAA-2249
 *
 * A page for developer options
 *
 * No translation needed
 */
@AndroidEntryPoint
class RnDevOptionsFragment : Fragment(), DevOptionsHelper {

    private lateinit var binding: RnFragmentDevOptionsBinding

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher : CoroutineDispatcher

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RnFragmentDevOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            withContext(ioDispatcher) { NexusContentProvider.sync(requireContext()) }
            initExportCurrentSettings()
            reinitExportLastSessionStats()
            initSeparateScreenDisplay()
            Timber.v("Nexus version code is ${NexusContentProvider.getBuildVersionCode(requireContext())}")
        }
    }

    @SuppressLint
    private fun reinitExportLastSessionStats() {
        with(binding.rowExportLastSessionStats) {
            val lastSession = RemotePlaySettingsPref.savedLastSessionStats
            root.setEnabledWithAlpha(RemotePlaySettingsPref.isPerfOverlayEnabled)
            tvTitle.text = "Export last session streaming stats"
            tvSubtitle.text =
                if (!root.isEnabled) {
                    "Disabled. Please re-enable \'Show performance stats while streaming\'"
                } else {
                    if (lastSession != null) {
                        "${Date(lastSession.startedAt)} (Lasted for ${(lastSession.elapsedTimeMs ?: 0L).milliseconds.inWholeSeconds}s)\n" +
                                "randomId: ${lastSession.randomId}"
                    } else {
                        "No stats yet"
                    }
                }
            ivActionIcon.gone()
            switchLayout.gone()

            root.setOnClickListener {
                if (!root.isEnabled) return@setOnClickListener
                if (lastSession != null) {
                    launchWithLoading {
                        export(requireActivity(), lastSession)
                    }
                } else {
                    toast("Start a streaming session first")
                }
            }
        }
    }

    private fun initExportCurrentSettings() {
        with(binding.rowExportCurrentSettings) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
            tvTitle.text = "Export all settings"
            ivActionIcon.gone()
            switchLayout.gone()
            tvSubtitle.text = "${sharedPref.all.keys.size} entries"
            root.setOnClickListener {
                launchWithLoading {
                    export(
                        requireActivity(),
                        sharedPref
                    )
                }
            }
        }
    }

    private fun initSeparateScreenDisplay() {
        with(binding.rowSeparateScreenDisplay) {
            val isSeparateScreenDisplayEnabled = RemotePlaySettingsPref.isSeparateScreenDisplayEnabled
            tvTitle.text = "Enable separate screen display"
            ivActionIcon.gone()
            tvSubtitle.gone()
            switchLayout.visible()
            switchAction.isChecked = isSeparateScreenDisplayEnabled
            root.setOnClickListener {
                switchAction.toggle()
                RemotePlaySettingsPref.isSeparateScreenDisplayEnabled = !isSeparateScreenDisplayEnabled
            }
            switchAction.setOnCheckedChangeListener { _, isEnabled ->
                RemotePlaySettingsPref.isSeparateScreenDisplayEnabled = isEnabled
            }
        }
    }

    private fun launchWithLoading(task: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                binding.loadingLayoutContainer.visible()
                task()
            } catch (t: Throwable) {
                Timber.w(t)
                toast(t.message)
            } finally {
                binding.loadingLayoutContainer.gone()
            }
        }
    }
}

interface DevOptionsHelper {
    /**
     * BAA-2249
     */
    suspend fun export(activity: Activity, sessionStats: SessionStats) {
        val last4Chars = sessionStats.randomId.takeLast(4)
        val fileName =
            "session_stats_${last4Chars}_${sessionStats.startedAt.toDateTimeString("yyyyMMdd_HHmmss")}.txt"
        shareFile(activity, fileName = fileName, intentTitle = "Session stats $last4Chars") {
            it.write(
                requireNotNull(sessionStats.toJson(true)) { "Cannot serialize SessionStats ${sessionStats.randomId}" }.toByteArray()
            )
        }
    }

    /**
     * BAA-2249
     */
    suspend fun export(activity: Activity, sharedPreferences: SharedPreferences) {
        val fileName =
            "neuron_settings_${now().toDateTimeString("yyyyMMdd_HHmmss")}.txt"

        shareFile(activity, fileName = fileName, intentTitle = "Neuron settings") {
            val jsonObj = JSONObject().apply {
                sharedPreferences.all.entries.forEach { (k, v) ->
                    when (k) {
                        RemotePlaySettingsPref.LAST_SESSION_STATS_JSON -> Unit // ignore key
                        else -> put(k, v)
                    }
                }
            }
            it.write(
                requireNotNull(jsonObj.toString(3)) { "Cannot serialize settings" }.toByteArray()
            )
        }
    }

    private suspend fun shareFile(
        activity: Activity,
        /**
         * Must match [com.limelight.R.xml.filepaths]
         */
        folderName: String = "neuron_dev",
        mimeType: String = "text/plain",
        fileName: String,
        intentTitle: String,
        write: (FileOutputStream) -> Unit
    ) {
        /**
         * Must match [com.limelight.R.xml.filepaths]
         */
        val folder =
            File(activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        val uri = FileProvider.getUriForFile(
            activity,
            activity.getString(R.string.neuron_dev_authority),
            File(folder, fileName).apply {
                withContext(Dispatchers.IO) {
                    FileOutputStream(this@apply).use {
                        write(it)
                    }
                }
            }
        )
        val shareIntent = ShareCompat.IntentBuilder.from(activity)
            .setType(mimeType) // Specify the MIME type for text files
            .setStream(uri)
            .setChooserTitle(intentTitle)
            .createChooserIntent()
        activity.startActivity(shareIntent)
    }


}