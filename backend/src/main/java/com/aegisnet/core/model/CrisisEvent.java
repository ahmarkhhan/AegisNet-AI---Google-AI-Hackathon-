package com.aegisnet.core.model;

import java.time.LocalDateTime;

/**
 * Represents a specific crisis event detected by the autonomous agent.
 * Each event has a type, location, criticality, affected population, and detailed intelligence.
 */
public class CrisisEvent {

    private String id;
    private String type;        // FLOOD, EARTHQUAKE, LANDSLIDE, SNOWSTORM, HEATWAVE, CYCLONE, DROUGHT
    private String area;        // Specific area name
    private String region;      // Province/broader region
    private String heading;     // Brief crisis headline
    private String description; // Detailed crisis description

    // Criticality
    private String criticality;       // CRITICAL, SEVERE, HIGH, MODERATE, LOW
    private int criticalityScore;     // 0-100

    // Impact
    private int affectedPopulation;
    private int casualtyEstimate;
    private int displacedEstimate;
    private int infrastructureDamagePercent;

    // Geolocation
    private double latitude;
    private double longitude;
    private double radiusKm;   // Affected radius in km

    // Crisis management
    private String responseStatus;     // ACTIVE, MOBILIZING, MONITORING, CONTAINED
    private String resourcesDeployed;
    private String evacuationStatus;
    private String recommendedActions;

    // Intelligence source
    private String source;       // WEATHER_API, GDELT_NEWS, SOCIAL_TREND, HISTORICAL_PATTERN
    private double confidence;
    private LocalDateTime detectedAt;
    private LocalDateTime lastUpdated;

    public CrisisEvent() {}

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCriticality() { return criticality; }
    public void setCriticality(String criticality) { this.criticality = criticality; }

    public int getCriticalityScore() { return criticalityScore; }
    public void setCriticalityScore(int criticalityScore) { this.criticalityScore = criticalityScore; }

    public int getAffectedPopulation() { return affectedPopulation; }
    public void setAffectedPopulation(int affectedPopulation) { this.affectedPopulation = affectedPopulation; }

    public int getCasualtyEstimate() { return casualtyEstimate; }
    public void setCasualtyEstimate(int casualtyEstimate) { this.casualtyEstimate = casualtyEstimate; }

    public int getDisplacedEstimate() { return displacedEstimate; }
    public void setDisplacedEstimate(int displacedEstimate) { this.displacedEstimate = displacedEstimate; }

    public int getInfrastructureDamagePercent() { return infrastructureDamagePercent; }
    public void setInfrastructureDamagePercent(int p) { this.infrastructureDamagePercent = p; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    public String getResponseStatus() { return responseStatus; }
    public void setResponseStatus(String responseStatus) { this.responseStatus = responseStatus; }

    public String getResourcesDeployed() { return resourcesDeployed; }
    public void setResourcesDeployed(String resourcesDeployed) { this.resourcesDeployed = resourcesDeployed; }

    public String getEvacuationStatus() { return evacuationStatus; }
    public void setEvacuationStatus(String evacuationStatus) { this.evacuationStatus = evacuationStatus; }

    public String getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
