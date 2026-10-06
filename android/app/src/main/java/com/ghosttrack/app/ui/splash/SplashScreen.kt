package com.ghosttrack.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghosttrack.app.data.repository.AuthRepository
import com.ghosttrack.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object RoleSelect : SplashDestination()
    object StudentDashboard : SplashDestination()
    object DriverDashboard : SplashDestination()
    object AdminDashboard : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableSharedFlow<SplashDestination>()
    val destination = _destination.asSharedFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            delay(1200) // Brief branded splash display
            if (authRepository.isLoggedIn()) {
                when (authRepository.getRole()) {
                    "student" -> _destination.emit(SplashDestination.StudentDashboard)
                    "driver" -> _destination.emit(SplashDestination.DriverDashboard)
                    "admin" -> _destination.emit(SplashDestination.AdminDashboard)
                    else -> _destination.emit(SplashDestination.RoleSelect)
                }
            } else {
                _destination.emit(SplashDestination.RoleSelect)
            }
        }
    }
}

@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.destination.collect { destination ->
            onNavigate(destination)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemGroupedBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceWhite),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚌", fontSize = 42.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "GIET Bus Tracker",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = LabelPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Live Campus Transit",
                style = MaterialTheme.typography.bodyMedium,
                color = SystemGray
            )
        }
    }
}
