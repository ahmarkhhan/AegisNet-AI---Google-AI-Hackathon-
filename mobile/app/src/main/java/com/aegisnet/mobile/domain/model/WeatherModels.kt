package com.aegisnet.mobile.domain.model

data class WeatherTelemetry(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val condition: String
)
