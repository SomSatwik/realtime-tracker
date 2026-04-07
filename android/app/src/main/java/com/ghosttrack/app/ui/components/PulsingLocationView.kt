package com.ghosttrack.app.ui.components

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.ghosttrack.app.R

class PulsingLocationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val inactiveColor = Color.parseColor("#F2F2F7")
    private val activeColor = Color.parseColor("#34C759")
    private val inactiveIconTint = Color.parseColor("#8E8E93")
    private val activeIconTint = Color.parseColor("#FFFFFF")

    private var currentColor = inactiveColor
    private var currentIconTint = inactiveIconTint

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = currentColor
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = activeColor
    }

    private val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_location_pin)?.mutate()

    var isSharing: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                animateColorChange(value)
                if (value) {
                    startPulse()
                } else {
                    stopPulse()
                }
            }
        }

    private var pulseRadius = 60f
    private var pulseAlpha = 255
    private var pulseAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    private fun animateColorChange(active: Boolean) {
        val startColor = if (active) inactiveColor else activeColor
        val endColor = if (active) activeColor else inactiveColor
        val startTint = if (active) inactiveIconTint else activeIconTint
        val endTint = if (active) activeIconTint else inactiveIconTint

        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                currentColor = ArgbEvaluator().evaluate(fraction, startColor, endColor) as Int
                currentIconTint = ArgbEvaluator().evaluate(fraction, startTint, endTint) as Int
                circlePaint.color = currentColor
                icon?.setTint(currentIconTint)
                invalidate()
            }
            start()
        }
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        val startRadius = dpToPx(60f)
        val endRadius = dpToPx(90f)

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                pulseRadius = startRadius + (endRadius - startRadius) * fraction
                pulseAlpha = (255 * (1f - fraction)).toInt()
                pulsePaint.alpha = pulseAlpha
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAlpha = 0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        
        // Draw expanding pulse if sharing
        if (isSharing && pulseAlpha > 0) {
            canvas.drawCircle(cx, cy, pulseRadius, pulsePaint)
        }

        // Draw main circle
        val baseRadius = dpToPx(60f)
        canvas.drawCircle(cx, cy, baseRadius, circlePaint)

        // Draw Icon
        icon?.let {
            val iconSize = dpToPx(32f).toInt()
            val left = (cx - iconSize / 2).toInt()
            val top = (cy - iconSize / 2).toInt()
            it.setBounds(left, top, left + iconSize, top + iconSize)
            it.setTint(currentIconTint)
            it.draw(canvas)
        }
    }
}
