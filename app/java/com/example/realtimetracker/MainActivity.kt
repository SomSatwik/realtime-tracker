package com.example.realtimetracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.realtimetracker.LocationHelper
import com.example.realtimetracker.RouteHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locationHelper: LocationHelper
    private lateinit var routeHelper: RouteHelper
    private var currentMarker: Marker? = null
    private lateinit var tvEta: TextView
    private var isTracking = false

    // Register the permission callback
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startTracking()
            } else {
                Toast.makeText(this, "Permission denied. Tracking unavailable.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvEta = findViewById(R.id.tvEta)
        routeHelper = RouteHelper()

        // Initialize Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Setup LocationHelper with a callback for updates
        locationHelper = LocationHelper(this) { location ->
            updateUI(location)
        }

        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startTracking()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startTracking() {
        if (!isTracking) {
            try {
                // Enable the blue dot (my location layer) as a visual confirmation
                // Note: The prompt asks for a "Moving Marker", so we will add a custom marker as well
                // per the requirement: "A marker moves on Google Maps in real time"
                map.isMyLocationEnabled = true 
                
                locationHelper.startLocationUpdates()
                isTracking = true
                Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show()
                
                // Show destination marker
                map.addMarker(MarkerOptions().position(routeHelper.getDestination()).title("Destination"))

            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)

        // 1. Update Marker
        if (currentMarker == null) {
            currentMarker = map.addMarker(MarkerOptions().position(currentLatLng).title("You"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        } else {
            currentMarker?.position = currentLatLng
            // Smoothly animate camera to follow user
            map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }

        // 2. Update Route
        routeHelper.updatePolyline(map, currentLatLng)

        // 3. Update ETA
        val etaText = routeHelper.calculateETA(location)
        tvEta.text = etaText
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationHelper.isInitialized) {
            locationHelper.stopLocationUpdates()
        }
    }
}
