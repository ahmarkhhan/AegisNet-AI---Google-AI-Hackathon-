package com.aegisnet.core.model;

import java.time.LocalDateTime;

/**
 * DTO representing a dispatch manifest record.
 * Generated autonomously by the agent when severity exceeds threshold.
 */
public class DispatchManifest {

    private String eventId;
    private String targetAgency;
    private String zone;
    private double latitude;
    private double longitude;
    private int squadsDeployed;
    private String status; // PENDING, ACKNOWLEDGED, EN_ROUTE, ON_SITE
    private String manifestFile;
    private String authorization;
    private LocalDateTime createdAt;

    public DispatchManifest() {}

    public DispatchManifest(String eventId, String targetAgency, String zone, double latitude, double longitude,
                            int squadsDeployed, String manifestFile) {
        this.eventId = eventId;
        this.targetAgency = targetAgency;
        this.zone = zone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.squadsDeployed = squadsDeployed;
        this.status = "PENDING";
        this.manifestFile = manifestFile;
        this.authorization = "ANTIGRAVITY_SWARM_AUTO";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTargetAgency() { return targetAgency; }
    public void setTargetAgency(String targetAgency) { this.targetAgency = targetAgency; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getSquadsDeployed() { return squadsDeployed; }
    public void setSquadsDeployed(int squadsDeployed) { this.squadsDeployed = squadsDeployed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getManifestFile() { return manifestFile; }
    public void setManifestFile(String manifestFile) { this.manifestFile = manifestFile; }

    public String getAuthorization() { return authorization; }
    public void setAuthorization(String authorization) { this.authorization = authorization; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
