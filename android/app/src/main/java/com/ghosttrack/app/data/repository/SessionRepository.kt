package com.ghosttrack.app.data.repository

import com.ghosttrack.app.data.model.TrackingSession
import com.ghosttrack.app.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

sealed class SessionResult<out T> {
    data class Success<T>(val data: T) : SessionResult<T>()
    data class Error(val message: String) : SessionResult<Nothing>()
}

@Singleton
class SessionRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getActiveSessions(): SessionResult<List<TrackingSession>> {
        return try {
            val response = api.getActiveSessions()
            if (response.isSuccessful && response.body() != null) {
                SessionResult.Success(response.body()!!.sessions)
            } else {
                SessionResult.Error("Failed to fetch sessions")
            }
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getDriverStatus(): SessionResult<TrackingSession?> {
        return try {
            val response = api.getDriverStatus()
            if (response.isSuccessful && response.body() != null) {
                SessionResult.Success(response.body()!!.activeSession)
            } else {
                SessionResult.Error("Failed to fetch driver status")
            }
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun startSession(routeInfo: String?): SessionResult<TrackingSession> {
        return try {
            val response = api.startSession(routeInfo)
            if (response.isSuccessful && response.body() != null) {
                SessionResult.Success(response.body()!!.session)
            } else {
                SessionResult.Error("Failed to start session")
            }
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun stopSession(): SessionResult<Boolean> {
        return try {
            val response = api.stopSession()
            if (response.isSuccessful) {
                SessionResult.Success(true)
            } else {
                SessionResult.Error("Failed to stop session")
            }
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "Network error")
        }
    }
}
