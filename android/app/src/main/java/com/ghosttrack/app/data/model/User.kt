package com.ghosttrack.app.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val rollNumber: String? = null,
    val phone: String? = null,
    val vehicle: String? = null,
    val routeInfo: String? = null
)
