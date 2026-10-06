package com.ghosttrack.app.ui.student

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghosttrack.app.data.remote.LocationUpdate
import com.ghosttrack.app.data.remote.SocketManager
import com.ghosttrack.app.ui.components.GhostTrackButton
import com.ghosttrack.app.ui.components.LiveBadge
import com.ghosttrack.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

data class LiveTrackingState(
    val isLive: Boolean = false,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastUpdated: String = "--",
    val etaMinutes: Int? = null,
    val distanceKm: Double? = null,
    val trail: List<GeoPoint> = emptyList(),
    val statusText: String = "Waiting for bus location..."
)

@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTrackingState())
    val uiState: StateFlow<LiveTrackingState> = _uiState.asStateFlow()

    private var targetSessionId: String = ""

    fun startTracking(sessionId: String) {
        targetSessionId = sessionId
        socketManager.connect()
        socketManager.joinSession(sessionId)

        viewModelScope.launch {
            socketManager.locationUpdates().collect { update ->
                if (update.sessionId == targetSessionId || targetSessionId.isEmpty()) {
                    handleLocationUpdate(update)
                }
            }
        }

        viewModelScope.launch {
            socketManager.onSharingStopped().collect {
                _uiState.value = _uiState.value.copy(
                    isLive = false,
                    statusText = "Driver stopped broadcasting"
                )
            }
        }
    }

    private fun handleLocationUpdate(update: LocationUpdate) {
        val newPoint = GeoPoint(update.latitude, update.longitude)
        val currentList = _uiState.value.trail.toMutableList()
        currentList.add(newPoint)

        val timeString = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

        // Calculate simple straight-line ETA / distance for campus demo (avg campus speed 20 km/h)
        val campusDestLat = 19.0330 // GIET University area default
        val campusDestLon = 83.8320
        val distKm = calculateDistanceKm(update.latitude, update.longitude, campusDestLat, campusDestLon)
        val etaMin = max(1, ((distKm / 20.0) * 60).toInt())

        _uiState.value = _uiState.value.copy(
            isLive = true,
            lastLatitude = update.latitude,
            lastLongitude = update.longitude,
            lastUpdated = timeString,
            distanceKm = round(distKm * 10) / 10,
            etaMinutes = etaMin,
            trail = currentList,
            statusText = "Broadcasting live"
        )
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        // We do not disconnect globally so other parts can listen, but stop listening
    }
}

@Composable
fun LiveTrackingScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: LiveTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var busMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    LaunchedEffect(sessionId) {
        viewModel.startTracking(sessionId)
    }

    // Update map marker & polyline when coordinates update
    LaunchedEffect(uiState.lastLatitude, uiState.lastLongitude) {
        val lat = uiState.lastLatitude
        val lon = uiState.lastLongitude
        val map = mapViewInstance
        if (lat != null && lon != null && map != null) {
            val point = GeoPoint(lat, lon)

            if (busMarker == null) {
                val marker = Marker(map).apply {
                    position = point
                    title = "Live Bus"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(marker)
                busMarker = marker
                map.controller.setZoom(16.5)
                map.controller.animateTo(point)
            } else {
                busMarker?.position = point
                map.controller.animateTo(point)
            }

            // Update Polyline trail
            if (routePolyline == null) {
                val polyline = Polyline().apply {
                    outlinePaint.color = AppleBlue.toArgb()
                    outlinePaint.strokeWidth = 10f
                    setPoints(uiState.trail)
                }
                map.overlays.add(polyline)
                routePolyline = polyline
            } else {
                routePolyline?.setPoints(uiState.trail)
            }

            map.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full screen osmdroid map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(19.0330, 83.8320)) // Campus fallback center
                    mapViewInstance = this
                }
            }
        )

        // Floating Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LabelPrimary
                )
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🚍", fontSize = 14.sp)
                    Text(
                        text = "Live Bus Tracker",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LabelPrimary
                    )
                }
            }
        }

        // Apple Maps Style Bottom Sheet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SystemGray5)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiveBadge(isLive = uiState.isLive)
                        Text(
                            text = uiState.statusText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = LabelPrimary
                        )
                    }
                    Text(
                        text = uiState.lastUpdated,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemGray
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = SystemGroupedBackground
                )

                // ETA & Distance Stats
                AnimatedVisibility(
                    visible = uiState.isLive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.etaMinutes ?: "--"}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = LabelPrimary
                            )
                            Text(
                                text = "minutes ETA",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemGray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(44.dp)
                                .background(SystemGroupedBackground)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.distanceKm ?: "--"}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = LabelPrimary
                            )
                            Text(
                                text = "km away",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Center on Bus Action Button
                GhostTrackButton(
                    text = "Center on Bus",
                    enabled = uiState.lastLatitude != null,
                    containerColor = SystemGroupedBackground,
                    contentColor = AppleBlue,
                    onClick = {
                        val lat = uiState.lastLatitude
                        val lon = uiState.lastLongitude
                        if (lat != null && lon != null) {
                            mapViewInstance?.controller?.animateTo(GeoPoint(lat, lon))
                        }
                    }
                )
            }
        }
    }
}
