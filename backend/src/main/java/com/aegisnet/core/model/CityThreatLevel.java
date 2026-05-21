package com.aegisnet.core.model;

import java.time.LocalDateTime;

/**
 * DTO representing the real-time threat assessment for a single city.
 * Pushed to the frontend via WebSocket on /topic/city-threats.
 */
public class CityThreatLevel {

    private String cityName;
    private double latitude;
    private double longitude;

    // Weather data
    private double temperatureC;
    private double precipitationMm;
    private double windSpeedKmh;
    private int humidityPct;
    private int weatherCode;
    private String weatherSummary;

    // Severity scores
    private int weatherSeverity;    // 0-100
    private int newsSeverity;       // 0-100
    private int overallThreatLevel; // 0-100

    // Classification
    private String threatCategory; // NOMINAL, ADVISORY, ELEVATED, HIGH, CRITICAL
    private String riskProfile;    // City-specific risk description

    // News
    private int activeNewsSignals;

    // Metadata
    private LocalDateTime lastUpdated;

    public CityThreatLevel() {}

    public CityThreatLevel(String cityName, double latitude, double longitude, String riskProfile) {
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.riskProfile = riskProfile;
        this.threatCategory = "NOMINAL";
        this.overallThreatLevel = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Computes the overall threat level and category from weather + news severity.
     */
    public void computeThreatLevel() {
        // Weather has 70% weight, news has 30% weight
        this.overallThreatLevel = (int) (weatherSeverity * 0.7 + newsSeverity * 0.3);
        this.overallThreatLevel = Math.min(overallThreatLevel, 100);

        if (overallThreatLevel >= 81) this.threatCategory = "CRITICAL";
        else if (overallThreatLevel >= 56) this.threatCategory = "HIGH";
        else if (overallThreatLevel >= 36) this.threatCategory = "ELEVATED";
        else if (overallThreatLevel >= 16) this.threatCategory = "ADVISORY";
        else this.threatCategory = "NOMINAL";

        this.lastUpdated = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(double temperatureC) { this.temperatureC = temperatureC; }

    public double getPrecipitationMm() { return precipitationMm; }
    public void setPrecipitationMm(double precipitationMm) { this.precipitationMm = precipitationMm; }

    public double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }

    public int getHumidityPct() { return humidityPct; }
    public void setHumidityPct(int humidityPct) { this.humidityPct = humidityPct; }

    public int getWeatherCode() { return weatherCode; }
    public void setWeatherCode(int weatherCode) { this.weatherCode = weatherCode; }

    public String getWeatherSummary() { return weatherSummary; }
    public void setWeatherSummary(String weatherSummary) { this.weatherSummary = weatherSummary; }

    public int getWeatherSeverity() { return weatherSeverity; }
    public void setWeatherSeverity(int weatherSeverity) { this.weatherSeverity = weatherSeverity; }

    public int getNewsSeverity() { return newsSeverity; }
    public void setNewsSeverity(int newsSeverity) { this.newsSeverity = newsSeverity; }

    public int getOverallThreatLevel() { return overallThreatLevel; }
    public void setOverallThreatLevel(int overallThreatLevel) { this.overallThreatLevel = overallThreatLevel; }

    public String getThreatCategory() { return threatCategory; }
    public void setThreatCategory(String threatCategory) { this.threatCategory = threatCategory; }

    public String getRiskProfile() { return riskProfile; }
    public void setRiskProfile(String riskProfile) { this.riskProfile = riskProfile; }

    public int getActiveNewsSignals() { return activeNewsSignals; }
    public void setActiveNewsSignals(int activeNewsSignals) { this.activeNewsSignals = activeNewsSignals; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
