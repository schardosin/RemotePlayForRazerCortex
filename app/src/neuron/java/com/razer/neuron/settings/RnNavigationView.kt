package com.razer.neuron.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView
import com.razer.neuron.extensions.getViewFocusDirection
import com.razer.neuron.utils.flattenDescendants
import com.razer.neuron.utils.isAncestorOf
import com.razer.neuron.utils.isKeyCodeEnter
import com.razer.neuron.utils.randomInt

/**
 * BAA-2220
 * This class extends [NavigationView] because by default Android will choose the next focus view
 * based on the closest focusable view on the screen at the focus direction.
 *
 * However, if [NavigationBehavior.NavigateOnFocus] is used then choosing the closest focusable
 * view can (and often) forces a change in "checked" item also (since we change "checked" on focus) which will
 * lead to accidental navigation.
 *
 * This class will attempt to override the [requestChildFocus] function and call [NavigationMenuItemView.requestFocus]
 * on the the item where [NavigationMenuItemView.isChecked] is true (only under specific conditions)
 */
class RnNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NavigationView(context, attrs, defStyleAttr) {

    /**
     * If true, then by default the currently checked item where
     * [NavigationMenuItemView.isChecked] will be focused
     */
    var isRefocusCheckedItem = false

    /**
     * Get all [NavigationMenuItemView] that matches [predicate] (look through sub-menu also)
     */
    @SuppressLint("RestrictedApi")
    fun navigationMenuItemViews(predicate: (NavigationMenuItemView) -> Boolean = { it.isFocusable }) =
        flattenDescendants()
            .filterIsInstance<NavigationMenuItemView>()
            .filter(predicate)

    private fun logDebug(tag: String, line: String) = Unit //Timber.v("RnNavigationView: $tag $line")

    override fun requestChildFocus(child: View?, focused: View?) {
        val checkedItem =
            navigationMenuItemViews { it.isCheckable() && it.isFocusable && it.isChecked() }.firstOrNull()
        if(isRefocusCheckedItem) requestChildFocusByRefocusCheckedItem(child, focused, checkedItem) else super.requestChildFocus(child, focused)
    }

    /**
     * When sub-menu is used, [NavigationView] will block parent view from getting focus (UP direction only)
     */
    private var blockedFocusableForeignView : View? = null
    private var blockedFocusableForeignViewCount = 0

    /**
     *  If next focusable view is not inside this [NavigationView], then we should force [NavigationView]
     *  to give up focus by calling [View.requestFocus].
     *
     *  This is a bug with [NavigationView] when [android.view.Menu.addSubMenu] is used BAA-2400
     *
     *  So if it is happens more than 2 times, we will force that foreign view to be focused
     */
    private fun unblockFocusabilityOnForeignView(event: KeyEvent) {
        if(event.action != KeyEvent.ACTION_DOWN) return
        val focusDirection = event.getViewFocusDirection() ?: return
        if(focusDirection != View.FOCUS_UP) return
        val v = focusSearch(focusDirection)?.takeIf { !this@RnNavigationView.isAncestorOf(it) }
        if(v != null) {
            if(v == blockedFocusableForeignView) {
                blockedFocusableForeignViewCount++
            } else {
                blockedFocusableForeignView = v
                blockedFocusableForeignViewCount = 0
            }

            if(blockedFocusableForeignViewCount > 1) {
                v.requestFocus()
                blockedFocusableForeignView = null
                blockedFocusableForeignViewCount = 0
            }
        } else {
            blockedFocusableForeignView = null
            blockedFocusableForeignViewCount = 0
        }
    }


    /**
     * There is a bug where focusing a currently "checked" item will focus the first
     * [NavigationMenuItemView], so the best way to fix this without complicating the
     * change for BAA-2220 changes, is to just prevent enter key from being processed.
     */
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        unblockFocusabilityOnForeignView(event)
        val isFocusedAlreadyChecked = navigationMenuItemViews { it.isFocused }.firstOrNull()?.isChecked() == true
        return if(isFocusedAlreadyChecked && event?.isKeyCodeEnter() == true) true else super.dispatchKeyEvent(event)
    }

    @SuppressLint("RestrictedApi")
    private fun requestChildFocusByRefocusCheckedItem(child: View?, focused: View?, checkedItem : NavigationMenuItemView?) {
        val tag = "requestChildFocusByRefocusCheckedItem(${randomInt(0, 100)})"
        logDebug(tag, "${"=".repeat(20)}")
        val hadFocus = hasFocus()
        logDebug(tag, "hadFocus=${hadFocus}")
        logDebug(tag, "child=${child}(${child?.hasFocus()})")
        logDebug(tag, "focused=${focused}(${focused?.hasFocus()})")

        var wasCheckItemFocused = false
        super.requestChildFocus(child, focused)
        if (!hadFocus
            && child is NavigationMenuView
            && focused is NavigationMenuItemView
            && checkedItem != null
            && checkedItem != focused
        ) {
            handler.post {
                if (checkedItem.requestFocus()) {
                    logDebug(
                        tag,
                        "currentChecked=${checkedItem}(${checkedItem.hasFocus()})"
                    )
                    wasCheckItemFocused = true
                }
            }
        }
        logDebug(tag, "wasCheckItemFocused=${wasCheckItemFocused}")
    }
}

@SuppressLint("RestrictedApi")
fun NavigationMenuItemView.isCheckable() = itemData.isCheckable

@SuppressLint("RestrictedApi")
fun NavigationMenuItemView.isChecked() = itemData.isChecked

