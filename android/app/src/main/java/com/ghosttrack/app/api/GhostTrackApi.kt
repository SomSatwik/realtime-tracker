package com.ghosttrack.app.api

import retrofit2.http.Body
import retrofit2.http.POST

data class SessionRequest(val mobile: String)
data class SessionResponse(val sessionId: String)

interface GhostTrackApi {
    @POST("/api/session")
    suspend fun createSession(@Body req: SessionRequest): SessionResponse
}
