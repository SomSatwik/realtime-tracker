package com.ghosttrack.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ghosttrack.app.R
import com.ghosttrack.app.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start spring animation on logo
        binding.icLogo.scaleX = 0.8f
        binding.icLogo.scaleY = 0.8f
        binding.icLogo.alpha = 0f

        val scaleXAnim = SpringAnimation(binding.icLogo, DynamicAnimation.SCALE_X, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }
        val scaleYAnim = SpringAnimation(binding.icLogo, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }

        binding.icLogo.animate().alpha(1f).setDuration(300).withEndAction {
            scaleXAnim.start()
            scaleYAnim.start()
        }.start()

        // Fade in text 200ms after icon
        view.postDelayed({
            binding.tvAppName.animate().alpha(1f).setDuration(400).start()
        }, 200)

        // Navigate
        lifecycleScope.launch {
            delay(1800)
            findNavController().navigate(R.id.action_splash_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
