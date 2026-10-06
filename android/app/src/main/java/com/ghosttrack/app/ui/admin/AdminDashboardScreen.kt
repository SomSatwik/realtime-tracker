package com.ghosttrack.app.ui.admin

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
import com.ghosttrack.app.data.model.User
import com.ghosttrack.app.data.repository.AdminDashboard
import com.ghosttrack.app.data.repository.AdminRepository
import com.ghosttrack.app.data.repository.AuthRepository
import com.ghosttrack.app.data.repository.SessionResult
import com.ghosttrack.app.ui.components.*
import com.ghosttrack.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AdminDashboardUiState {
    object Loading : AdminDashboardUiState()
    data class Success(val dashboard: AdminDashboard, val adminEmail: String) : AdminDashboardUiState()
    data class Error(val message: String) : AdminDashboardUiState()
}

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading)
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = AdminDashboardUiState.Loading
            val email = authRepository.getUserName() ?: "Admin"
            when (val result = adminRepository.getDashboard()) {
                is SessionResult.Success -> {
                    _uiState.value = AdminDashboardUiState.Success(
                        dashboard = result.data,
                        adminEmail = email
                    )
                }
                is SessionResult.Error -> {
                    _uiState.value = AdminDashboardUiState.Error(result.message)
                }
            }
        }
    }

    fun endSession(sessionId: String) {
        viewModelScope.launch {
            adminRepository.endSession(sessionId)
            loadDashboard()
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            adminRepository.deleteUser(userId)
            loadDashboard()
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
fun AdminDashboardScreen(
    onTrackSession: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            GhostTrackTopBar(
                title = "Admin Console",
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
                is AdminDashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppleBlue)
                    }
                }
                is AdminDashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorCard(message = state.message, onRetry = { viewModel.loadDashboard() })
                    }
                }
                is AdminDashboardUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Admin Header
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = "🛡️", fontSize = 24.sp)
                                        Column {
                                            Text(
                                                text = "System Administrator",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = LabelPrimary
                                            )
                                            Text(
                                                text = state.adminEmail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SystemGray
                                            )
                                        }
                                    }
                                    TextButton(onClick = { viewModel.logout(onLogout) }) {
                                        Text(
                                            text = "Logout",
                                            color = AppleRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Bus Sessions Section Header
                        item {
                            Text(
                                text = "BUS SESSIONS (${state.dashboard.sessions.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.08.sp
                                ),
                                color = SystemGray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (state.dashboard.sessions.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = "📋",
                                    title = "No bus sessions found",
                                    subtitle = "Active sessions created by drivers will appear here."
                                )
                            }
                        } else {
                            items(state.dashboard.sessions) { session ->
                                AdminSessionCard(
                                    session = session,
                                    onViewMap = { onTrackSession(session.socketRoomId) },
                                    onEnd = { viewModel.endSession(session.id) }
                                )
                            }
                        }

                        // Registered Users Section Header
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "REGISTERED USERS (${state.dashboard.users.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.08.sp
                                ),
                                color = SystemGray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (state.dashboard.users.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = "👥",
                                    title = "No users found",
                                    subtitle = "Registered accounts will appear here."
                                )
                            }
                        } else {
                            items(state.dashboard.users) { user ->
                                AdminUserCard(
                                    user = user,
                                    onDelete = { viewModel.deleteUser(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSessionCard(
    session: TrackingSession,
    onViewMap: () -> Unit,
    onEnd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        text = session.routeInfo ?: "Campus Route",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = LabelPrimary
                    )
                    Text(
                        text = "Driver: ${session.driver?.name ?: "Unknown"} (${session.driver?.email ?: ""})",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemGray
                    )
                    Text(
                        text = "Room: ${session.socketRoomId}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = SystemGray
                    )
                }
                LiveBadge(isLive = session.status == "active")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewMap,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemGroupedBackground)
                ) {
                    Text(
                        text = "View Map",
                        color = AppleBlue,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (session.status == "active") {
                    Button(
                        onClick = onEnd,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedTint)
                    ) {
                        Text(
                            text = "End Session",
                            color = AppleRed,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(
    user: User,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = LabelPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (user.role) {
                                    "driver" -> Color(0x26FF9500)
                                    "admin" -> BlueTint
                                    else -> SystemGroupedBackground
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = user.role.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = when (user.role) {
                                "driver" -> AppleOrange
                                "admin" -> AppleBlue
                                else -> LabelPrimary
                            }
                        )
                    }
                }
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemGray
                )
                if (!user.rollNumber.isNullOrEmpty()) {
                    Text(
                        text = "Roll: ${user.rollNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemGray
                    )
                }
                if (!user.vehicle.isNullOrEmpty()) {
                    Text(
                        text = "Vehicle: ${user.vehicle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemGray
                    )
                }
            }

            if (user.role != "admin") {
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = AppleRed,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Text(
                    text = "Protected",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemGray,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
