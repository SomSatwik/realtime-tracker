package com.ghosttrack.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ghosttrack.app.Constants
import com.ghosttrack.app.MainActivity
import com.ghosttrack.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class LocationSharingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var socket: Socket? = null
    private var sessionId: String? = null
    private var isSharing = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LocationSharingChannel"
        const val PREFS_NAME = "ghosttrack_prefs"
        const val KEY_IS_SHARING = "is_sharing"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val newSessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (!isSharing || newSessionId != sessionId) {
                    sessionId = newSessionId
                    startSharing()
                }

                val notification = createNotification()
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }

                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
                } catch (e: Exception) {
                    Log.e("LocationService", "Failed to start foreground service", e)
                }
            }
            ACTION_STOP -> {
                stopSharing()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startSharing() {
        if (sessionId == null) {
            Log.e("LocationService", "Cannot start sharing: sessionId is null")
            return
        }

        cleanupExistingResources()
        updateSharingState(true)

        try {
            socket = IO.socket(Constants.SOCKET_URL)
            socket?.connect()
            socket?.emit("join-session", sessionId)
        } catch (e: Exception) {
            Log.e("LocationService", "Socket initialization error", e)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val data = JSONObject().apply {
                        put("sessionId", sessionId)
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                    }
                    socket?.emit("send-location", data)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission denied", e)
            stopSharing()
            stopSelf()
        }
    }

    private fun stopSharing() {
        updateSharingState(false)
        cleanupExistingResources()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun updateSharingState(sharing: Boolean) {
        isSharing = sharing
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_SHARING, sharing).apply()
    }

    private fun cleanupExistingResources() {
        socket?.apply {
            emit("stop-sharing", sessionId)
            disconnect()
        }
        socket = null

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, LocationSharingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GhostTrack Active")
            .setContentText("Sharing your location live")
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_location_pin, "Stop Sharing", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active location sharing status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopSharing()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
