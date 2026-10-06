package com.ghosttrack.app.data.remote

import com.ghosttrack.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @FormUrlEncoded
    @POST("/api/auth/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("role") role: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("/api/auth/signup")
    suspend fun signup(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("role") role: String,
        @Field("rollNumber") rollNumber: String? = null,
        @Field("phone") phone: String? = null,
        @Field("vehicle") vehicle: String? = null,
        @Field("routeInfo") routeInfo: String? = null
    ): Response<AuthResponse>

    // Sessions
    @GET("/api/sessions/active")
    suspend fun getActiveSessions(): Response<SessionsResponse>

    @GET("/api/sessions/driver/status")
    suspend fun getDriverStatus(): Response<DriverStatusResponse>

    @FormUrlEncoded
    @POST("/api/sessions/driver/start")
    suspend fun startSession(
        @Field("routeInfo") routeInfo: String?
    ): Response<SessionResponse>

    @POST("/api/sessions/driver/stop")
    suspend fun stopSession(): Response<SuccessResponse>

    // Admin
    @GET("/api/admin/dashboard")
    suspend fun getAdminDashboard(): Response<AdminDashboardResponse>

    @POST("/api/admin/users/{id}/delete")
    suspend fun deleteUser(@Path("id") userId: String): Response<SuccessResponse>

    @POST("/api/admin/sessions/{id}/end")
    suspend fun endSession(@Path("id") sessionId: String): Response<SuccessResponse>
}
