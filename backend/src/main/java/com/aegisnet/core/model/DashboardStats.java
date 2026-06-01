package com.aegisnet.core.model;

/**
 * DTO for real-time dashboard statistics broadcast via WebSocket.
 */
public class DashboardStats {

    private int activeAlerts;
    private int signalsProcessed;
    private double avgResponseTimeMinutes;
    private double systemReadinessPercent;
    private long totalAffectedPopulation;
    private int activeAgents;
    private int dispatchesExecuted;
    private int totalCrisisEvents;

    // Agent health
    private String meteorologistStatus;
    private String socialVerifierStatus;
    private String newsIntelStatus;
    private String advancedIntelStatus;
    private String gdacsAgentStatus;

    public DashboardStats() {}

    // Getters and Setters
    public int getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(int activeAlerts) { this.activeAlerts = activeAlerts; }

    public int getSignalsProcessed() { return signalsProcessed; }
    public void setSignalsProcessed(int signalsProcessed) { this.signalsProcessed = signalsProcessed; }

    public double getAvgResponseTimeMinutes() { return avgResponseTimeMinutes; }
    public void setAvgResponseTimeMinutes(double avgResponseTimeMinutes) { this.avgResponseTimeMinutes = avgResponseTimeMinutes; }

    public double getSystemReadinessPercent() { return systemReadinessPercent; }
    public void setSystemReadinessPercent(double systemReadinessPercent) { this.systemReadinessPercent = systemReadinessPercent; }

    public long getTotalAffectedPopulation() { return totalAffectedPopulation; }
    public void setTotalAffectedPopulation(long totalAffectedPopulation) { this.totalAffectedPopulation = totalAffectedPopulation; }

    public int getActiveAgents() { return activeAgents; }
    public void setActiveAgents(int activeAgents) { this.activeAgents = activeAgents; }

    public int getDispatchesExecuted() { return dispatchesExecuted; }
    public void setDispatchesExecuted(int dispatchesExecuted) { this.dispatchesExecuted = dispatchesExecuted; }

    public int getTotalCrisisEvents() { return totalCrisisEvents; }
    public void setTotalCrisisEvents(int totalCrisisEvents) { this.totalCrisisEvents = totalCrisisEvents; }

    public String getMeteorologistStatus() { return meteorologistStatus; }
    public void setMeteorologistStatus(String meteorologistStatus) { this.meteorologistStatus = meteorologistStatus; }

    public String getSocialVerifierStatus() { return socialVerifierStatus; }
    public void setSocialVerifierStatus(String socialVerifierStatus) { this.socialVerifierStatus = socialVerifierStatus; }

    public String getNewsIntelStatus() { return newsIntelStatus; }
    public void setNewsIntelStatus(String newsIntelStatus) { this.newsIntelStatus = newsIntelStatus; }

    public String getAdvancedIntelStatus() { return advancedIntelStatus; }
    public void setAdvancedIntelStatus(String advancedIntelStatus) { this.advancedIntelStatus = advancedIntelStatus; }

    public String getGdacsAgentStatus() { return gdacsAgentStatus; }
    public void setGdacsAgentStatus(String gdacsAgentStatus) { this.gdacsAgentStatus = gdacsAgentStatus; }
}
