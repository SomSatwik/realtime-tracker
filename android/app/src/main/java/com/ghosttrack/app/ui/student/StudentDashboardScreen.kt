package com.ghosttrack.app.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

sealed class StudentDashboardUiState {
    object Loading : StudentDashboardUiState()
    data class Success(val sessions: List<TrackingSession>, val studentName: String) : StudentDashboardUiState()
    data class Error(val message: String) : StudentDashboardUiState()
}

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Loading)
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = StudentDashboardUiState.Loading
            val name = authRepository.getUserName() ?: "Student"
            when (val result = sessionRepository.getActiveSessions()) {
                is SessionResult.Success -> {
                    _uiState.value = StudentDashboardUiState.Success(
                        sessions = result.data,
                        studentName = name
                    )
                }
                is SessionResult.Error -> {
                    _uiState.value = StudentDashboardUiState.Error(result.message)
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
fun StudentDashboardScreen(
    onTrackSession: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: StudentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            GhostTrackTopBar(
                title = "Student Portal",
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
                is StudentDashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppleBlue)
                    }
                }
                is StudentDashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorCard(message = state.message, onRetry = { viewModel.loadDashboard() })
                    }
                }
                is StudentDashboardUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Header card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Active Buses",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = LabelPrimary
                                        )
                                        Text(
                                            text = "Welcome, ${state.studentName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemGray
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(BlueTint)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${state.sessions.size} ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AppleBlue
                                        )
                                    }
                                }
                            }
                        }

                        // Buses list or empty state
                        if (state.sessions.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = "📭",
                                    title = "No buses live right now",
                                    subtitle = "Drivers will appear here when they start broadcasting from the campus."
                                )
                            }
                        } else {
                            items(state.sessions) { session ->
                                BusSessionCard(
                                    session = session,
                                    onTrack = { onTrackSession(session.socketRoomId) }
                                )
                            }
                        }

                        // Bottom Actions
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
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
}

@Composable
private fun BusSessionCard(
    session: TrackingSession,
    onTrack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🚍 ${session.routeInfo ?: "Campus Shuttle"}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = LabelPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Driver: ${session.driver?.name ?: "Assigned Driver"}${if (session.driver?.vehicle != null) " • ${session.driver.vehicle}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemGray
                    )
                }
                LiveBadge(isLive = true)
            }

            Spacer(modifier = Modifier.height(14.dp))

            GhostTrackButton(
                text = "Track Live Map →",
                onClick = onTrack,
                containerColor = LabelPrimary,
                contentColor = SurfaceWhite
            )
        }
    }
}
