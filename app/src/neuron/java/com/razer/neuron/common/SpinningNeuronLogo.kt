package com.razer.neuron.common

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.animation.ValueAnimator.*
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import com.limelight.R
import com.limelight.databinding.RnLayoutSpinningRazerLogoBinding
import kotlin.math.max

/**
 * NEUR-78
 */
class SpinningNeuronLogo @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var binding: RnLayoutSpinningRazerLogoBinding
    private val pauseDurationMs : Int
    private val rotateDurationMs : Int
    private val sections : Int
    val ivLogo by lazy { binding.ivNeuronLogo }

    init {
        binding = RnLayoutSpinningRazerLogoBinding.inflate(LayoutInflater.from(context), this)
        with(context.obtainStyledAttributes(attrs, R.styleable.SpinningNeuronLogo)) {
            pauseDurationMs =  max(0,getInteger(R.styleable.SpinningNeuronLogo_pause_duration_ms, 500))
            rotateDurationMs =  max(0,getInteger(R.styleable.SpinningNeuronLogo_rotate_duration_ms, 500))
            sections = max(2, getInteger(R.styleable.SpinningNeuronLogo_sections, 3))
            recycle()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        playOrResumeAnimation()
    }


    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            playOrResumeAnimation()
        } else {
            pauseAnimation()
        }
    }

    private var animator: Animator? = null

    fun playOrResumeAnimation() {
        isPlayAnimation = true
        animator?.let {
            existingAnimator ->
            if(existingAnimator.isRunning) {
                return
            } else if(existingAnimator.isPaused) {
                existingAnimator.resume()
                return
            } else if(existingAnimator.isStarted) {
                existingAnimator.cancel()
            }
        }

        val v : View = ivLogo
        fun createRotation(startDelayMs : Long, startDeg : Float, endDeg: Float) : ValueAnimator {
            return ofFloat(startDeg, endDeg).apply {
                duration = rotateDurationMs.toLong()
                startDelay = startDelayMs
                addUpdateListener {
                        animation ->
                    (animation.animatedValue as? Float)?.let {
                            deg ->
                        v.rotation = deg
                    }
                }
            }
        }

        val animatorSet = AnimatorSet()
        val list = mutableListOf<Animator>()
        val incDeg = 360f/sections
        var startDeg = 0f
        for(i in 0 until sections) {
            val endDeg = startDeg + incDeg
            list += createRotation(startDeg = startDeg, endDeg = endDeg, startDelayMs = pauseDurationMs.toLong())
            startDeg = endDeg
        }
        animatorSet.playSequentially(*list.toTypedArray())
        animatorSet.doOnEnd {
            if(isPlayAnimation) {
                it.start()
            }
        }
        animator = animatorSet.apply { start() }
    }

    fun pauseAnimation() {
        isPlayAnimation = false
        animator?.pause()
    }

    fun stopAnimation() {
        isPlayAnimation = false
        animator?.cancel()
    }

    private var isPlayAnimation = false
}