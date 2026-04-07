package com.ghosttrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghosttrack.app.api.GhostTrackApi
import com.ghosttrack.app.api.SessionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionState {
    object Idle : SessionState()
    object Loading : SessionState()
    data class Success(val sessionId: String) : SessionState()
    data class Error(val message: String) : SessionState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val api: GhostTrackApi
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState

    fun createSession(phone: String) {
        viewModelScope.launch {
            _sessionState.value = SessionState.Loading
            try {
                val response = api.createSession(SessionRequest(phone))
                _sessionState.value = SessionState.Success(response.sessionId)
            } catch (e: Exception) {
                _sessionState.value = SessionState.Error(e.message ?: "Failed to create session")
            }
        }
    }
}
