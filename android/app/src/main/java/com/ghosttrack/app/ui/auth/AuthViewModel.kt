package com.ghosttrack.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghosttrack.app.data.model.User
import com.ghosttrack.app.data.repository.AuthRepository
import com.ghosttrack.app.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun login(email: String, password: String, role: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.login(email.trim(), password, role)) {
                is AuthResult.Success -> _uiState.value = AuthUiState.Success(result.user)
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun signupStudent(name: String, email: String, password: String, rollNumber: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all required fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signup(
                name = name.trim(),
                email = email.trim(),
                password = password,
                role = "student",
                rollNumber = rollNumber.trim()
            )) {
                is AuthResult.Success -> _uiState.value = AuthUiState.Success(result.user)
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun signupDriver(name: String, email: String, password: String, phone: String, vehicle: String, routeInfo: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all required fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signup(
                name = name.trim(),
                email = email.trim(),
                password = password,
                role = "driver",
                phone = phone.trim(),
                vehicle = vehicle.trim(),
                routeInfo = routeInfo.trim()
            )) {
                is AuthResult.Success -> _uiState.value = AuthUiState.Success(result.user)
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }
}
