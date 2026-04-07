package com.example.realtimetracker

import android.graphics.Color
import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.concurrent.TimeUnit

/**
 * Helper class to manage Route Polyline and ETA calculation.
 */
class RouteHelper {

    private var polyline: Polyline? = null
    // Hardcoded destination for demo purposes (e.g., a nearby landmark)
    // You can change this to a coordinate near your demo location
    private val destination = LatLng(37.4220, -122.0841) // Googleplex
    private val fullRoutePoints = mutableListOf<LatLng>()

    /**
     * Updates the polyline on the map.
     * Draws a line from current location to destination.
     * In a real app, this would use Directions API points. For demo, strictly straight line or cumulative path.
     * The prompt asks for: "Draw a polyline between: current location, destination".
     * And "Polyline must update when user moves".
     */
    fun updatePolyline(map: GoogleMap, currentLocation: LatLng) {
        if (polyline == null) {
            val polylineOptions = PolylineOptions()
                .add(currentLocation)
                .add(destination)
                .color(Color.BLUE)
                .width(10f)
            polyline = map.addPolyline(polylineOptions)
        } else {
            // Update the points: Start at current, End at destination
            val points = listOf(currentLocation, destination)
            polyline?.points = points
        }
    }

    /**
     * Calculates ETA based on straight-line distance and average walking speed.
     * Speed assumption: 1.4 m/s (average walking speed).
     */
    fun calculateETA(currentLocation: Location): String {
        val destLocation = Location("destination").apply {
            latitude = destination.latitude
            longitude = destination.longitude
        }

        val distanceMeters = currentLocation.distanceTo(destLocation)
        val speedMetersPerSecond = 1.4 // Walking speed
        val timeSeconds = distanceMeters / speedMetersPerSecond

        val minutes = TimeUnit.SECONDS.toMinutes(timeSeconds.toLong())
        return if (minutes < 1) {
            "ETA: < 1 min"
        } else {
            "ETA: $minutes min"
        }
    }

    fun getDestination(): LatLng {
        return destination
    }
}
