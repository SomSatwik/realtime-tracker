package com.ghosttrack.app.data.repository

import com.ghosttrack.app.data.model.TrackingSession
import com.ghosttrack.app.data.model.User
import com.ghosttrack.app.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

data class AdminDashboard(
    val users: List<User>,
    val sessions: List<TrackingSession>
)

@Singleton
class AdminRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getDashboard(): SessionResult<AdminDashboard> {
        return try {
            val response = api.getAdminDashboard()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                SessionResult.Success(AdminDashboard(body.users, body.sessions))
            } else {
                SessionResult.Error("Failed to fetch admin dashboard")
            }
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteUser(userId: String): SessionResult<Boolean> {
        return try {
            val response = api.deleteUser(userId)
            if (response.isSuccessful) SessionResult.Success(true)
            else SessionResult.Error("Failed to delete user")
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun endSession(sessionId: String): SessionResult<Boolean> {
        return try {
            val response = api.endSession(sessionId)
            if (response.isSuccessful) SessionResult.Success(true)
            else SessionResult.Error("Failed to end session")
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }
}
