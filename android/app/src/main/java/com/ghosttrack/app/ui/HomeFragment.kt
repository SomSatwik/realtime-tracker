package com.ghosttrack.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ghosttrack.app.R
import com.ghosttrack.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPressableCard(binding.cardShare) {
            findNavController().navigate(R.id.action_home_to_share)
        }

        setupPressableCard(binding.cardTrack) {
            findNavController().navigate(R.id.action_home_to_track)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPressableCard(card: View, onClick: () -> Unit) {
        val scaleXAnim = SpringAnimation(card, DynamicAnimation.SCALE_X, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        }
        val scaleYAnim = SpringAnimation(card, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        }

        card.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.scaleX = 0.98f
                    v.scaleY = 0.98f
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    scaleXAnim.start()
                    scaleYAnim.start()
                    v.performClick()
                    onClick()
                    true
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    scaleXAnim.start()
                    scaleYAnim.start()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
