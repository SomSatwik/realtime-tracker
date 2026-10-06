package com.ghosttrack.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ghost_track_prefs")

class TokenStore(private val context: Context) {

    companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userRoleFlow: Flow<String?> = context.dataStore.data.map { it[USER_ROLE_KEY] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }

    suspend fun getToken(): String? = context.dataStore.data.first()[TOKEN_KEY]
    suspend fun getRole(): String? = context.dataStore.data.first()[USER_ROLE_KEY]
    suspend fun getUserName(): String? = context.dataStore.data.first()[USER_NAME_KEY]
    suspend fun getUserId(): String? = context.dataStore.data.first()[USER_ID_KEY]

    suspend fun saveAuthData(token: String, user: com.ghosttrack.app.data.model.User) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = user.id
            prefs[USER_NAME_KEY] = user.name
            prefs[USER_EMAIL_KEY] = user.email
            prefs[USER_ROLE_KEY] = user.role
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
