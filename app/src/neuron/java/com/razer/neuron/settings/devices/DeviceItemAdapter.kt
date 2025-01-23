package com.razer.neuron.settings.devices

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.limelight.R
import com.limelight.databinding.RnDevicePopupMenuBinding
import com.limelight.databinding.RnDevicePopupMenuItemBinding
import com.limelight.databinding.RnItemNeuronSettingsActionBinding
import com.limelight.databinding.RnItemNeuronSettingsComputerBinding
import com.limelight.databinding.RnItemNeuronSettingsGroupHeaderBinding
import com.limelight.databinding.RnItemNeuronSettingsHeaderBinding
import com.limelight.databinding.RnItemNeuronSettingsLoadingBinding
import com.limelight.nvstream.http.ComputerDetails
import com.razer.neuron.RnApp
import com.razer.neuron.extensions.dimenResToPx
import com.razer.neuron.extensions.dpToPx
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.visible
import com.razer.neuron.extensions.visibleIf
import com.razer.neuron.model.getHostVariant
import kotlin.math.roundToInt


private const val TYPE_COMPUTER_HEADER = 0
private const val TYPE_COMPUTER_GROUP_HEADER = 1
private const val TYPE_COMPUTER = 2
private const val TYPE_ACTION = 3
private const val TYPE_LOADING = 5


class DeviceItemAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<DeviceItem, RecyclerView.ViewHolder>(
    DiffItemCallback()
) {

    interface Listener {
        fun onComputerActionClicked(computer: ComputerDetails, action: DeviceAction)

        fun onActionClicked(actionItem : DeviceItem.ActionItem)

        fun onSwitchChecked(switchItem : DeviceItem.SwitchItem, isChecked : Boolean)

    }

    var listener: Listener? = null

    private var popupWindow: PopupWindow? = null

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is DeviceItem.Computer -> TYPE_COMPUTER
            is DeviceItem.Header -> TYPE_COMPUTER_HEADER
            is DeviceItem.PairedGroupHeader,
            is DeviceItem.UnpairedGroupHeader,
            -> TYPE_COMPUTER_GROUP_HEADER

            is DeviceItem.Loading -> TYPE_LOADING
            is DeviceItem.ActionItem -> TYPE_ACTION
            else -> error("Unsupported type ${item.javaClass.simpleName} at $position")
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    popupWindow.dismissSafely()
                    popupWindow = null
                }
                else -> Unit
            }
        })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_COMPUTER_HEADER -> HeaderViewHolder(
                RnItemNeuronSettingsHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            TYPE_COMPUTER_GROUP_HEADER -> GroupHeaderViewHolder(
                RnItemNeuronSettingsGroupHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            TYPE_LOADING -> LoadingViewHolder(
                RnItemNeuronSettingsLoadingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            TYPE_COMPUTER -> ComputerViewHolder(
                RnItemNeuronSettingsComputerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            TYPE_ACTION -> ComputerActionViewHolder(
                RnItemNeuronSettingsActionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> error("$viewType not supported")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (holder.itemViewType) {
            TYPE_COMPUTER_HEADER -> (holder as HeaderViewHolder).bind()
            TYPE_COMPUTER_GROUP_HEADER -> (holder as GroupHeaderViewHolder).bind(getItem(position), position)
            TYPE_COMPUTER -> (holder as ComputerViewHolder).bind(getItem(position))
            TYPE_ACTION -> (holder as ComputerActionViewHolder).bind(getItem(position))
            TYPE_LOADING -> (holder as LoadingViewHolder).bind()

        }
    }

    private inner class HeaderViewHolder(val binding: RnItemNeuronSettingsHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.tvHeaderTitle.text = itemView.resources.getText(R.string.rn_all_computers)
            binding.tvHeaderSubtitle.text = itemView.resources.getText(R.string.rn_all_computers_subtitle)
        }
    }

    private fun DeviceItem.Computer.hasDuplicateComputerName() = runCatching {
        toIterator().asSequence().any {
            it is DeviceItem.Computer
                    && it != this
                    && it.computerDetails.name == computerDetails.name
        }
    }.getOrNull() ?: false



    private inner class GroupHeaderViewHolder(val binding: RnItemNeuronSettingsGroupHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var isShowProgressBar : Boolean
            get() = binding.layoutHeaderLoadingContainer.isVisible
            set(value) {
                binding.layoutHeaderLoadingContainer.visibleIf { value }
            }


        fun bind(data: DeviceItem, position: Int) {

            when (data) {
                is DeviceItem.PairedGroupHeader -> data.bind()
                is DeviceItem.UnpairedGroupHeader -> data.bind()
                else -> error("${data.javaClass.simpleName} bind impl found")
            }
        }

        private fun DeviceItem.PairedGroupHeader.bind() {
            isShowProgressBar = false
            binding.tvHeaderSubtitle.text = itemView.resources.getText(R.string.rn_paired_computers)
        }

        private fun DeviceItem.UnpairedGroupHeader.bind() {
            isShowProgressBar = isLoading
            binding.tvHeaderSubtitle.text = itemView.resources.getText(R.string.rn_unpaired_computers)
        }
    }


    private inner class LoadingViewHolder(val binding: RnItemNeuronSettingsLoadingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.tvLoading.text = itemView.resources.getString(R.string.rn_pairing_hint)
        }
    }

    /**
     * If we have duplicate names for paired, we need some way to identify them.
     */
    private fun ComputerDetails.last4UuidChars() = with(uuid) {
        if (length >= 4) substring(length - 4) else this
    }


    private inner class ComputerViewHolder(val binding: RnItemNeuronSettingsComputerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(data: DeviceItem) {
            val computerDetails = (data as? DeviceItem.Computer)?.computerDetails ?: return
            val nameExtra = computerDetails.last4UuidChars()?.run { "($this)" } ?: ""
            binding.tvComputerTitle.text = "${computerDetails.name} ${if(data.hasDuplicateComputerName()) nameExtra else ""}".trimEnd()
            val isOnline = computerDetails.state == ComputerDetails.State.ONLINE
            val isOffline = computerDetails.state == ComputerDetails.State.OFFLINE
            val isPaired = data.isPaired()

            binding.tvComputerSubtitle.visible()
            binding.ivComputerIcon.alpha = 1f
            if(isOnline) {
                binding.tvComputerSubtitle.text = itemView.resources.getString(R.string.rn_discovery_state_pc_online)
                if (computerDetails.offlineCount > 0) {
                    // some visual indication that it is trying to poll the computer
                    // but hide the subtitle
                    binding.ivComputerIcon.alpha = 0.66f
                    binding.tvComputerSubtitle.gone()
                }
            } else if(isOffline) {
                // since it is offline we can say it is offline
                // icon will also indicate this
                binding.tvComputerSubtitle.gone()
            } else {
                // hide the subtitle since it is not paired and we don't know
                // if it is online or not
                binding.tvComputerSubtitle.gone()
            }


            binding.ivComputerIcon.setImageResource(
                if (isPaired) {
                    // for paired, we only show offline if we are sure
                    if (isOnline) R.drawable.ic_neuron_computer_online_paired else R.drawable.ic_neuron_computer_offline_paired
                } else {
                    // for unpaired, we only show online if we are sure
                    if (isOnline) R.drawable.ic_neuron_computer_online else R.drawable.ic_neuron_computer_offline
                }
            )


            itemView.setOnClickListener { binding.ivActionMenu.performClick() }
            binding.ivActionMenu.setOnClickListener { v ->
                val menuItems = data.actions.map { it.toMenuItem() }
                val popupWindowHeight = dimenResToPx(R.dimen.settings_device_popup_item_height) * data.actions.size
                popupWindow?.dismissSafely()
                popupWindow = PopupMenuHelper.create(v,
                    yOffset = -1*(((popupWindowHeight / 2).roundToInt()) + v.height / 2),
                    menuItems = menuItems) {
                    listener?.onComputerActionClicked(computerDetails, it.item)
                    true
                }
            }
        }
    }

    fun DeviceAction.toMenuItem() = when(this) {
        DeviceAction.PAIR -> PopupMenuHelper.MenuItem(this, RnApp.appContext.resources.getText(R.string.rn_device_page_pc_pair))
        DeviceAction.UNPAIR -> PopupMenuHelper.MenuItem(this, RnApp.appContext.resources.getText(R.string.rn_device_page_pc_unpair))
        DeviceAction.STREAM -> PopupMenuHelper.MenuItem(this, RnApp.appContext.resources.getText(R.string.rn_device_page_pc_start_play))
        DeviceAction.WOL -> PopupMenuHelper.MenuItem(this, RnApp.appContext.resources.getText(R.string.rn_retry))
    }


    private inner class ComputerActionViewHolder(val binding: RnItemNeuronSettingsActionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val switchLayout by lazy { binding.switchLayout }
        private val switchAction by lazy { binding.switchAction }
        private val actionIcon by lazy { binding.ivActionIcon }
        fun bind(data: DeviceItem) {
            if(data !is DeviceItem.ActionItem) return
            itemView.setOnClickListener {
                when(data) {
                    is DeviceItem.SwitchItem -> {
                        switchAction.isChecked = !data.isChecked
                    }
                    else -> {
                        listener?.onActionClicked(data)
                    }
                }
            }

            if(data is DeviceItem.SwitchItem) {
                switchAction.setOnCheckedChangeListener(null)
                switchAction.isChecked = data.isChecked
                switchAction.setOnCheckedChangeListener { compoundButton, b ->
                    listener?.onSwitchChecked(data, b)
                }
                switchLayout.visible()
            } else {
                switchLayout.gone()
            }


            when (data) {
                is DeviceItem.AddManually -> data.bind()
                is DeviceItem.SelectTheme -> data.bind()
                else -> error("${data.javaClass.simpleName} bind impl found")
            }
        }

        private fun DeviceItem.SelectTheme.bind() {
            actionIcon.gone()
            binding.tvTitle.text = itemView.resources.getText(R.string.rn_theme_title)
            binding.tvSubtitle.text = itemView.resources.getText(selected.title)
        }

        private fun DeviceItem.AddManually.bind() {
            actionIcon.visible()
            actionIcon.setImageResource(R.drawable.ic_settings_add)
            binding.tvTitle.text = itemView.resources.getText(R.string.rn_add_computer_manually)
            binding.tvSubtitle.text = itemView.resources.getText(R.string.rn_add_computer_manually_subtitle)
        }
    }

    class ItemDecoration : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            val position = parent.getChildAdapterPosition(view)
            if (position == -1) return
            val first = position == 0
            val last = position == (parent.adapter?.itemCount ?: 0) - 1
            val adapter = parent.adapter ?: return
            val itemViewType = adapter.getItemViewType(position)
            val previousItemViewType = (position - 1).takeIf { it >= 0 }?.let { adapter.getItemViewType(it) }
            when (itemViewType) {
                TYPE_COMPUTER_GROUP_HEADER -> {
                    outRect.top = dimenResToPx(R.dimen.margin_5x).roundToInt()
                    outRect.bottom = dimenResToPx(R.dimen.margin_2x).roundToInt()
                }
                else -> Unit
            }
            if (first) {
                outRect.top += dimenResToPx(R.dimen.settings_title_margin_top).roundToInt()
            }
            if (last) {
                outRect.bottom += dimenResToPx(R.dimen.margin_4x).roundToInt()
            }
        }
    }

    private class DiffItemCallback : DiffUtil.ItemCallback<DeviceItem>() {
        override fun areItemsTheSame(oldItem: DeviceItem, newItem: DeviceItem) = oldItem.isItemTheSame(newItem)
        override fun areContentsTheSame(oldItem: DeviceItem, newItem: DeviceItem) = oldItem.isContentTheSame(newItem)
    }

    private fun toIterator() = object : Iterator<DeviceItem> {
        private var i = 0
        override fun hasNext() = i < itemCount
        override fun next(): DeviceItem {
            check(i + 1 < itemCount)
            return getItem(++i)
        }
    }
}


