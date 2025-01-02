package com.razer.neuron.settings.devices

import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.isPaired

import com.razer.neuron.model.AppThemeType

/**
 * Super class to generalize the items to be displayed on the [RnDevicesFragment]
 */
sealed class DeviceItem(val id: String) : HasContent {
    override fun isItemTheSame(that: HasContent) = (id == (that as? DeviceItem)?.id)

    override fun isContentTheSame(that: HasContent) = this == that

    object Header : DeviceItem("computer_item_header")

    object PairedGroupHeader : DeviceItem("paired_group_header")

    class Loading(val hint : String) : DeviceItem("loading")

    class Computer(val computerDetails: ComputerDetails, val hasDuplicate : Boolean) : DeviceItem(computerDetails.uuid) {
        private val computerDetailsHashCode = computerDetails.hashCode()
        private val computerDetailsString = computerDetails.toString()

        val actions : List<DeviceAction>
            get() = when (isPaired()) {
                true -> listOf(DeviceAction.STREAM, DeviceAction.UNPAIR)
                false -> listOf(DeviceAction.PAIR)
                else -> emptyList()
            }

        /**
         * Since [ComputerDetails] is mutable, this needs to compare a immutable hashCode
         * that will capture the state of the [ComputerDetails] when init was called.
         */
        override fun isContentTheSame(that: HasContent) : Boolean {
            if(that !is Computer) return false
            return (computerDetailsHashCode == that.computerDetailsHashCode) &&
                    (hasDuplicate == that.hasDuplicate)
        }

        override fun toString(): String {
            return "${javaClass.simpleName}(name=${computerDetails.name},hasDuplicate=$hasDuplicate,isPaired=${computerDetails.isPaired()},state=${computerDetails.state})"
        }
    }

    class UnpairedGroupHeader(val isLoading: Boolean) : DeviceItem("available_group_header") {
        override fun isContentTheSame(that: HasContent): Boolean {
            return that is UnpairedGroupHeader && that.isLoading == this.isLoading
        }
    }

    open class ActionItem(id : String) : DeviceItem(id)

    open class SwitchItem(id : String, val isChecked : Boolean) : ActionItem(id)

    object AddManually : ActionItem("add_manually")

    class SelectTheme(val selected: AppThemeType) : ActionItem("theme") {
        override fun isContentTheSame(that: HasContent) = this.selected == (that as? SelectTheme)?.selected
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(id=$id)"
    }

}

fun DeviceItem.Computer.isPaired() = computerDetails.isPaired()


/**
 * To be used by items in [DiffItemCallback]
 */
interface HasContent {
    fun isItemTheSame(that: HasContent) : Boolean
    fun isContentTheSame(that : HasContent) : Boolean
}
