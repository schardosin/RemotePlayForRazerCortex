package com.razer.neuron.common

import android.R
import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder


/**
 * @param onSelected return true if you want to dismiss dialog
 */
fun <T : Enum<*>> Activity.showSingleChoiceItemsDialog(
    @StringRes titleId: Int,
    @StringRes messageId: Int?,
    onSelected: (option: T) -> Boolean,
    nameResList: List<Int>,
    options: List<T>,
    defaultOption: T,
    isCancelable : Boolean,
    positiveButton : Pair<String, (T) -> Unit>? = null,
    negativeButton : Pair<String, () -> Unit>? = null,
) : AlertDialog {
    val optionsNames = nameResList.map { getString(it) }
    val selectedIndex = options.map { it.name }.indexOf(defaultOption.name)
    var selectedOption = defaultOption
    return MaterialAlertDialogBuilder(this, materialAlertDialogTheme())
        .setTitle(getString(titleId))
        .setSingleChoiceItems(optionsNames.toTypedArray(), selectedIndex) { dialog, which ->
            selectedOption = options[which]
            if(onSelected.invoke(selectedOption)) {
                dialog.dismiss()
            }
        }
        .apply {
            if (messageId != null) {
                setMessage(messageId)
            }
            if(positiveButton != null) {
                setPositiveButton(positiveButton.first) { dialog, _ ->
                    positiveButton.second.invoke(selectedOption)
                    dialog.dismiss()
                }
            }
            if(negativeButton != null) {
                setNegativeButton(negativeButton.first) { dialog, _ ->
                    negativeButton.second.invoke()
                    dialog.dismiss()
                }
            }
        }
        .setCancelable(isCancelable)
        .show()
}
