package com.aegisnet.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val voiceTranscript: String?,
    val photoPath: String?,
    val videoPath: String?,
    val isSynced: Boolean = false,
    val placeName: String? = null,
    val nearbyHospitalsJson: String? = null,
    val nearbyPoliceJson: String? = null,
    val politicalParty: String? = null,
    val politicalImplications: String? = null,
    val seismicMagnitude: Double? = null,
    val seismicDepth: Double? = null,
    val seismicTremors: Boolean? = null
)

