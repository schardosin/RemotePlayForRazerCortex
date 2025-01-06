package com.razer.neuron.oobe

import android.content.Context
import android.view.View
import com.limelight.databinding.RnLayoutOobeTosBinding

interface RnLayoutOobeTosBindingHelper {

    val context: Context

    val layoutTosBinding: RnLayoutOobeTosBinding

    fun showLayout(view: View)

    fun RnOobeModel.State.Tos.handle() {
        val binding = layoutTosBinding
        showLayout(binding.root)
        binding.btnTos.setOnClickListener {
            onTosClicked()
        }
        binding.btnPp.setOnClickListener {
            onPpClicked()
        }
    }

    fun onTosClicked()

    fun onPpClicked()
}