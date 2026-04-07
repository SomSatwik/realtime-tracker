package com.ghosttrack.app.ui

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ghosttrack.app.Constants
import com.ghosttrack.app.R
import com.ghosttrack.app.databinding.FragmentShareBinding
import com.ghosttrack.app.service.LocationSharingService
import com.ghosttrack.app.viewmodel.MainViewModel
import com.ghosttrack.app.viewmodel.SessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareFragment : Fragment() {

    private var _binding: FragmentShareBinding? = null
    private val binding get() = _binding!!
    
    // Use activityViewModels so the session persists if we navigate away and back
    private val viewModel: MainViewModel by activityViewModels()

    private val prefs: SharedPreferences by lazy {
        requireContext().getSharedPreferences(LocationSharingService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == LocationSharingService.KEY_IS_SHARING) {
            val isSharing = sharedPreferences.getBoolean(key, false)
            updateUiForSharingState(isSharing)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            startLocationService()
        } else {
            Toast.makeText(requireContext(), "Permissions required for sharing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnCreateSession.setOnClickListener {
            val phone = binding.etPhone.text.toString()
            if (phone.isNotBlank()) {
                viewModel.createSession(phone)
            }
        }

        binding.btnAction.setOnClickListener {
            val isSharing = prefs.getBoolean(LocationSharingService.KEY_IS_SHARING, false)
            if (isSharing) {
                stopLocationService()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.btnCopy.setOnClickListener {
            val url = binding.tvLink.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("tracking link", url))
            Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val url = binding.tvLink.text.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Track my live location: $url")
            }
            startActivity(Intent.createChooser(intent, "Share via..."))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { state ->
                    updateSessionUi(state)
                }
            }
        }

        // Initial UI sync
        updateUiForSharingState(prefs.getBoolean(LocationSharingService.KEY_IS_SHARING, false))
    }

    override fun onResume() {
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateUiForSharingState(prefs.getBoolean(LocationSharingService.KEY_IS_SHARING, false))
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun updateSessionUi(state: SessionState) {
        when (state) {
            is SessionState.Loading -> {
                binding.btnCreateSession.isEnabled = false
                binding.btnCreateSession.text = "Loading..."
            }
            is SessionState.Success -> {
                onSessionCreated(state.sessionId)
            }
            is SessionState.Error -> {
                binding.btnCreateSession.isEnabled = true
                binding.btnCreateSession.text = "Create Session"
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
            is SessionState.Idle -> {
                binding.cardPreSession.visibility = View.VISIBLE
                binding.cardPostSession.visibility = View.GONE
                binding.btnAction.isEnabled = false
            }
        }
    }

    private fun onSessionCreated(sessionId: String) {
        binding.cardPreSession.visibility = View.GONE
        binding.cardPostSession.visibility = View.VISIBLE
        binding.btnAction.isEnabled = true

        val url = "${Constants.BASE_URL}track/$sessionId"
        binding.tvLink.text = url
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        
        val required = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (required.isEmpty()) {
            startLocationService()
        } else {
            requestPermissionLauncher.launch(required.toTypedArray())
        }
    }

    private fun startLocationService() {
        val sessionId = (viewModel.sessionState.value as? SessionState.Success)?.sessionId
        if (sessionId == null) return

        val intent = Intent(requireContext(), LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_START
            putExtra(LocationSharingService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopLocationService() {
        val intent = Intent(requireContext(), LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_STOP
        }
        requireContext().startService(intent)
    }

    private fun updateUiForSharingState(active: Boolean) {
        binding.pulsingIndicator.isSharing = active
        
        val colorStart = if (active) Color.parseColor("#007AFF") else Color.parseColor("#34C759")
        val colorEnd = if (active) Color.parseColor("#FF3B30") else Color.parseColor("#34C759")

        ValueAnimator.ofObject(ArgbEvaluator(), colorStart, colorEnd).apply {
            duration = 300
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                binding.btnAction.backgroundTintList = ColorStateList.valueOf(color)
            }
            start()
        }

        if (active) {
            binding.btnAction.text = "Stop Sharing"
            binding.tvStatus.text = "Sharing Live"
            binding.tvStatus.setTextColor(Color.parseColor("#34C759"))
        } else {
            binding.btnAction.text = "Start Sharing"
            binding.tvStatus.text = "Ready"
            binding.tvStatus.setTextColor(Color.parseColor("#8E8E93"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
