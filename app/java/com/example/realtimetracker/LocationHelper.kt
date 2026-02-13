package com.example.realtimetracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

/**
 * Helper class to handle location updates using FusedLocationProviderClient.
 * Respects the rule: DO NOT run location updates silently/without permission (caller must ensure permission).
 */
class LocationHelper(private val context: Context, private val onLocationUpdate: (Location) -> Unit) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    /**
     * Starts location updates.
     * @throws SecurityException if permission is not granted. Caller must handle this.
     */
    @SuppressLint("MissingPermission") // Caller handles permission check
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    onLocationUpdate(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    /**
     * Stops location updates to prevent background tracking.
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }
}
