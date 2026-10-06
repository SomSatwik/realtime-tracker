package com.ghosttrack.app.data.remote

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.net.URI

data class LocationUpdate(
    val id: String,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double
)

class SocketManager(private val baseUrl: String) {

    private var socket: Socket? = null
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    fun connect() {
        if (socket?.connected() == true) return
        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                transports = arrayOf("websocket", "polling")
            }
            socket = IO.socket(URI.create(baseUrl), options)
            socket?.on(Socket.EVENT_CONNECT) { _connectionState.value = true }
            socket?.on(Socket.EVENT_DISCONNECT) { _connectionState.value = false }
            socket?.on(Socket.EVENT_CONNECT_ERROR) { _connectionState.value = false }
            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun joinSession(sessionId: String) {
        socket?.emit("join-session", sessionId)
    }

    fun sendLocation(sessionId: String, latitude: Double, longitude: Double) {
        val data = JSONObject().apply {
            put("sessionId", sessionId)
            put("latitude", latitude)
            put("longitude", longitude)
        }
        socket?.emit("send-location", data)
    }

    fun stopSharing(sessionId: String) {
        socket?.emit("stop-sharing", sessionId)
    }

    fun locationUpdates(): Flow<LocationUpdate> = callbackFlow {
        val listener = io.socket.emitter.Emitter.Listener { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                val json = args[0] as JSONObject
                val update = LocationUpdate(
                    id = json.optString("id", ""),
                    sessionId = json.optString("sessionId", ""),
                    latitude = json.optDouble("latitude", 0.0),
                    longitude = json.optDouble("longitude", 0.0)
                )
                trySend(update)
            }
        }
        socket?.on("receive-location", listener)
        awaitClose { socket?.off("receive-location", listener) }
    }

    fun onSharingStopped(): Flow<Unit> = callbackFlow {
        val listener = io.socket.emitter.Emitter.Listener { trySend(Unit) }
        socket?.on("sharing-stopped", listener)
        awaitClose { socket?.off("sharing-stopped", listener) }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = false
    }
}
