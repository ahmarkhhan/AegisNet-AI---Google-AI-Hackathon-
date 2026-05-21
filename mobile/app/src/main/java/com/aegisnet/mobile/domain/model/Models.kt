package com.aegisnet.mobile.domain.model

import java.util.*

data class CrisisAlert(
    val id: Long,
    val type: String,
    val title: String,
    val description: String,
    val casualtyRiskScore: Int,
    val escalationProbability: Int,
    val epicenterLat: Double? = null,
    val epicenterLng: Double? = null,
    val status: String = "PREDICTED",
    val createdAt: String = "",
    val affectedPopulation: Int = 0,
    val responseTimeMinutes: Int = 0,
    val resourcesDeployed: List<String> = emptyList(),
    val zone: String = "", // affected area/location
    val severity: String = "HIGH", // LOW, MEDIUM, HIGH, CRITICAL
    val placeName: String? = null,
    val nearbyHospitals: List<String> = emptyList(),
    val nearbyPolice: List<String> = emptyList(),
    val politicalParty: String? = null,
    val politicalImplications: String? = null,
    val seismicMagnitude: Double? = null,
    val seismicDepth: Double? = null,
    val seismicTremors: Boolean? = null,
    val photoPath: String? = null,
    val videoPath: String? = null,
    val timestamp: Long = System.currentTimeMillis() - (5 * 60 * 1000)
)

data class UserReportedCrisis(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val title: String,
    val description: String,
    val severity: String,
    val lat: Double,
    val lng: Double,
    val zone: String,
    val affectedCount: Int,
    val mediaUrls: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, VERIFIED, RESOLVED
)

data class ResourceAllocation(
    val id: String = UUID.randomUUID().toString(),
    val crisisId: Long,
    val region: String,
    val department: String, // Police, Medical, Fire, Military, NGO, etc.
    val resourceName: String, // SWAT Team, Ambulances, Fire Truck, etc.
    val count: Int,
    val assignedAt: Long = System.currentTimeMillis(),
    val estimatedArrivalMinutes: Int,
    val status: String = "DISPATCHED" // DISPATCHED, EN_ROUTE, ON_SITE, ACTIVE
)

data class EventSignal(
    val id: Long,
    val source: String,
    val type: String,
    val rawPayload: String,
    val severityScore: Int,
    val confidence: Double,
    val locationDescription: String = "",
    val timestamp: String = ""
)

data class DroneStatus(
    val droneId: String,
    val status: String,
    val batteryLevel: Int,
    val strandedHumansDetected: Int,
    val strandedVehiclesDetected: Int
)

data class IncidentReport(
    val id: Long,
    val type: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,
    val location: String,
    val reportedBy: String,
    val timestamp: String,
    val photoUrl: String? = null,
    val affectedCount: Int = 0
)

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
