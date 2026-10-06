package com.ghosttrack.app.data.model

data class AuthResponse(
    val token: String,
    val user: User
)

data class SessionsResponse(
    val sessions: List<TrackingSession>
)

data class DriverStatusResponse(
    val activeSession: TrackingSession?
)

data class SessionResponse(
    val session: TrackingSession
)

data class AdminDashboardResponse(
    val users: List<User>,
    val sessions: List<TrackingSession>
)

data class SuccessResponse(
    val success: Boolean
)

data class ErrorResponse(
    val error: String
)