object PopupMenuHelper {

    class MenuItem<T>(val item : T, val text : CharSequence)

    fun <T> create(
        anchorView: View,
        xOffset: Int = 0,
        yOffset: Int = 0,
        menuItems: List<MenuItem<T>>,
        onMenuItemClicked : (MenuItem<T>) -> Boolean
    ): PopupWindow {
        val context = anchorView.context
        val inflater = LayoutInflater.from(anchorView.context)
        val binding = RnDevicePopupMenuBinding.inflate(inflater, null, false)

        val p = Rect().apply { anchorView.getGlobalVisibleRect(this) }.run { Point(left, bottom) }
        p.x += (-1 * dpToPx(195f)).roundToInt() + xOffset
        p.y += yOffset

        return PopupWindow(context).apply {
            contentView = binding.root
            elevation = dimenResToPx(R.dimen.margin_4x)
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            isFocusable = true
            setBackgroundDrawable(null)
            animationStyle = R.style.RnPopupWindowAnimation
            binding.llMenuItems.removeAllViews()
            menuItems.forEachIndexed {
                i, menuItem ->
                binding.llMenuItems.addView(RnDevicePopupMenuItemBinding.inflate(inflater, null, false).run {
                    val textView = root
                    textView.text = menuItem.text
                    textView.setOnClickListener {
                        if(onMenuItemClicked(menuItem)) {
                            dismissSafely()
                        }
                    }
                    if(i == 0) {
                       textView.isFocusedByDefault = true
                    }
                    textView
                })
            }
            showAtLocation(binding.root, Gravity.TOP or Gravity.START, p.x, p.y)
        }
    }
}



fun PopupWindow?.dismissSafely() = runCatching {
    this?.dismiss()
}



