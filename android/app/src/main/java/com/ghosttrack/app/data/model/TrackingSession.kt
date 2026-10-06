package com.ghosttrack.app.data.model

import com.google.gson.annotations.SerializedName

data class TrackingSession(
    @SerializedName("_id") val id: String = "",
    val driver: DriverInfo? = null,
    val routeInfo: String? = null,
    val status: String = "active",
    val socketRoomId: String = "",
    val createdAt: String? = null,
    val endedAt: String? = null
)

data class DriverInfo(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val vehicle: String? = null,
    val email: String? = null
)
