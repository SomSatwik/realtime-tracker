package com.ghosttrack.app.ui

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import com.ghosttrack.app.Constants
import com.ghosttrack.app.R
import com.ghosttrack.app.databinding.DialogTrackInputBinding
import com.ghosttrack.app.databinding.FragmentTrackBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import kotlin.math.abs

@AndroidEntryPoint
class TrackFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTrackBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null

    private var socket: Socket? = null
    private var sessionId: String? = null

    private var userMarker: Marker? = null
    private var isFollowing = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.btnCenter.setOnClickListener {
            isFollowing = true
            updateFollowingUi()
            val m = userMarker
            if (m != null) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(m.position, 16f))
            }
        }

        binding.fabFollow.setOnClickListener {
            isFollowing = !isFollowing
            updateFollowingUi()
            
            val scaleAnim = SpringAnimation(binding.fabFollow, DynamicAnimation.SCALE_X, 1f).apply {
                spring.stiffness = SpringForce.STIFFNESS_LOW
                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
            val scaleAnimY = SpringAnimation(binding.fabFollow, DynamicAnimation.SCALE_Y, 1f).apply {
                spring.stiffness = SpringForce.STIFFNESS_LOW
                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
            binding.fabFollow.scaleX = 0.8f
            binding.fabFollow.scaleY = 0.8f
            scaleAnim.start()
            scaleAnimY.start()
        }

        promptForSessionId()
    }

    private fun promptForSessionId() {
        val dialogBinding = DialogTrackInputBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Enter Session ID")
            .setView(dialogBinding.root)
            .setPositiveButton("Track") { _, _ ->
                val sid = dialogBinding.etSessionId.text.toString()
                if (sid.isNotBlank()) {
                    sessionId = sid
                    connectSocket()
                } else {
                    Toast.makeText(requireContext(), "Invalid session ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                activity?.onBackPressed()
            }
            .setCancelable(false)
            .show()
    }

    private fun connectSocket() {
        try {
            socket = IO.socket(Constants.SOCKET_URL)
            socket?.connect()
            
            socket?.on(Socket.EVENT_CONNECT) {
                socket?.emit("join-session", sessionId)
            }
            
            socket?.on("receive-location") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val lat = data.getDouble("latitude")
                    val lng = data.getDouble("longitude")
                    handler.post { updateLocationOnMap(LatLng(lat, lng)) }
                }
            }
            
            socket?.on("sharing-stopped") {
                handler.post {
                    binding.liveDot.visibility = View.GONE
                    Toast.makeText(requireContext(), "User stopped sharing", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        
        // Simple dark map styling (Aubergine) could be injected here via setMapStyle
        // if we had the raw json file. For now, default maps.
    }

    private fun updateLocationOnMap(latLng: LatLng) {
        binding.liveDot.visibility = View.VISIBLE
        
        if (userMarker == null) {
            userMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(createCustomAppleMarker())
                    .anchor(0.5f, 0.5f)
            )
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        } else {
            animateMarkerToPosition(userMarker!!, latLng)
            if (isFollowing) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }
    }

    private fun animateMarkerToPosition(marker: Marker, toPosition: LatLng) {
        val startPosition = marker.position
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            addUpdateListener { animator ->
                val v = animator.animatedFraction
                val lat = (toPosition.latitude - startPosition.latitude) * v + startPosition.latitude
                val lng = (toPosition.longitude - startPosition.longitude) * v + startPosition.longitude
                marker.position = LatLng(lat, lng)
            }
            start()
        }
    }

    private fun createCustomAppleMarker(): BitmapDescriptor {
        val size = (24 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A84FF")
            style = Paint.Style.STROKE
            strokeWidth = 3 * resources.displayMetrics.density
        }

        val radius = size / 2f - paintStroke.strokeWidth / 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, paintFill)
        canvas.drawCircle(size / 2f, size / 2f, radius, paintStroke)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun updateFollowingUi() {
        if (isFollowing) {
            binding.fabFollow.setImageResource(android.R.drawable.ic_menu_mylocation) // Should be gps_fixed
            binding.fabFollow.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_primary)
        } else {
            binding.fabFollow.setImageResource(android.R.drawable.ic_menu_directions) // Should be map nav arrow
            binding.fabFollow.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.md_theme_light_onSurfaceVariant)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        socket?.disconnect()
        _binding = null
    }
}
