package com.ghosttrack.app.ui.driver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghosttrack.app.data.remote.LocationUpdate
import com.ghosttrack.app.data.remote.SocketManager
import com.ghosttrack.app.service.LocationForegroundService
import com.ghosttrack.app.ui.components.GhostTrackButton
import com.ghosttrack.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BroadcastState(
    val isSharing: Boolean = false,
    val coordinates: String = "",
    val statusText: String = "Ready",
    val logs: List<String> = emptyList()
)

@HiltViewModel
class DriverBroadcastViewModel @Inject constructor(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(BroadcastState())
    val state: StateFlow<BroadcastState> = _state.asStateFlow()

    fun setSharing(isSharing: Boolean) {
        val status = if (isSharing) "Sharing Live" else "Stopped"
        _state.value = _state.value.copy(
            isSharing = isSharing,
            statusText = status
        )
        addLog(if (isSharing) "Broadcasting started." else "Broadcasting stopped.")
    }

    fun updateCoordinates(lat: Double, lon: Double) {
        val coords = String.format(Locale.getDefault(), "%.5f°N, %.5f°E", lat, lon)
        _state.value = _state.value.copy(coordinates = coords)
        addLog(String.format(Locale.getDefault(), "Sent: %.5f, %.5f", lat, lon))
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "[$time] $message"
        val currentLogs = _state.value.logs.toMutableList()
        currentLogs.add(0, newLog)
        if (currentLogs.size > 6) currentLogs.removeAt(currentLogs.size - 1)
        _state.value = _state.value.copy(logs = currentLogs)
    }
}

@Composable
fun DriverBroadcastScreen(
    sessionId: String,
    busName: String,
    onBack: () -> Unit,
    viewModel: DriverBroadcastViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Permissions launcher
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            LocationForegroundService.startService(context, sessionId)
            viewModel.setSharing(true)
        }
    }

    fun toggleBroadcasting() {
        if (state.isSharing) {
            LocationForegroundService.stopService(context)
            viewModel.setSharing(false)
        } else {
            val hasLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocation) {
                LocationForegroundService.startService(context, sessionId)
                viewModel.setSharing(true)
            } else {
                launcher.launch(permissionsToRequest)
            }
        }
    }

    // Pulse animation for active sharing circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val circleColor by animateColorAsState(
        targetValue = if (state.isSharing) AppleGreen else SystemGroupedBackground,
        label = "circleColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (state.isSharing) SurfaceWhite else SystemGray,
        label = "iconTint"
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LabelPrimary
                    )
                }
                Text(
                    text = busName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SystemGray
                )
            }
        },
        containerColor = SurfaceWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Hero Sharing Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isSharing) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(AppleGreen.copy(alpha = pulseAlpha))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(circleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚍",
                            fontSize = 44.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = if (state.isSharing) AppleGreen else LabelPrimary
                )

                if (state.coordinates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.coordinates,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemGray
                    )
                }
            }

            // Fading Logs Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SystemGroupedBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TRANSMISSION LOG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp
                        ),
                        color = SystemGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = LabelPrimary
                            )
                        }
                    }
                }
            }

            // Bottom Action Button
            GhostTrackButton(
                text = if (state.isSharing) "Stop Broadcasting" else "Start Broadcasting",
                containerColor = if (state.isSharing) AppleRed else LabelPrimary,
                onClick = { toggleBroadcasting() }
            )
        }
    }
}
