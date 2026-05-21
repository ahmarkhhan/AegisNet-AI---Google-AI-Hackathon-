package com.aegisnet.mobile.data.remote

import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.domain.model.DroneStatus
import com.aegisnet.mobile.domain.model.EventSignal
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AegisNetApi {

    @GET("api/alerts")
    suspend fun getActiveAlerts(): List<CrisisAlert>

    @GET("api/signals")
    suspend fun getRecentSignals(): List<EventSignal>

    @GET("api/drones")
    suspend fun getDroneStatuses(): List<DroneStatus>

    @POST("api/simulation/weather")
    suspend fun triggerWeatherAnomaly()

    @POST("api/simulation/social")
    suspend fun triggerSocialPanic()

    @POST("api/citizen/report")
    suspend fun reportIncident(
        @Query("type") type: String,
        @Query("description") description: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    )
}
