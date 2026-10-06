package com.ghosttrack.app.data.repository

import com.ghosttrack.app.data.local.TokenStore
import com.ghosttrack.app.data.model.User
import com.ghosttrack.app.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String, role: String): AuthResult {
        return try {
            val response = api.login(email, password, role)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.saveAuthData(body.token, body.user)
                AuthResult.Success(body.user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Login failed"
                AuthResult.Error(parseError(errorBody))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun signup(
        name: String, email: String, password: String, role: String,
        rollNumber: String? = null, phone: String? = null,
        vehicle: String? = null, routeInfo: String? = null
    ): AuthResult {
        return try {
            val response = api.signup(name, email, password, role, rollNumber, phone, vehicle, routeInfo)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.saveAuthData(body.token, body.user)
                AuthResult.Success(body.user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Signup failed"
                AuthResult.Error(parseError(errorBody))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.getToken() != null
    suspend fun getRole(): String? = tokenStore.getRole()
    suspend fun getUserName(): String? = tokenStore.getUserName()

    suspend fun logout() {
        tokenStore.clear()
    }

    private fun parseError(body: String): String {
        return try {
            org.json.JSONObject(body).optString("error", "An error occurred")
        } catch (e: Exception) {
            "An error occurred"
        }
    }
}
