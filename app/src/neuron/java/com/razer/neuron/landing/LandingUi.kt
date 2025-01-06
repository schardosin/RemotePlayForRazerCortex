package com.razer.neuron.landing

import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.isPaired
import com.razer.neuron.model.ButtonHint

/**
 * Super class to generalize the items to be displayed on the [RnDevicesFragment]
 */
sealed class LandingItem(open val id: String) : HasContent {
    override fun isItemTheSame(that: HasContent) = (id == (that as? LandingItem)?.id)

    override fun isContentTheSame(that: HasContent) = this == that

    object PairedGroupHeader : LandingItem("paired_group_header")

    class UnpairedGroupHeader(val isLoading : Boolean) : LandingItem("unpaired_group_header") {
        override fun isContentTheSame(that: HasContent): Boolean {
            return that is UnpairedGroupHeader && that.isLoading == this.isLoading
        }
    }

    class Loading(val hint : String) : LandingItem("loading")

    open class ComputerList(val computerItemList: List<ComputerItem>, override val id: String) : LandingItem(id)

    class PairedComputerList(computerItemList: List<ComputerItem>) : ComputerList(computerItemList, "paired_computer_list")

    class UnpairedComputerList(computerItemList: List<ComputerItem>): ComputerList(computerItemList, "unpaired_computer_list")

    open class ActionItem(id : String) : LandingItem(id)

    object AddManually : ActionItem("add_manually")

    override fun toString(): String {
        return "${javaClass.simpleName}(id=$id)"
    }

}

class ComputerItem(
    val computerDetails: ComputerDetails,
    val isFocus: Boolean,
    val hasDuplicate: Boolean = false
) : HasContent {
    private val computerDetailsHashCode = computerDetails.hashCode()
    private val computerDetailsString = computerDetails.toString()

    override fun isItemTheSame(that: HasContent): Boolean {
        return computerDetails.uuid == (that as? ComputerItem)?.computerDetails?.uuid &&
               isFocus == (that as? ComputerItem)?.isFocus &&
                hasDuplicate == (that as? ComputerItem)?.hasDuplicate
    }

    /**
     * Since [ComputerDetails] is mutable, this needs to compare a immutable hashCode
     * that will capture the state of the [ComputerDetails] when init was called.
     */
    override fun isContentTheSame(that: HasContent) : Boolean {
        if(that !is ComputerItem) return false
        return (computerDetailsHashCode == that.computerDetailsHashCode) && hasDuplicate == that.hasDuplicate
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(name=${computerDetails.name},isPaired=${computerDetails.isPaired()},state=${computerDetails.state})"
    }
}

fun ComputerItem.isPaired() = computerDetails.isPaired()

sealed class FocusItem(val buttonHints: Set<ButtonHint>? = null) {
    data object None : FocusItem()

    data object Settings : FocusItem(
        setOf(
            ButtonHint.Settings
        )
    )

    class Computer(val computerDetails: ComputerDetails) : FocusItem(
        if (computerDetails.state != ComputerDetails.State.ONLINE) {
            setOf(ButtonHint.Retry)
        } else if (computerDetails.isPaired()) {
            setOf(ButtonHint.Unpair, ButtonHint.StartPlay)
        } else {
            setOf(ButtonHint.Pair)
        }
    )

    data object AddPcManually : FocusItem(
        setOf(ButtonHint.ManuallyPair)
    )
}

/**
 * To be used by items in [DiffItemCallback]
 */
interface HasContent {
    fun isItemTheSame(that: HasContent) : Boolean
    fun isContentTheSame(that : HasContent) : Boolean
}
