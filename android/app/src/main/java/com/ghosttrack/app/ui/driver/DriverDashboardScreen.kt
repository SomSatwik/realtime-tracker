package com.ghosttrack.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghosttrack.app.data.model.TrackingSession
import com.ghosttrack.app.data.repository.AuthRepository
import com.ghosttrack.app.data.repository.SessionRepository
import com.ghosttrack.app.data.repository.SessionResult
import com.ghosttrack.app.ui.components.*
import com.ghosttrack.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DriverDashboardUiState {
    object Loading : DriverDashboardUiState()
    data class Success(
        val driverName: String,
        val activeSession: TrackingSession?
    ) : DriverDashboardUiState()
    data class Error(val message: String) : DriverDashboardUiState()
}

@HiltViewModel
class DriverDashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DriverDashboardUiState>(DriverDashboardUiState.Loading)
    val uiState: StateFlow<DriverDashboardUiState> = _uiState.asStateFlow()

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DriverDashboardUiState.Loading
            val name = authRepository.getUserName() ?: "Driver"
            when (val result = sessionRepository.getDriverStatus()) {
                is SessionResult.Success -> {
                    _uiState.value = DriverDashboardUiState.Success(
                        driverName = name,
                        activeSession = result.data
                    )
                }
                is SessionResult.Error -> {
                    _uiState.value = DriverDashboardUiState.Error(result.message)
                }
            }
        }
    }

    fun startSession(routeInfo: String, onSuccess: (TrackingSession) -> Unit) {
        viewModelScope.launch {
            _actionLoading.value = true
            when (val result = sessionRepository.startSession(routeInfo)) {
                is SessionResult.Success -> {
                    _actionLoading.value = false
                    loadDashboard()
                    onSuccess(result.data)
                }
                is SessionResult.Error -> {
                    _actionLoading.value = false
                    _uiState.value = DriverDashboardUiState.Error(result.message)
                }
            }
        }
    }

    fun stopSession(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _actionLoading.value = true
            when (sessionRepository.stopSession()) {
                is SessionResult.Success -> {
                    _actionLoading.value = false
                    loadDashboard()
                    onSuccess()
                }
                is SessionResult.Error -> {
                    _actionLoading.value = false
                    loadDashboard()
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}

@Composable
fun DriverDashboardScreen(
    onOpenBroadcast: (sessionId: String, routeInfo: String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DriverDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionLoading by viewModel.actionLoading.collectAsState()
    var routeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            GhostTrackTopBar(
                title = "Driver Portal",
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = LabelPrimary
                        )
                    }
                }
            )
        },
        containerColor = SystemGroupedBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is DriverDashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppleOrange)
                    }
                }
                is DriverDashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorCard(message = state.message, onRetry = { viewModel.loadDashboard() })
                    }
                }
                is DriverDashboardUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Driver Controls",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = LabelPrimary
                                        )
                                        Text(
                                            text = "Driver: ${state.driverName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemGray
                                        )
                                    }
                                    LiveBadge(isLive = state.activeSession != null)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (state.activeSession != null) {
                                    // Active session UI
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SystemGroupedBackground)
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "ACTIVE BROADCAST",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.08.sp
                                                ),
                                                color = SystemGray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = state.activeSession.routeInfo ?: "Campus Route",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = LabelPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Room: ${state.activeSession.socketRoomId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SystemGray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    GhostTrackButton(
                                        text = "📡 Open Live Broadcast Map",
                                        containerColor = AppleBlue,
                                        onClick = {
                                            onOpenBroadcast(
                                                state.activeSession.socketRoomId,
                                                state.activeSession.routeInfo ?: "Bus"
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    GhostTrackButton(
                                        text = "Stop Sharing",
                                        containerColor = AppleRed,
                                        isLoading = actionLoading,
                                        onClick = { viewModel.stopSession {} }
                                    )
                                } else {
                                    // Idle: Start new session UI
                                    GhostTrackTextField(
                                        value = routeInput,
                                        onValueChange = { routeInput = it },
                                        label = "Assigned Route / Bus Name",
                                        placeholder = "e.g. Bus 4 — Campus to Station"
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    GhostTrackButton(
                                        text = "Start Broadcasting Session",
                                        containerColor = LabelPrimary,
                                        isLoading = actionLoading,
                                        onClick = {
                                            val route = if (routeInput.isBlank()) "Campus Shuttle" else routeInput.trim()
                                            viewModel.startSession(route) { session ->
                                                onOpenBroadcast(session.socketRoomId, route)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Logout section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Account",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = LabelPrimary
                                )
                                TextButton(onClick = { viewModel.logout(onLogout) }) {
                                    Text(
                                        text = "Logout",
                                        color = AppleRed,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
