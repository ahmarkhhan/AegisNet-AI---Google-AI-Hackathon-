package com.aegisnet.mobile.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// --- Geocoding Models ---
data class GeocodingResponse(
    @SerializedName("results") val results: List<GeocodingResult>,
    @SerializedName("status") val status: String
)

data class GeocodingResult(
    @SerializedName("formatted_address") val formattedAddress: String,
    @SerializedName("geometry") val geometry: Geometry
)

data class Geometry(
    @SerializedName("location") val location: LatLngLiteral
)

data class LatLngLiteral(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

// --- Places Models ---
data class PlacesResponse(
    @SerializedName("results") val results: List<PlaceResult>,
    @SerializedName("status") val status: String
)

data class PlaceResult(
    @SerializedName("name") val name: String,
    @SerializedName("vicinity") val vicinity: String?,
    @SerializedName("geometry") val geometry: Geometry
)

// --- Retrofit Interface ---
interface GooglePlacesApi {

    @GET("maps/api/geocode/json")
    suspend fun resolveAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodingResponse

    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String, // format: "lat,lng"
        @Query("radius") radius: Int,       // in meters
        @Query("type") type: String,        // e.g. "hospital", "police"
        @Query("key") apiKey: String
    ): PlacesResponse
}
