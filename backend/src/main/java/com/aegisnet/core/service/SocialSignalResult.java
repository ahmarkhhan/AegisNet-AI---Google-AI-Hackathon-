package com.aegisnet.core.service;

import lombok.Getter;
import java.util.*;

/**
 * Shared DTO for Social Media Signals (Bluesky, Mastodon, Reddit)
 */
@Getter
public class SocialSignalResult {
    private final String platform;
    private final Map<String, Integer> citySeverities = new HashMap<>();
    private final Map<String, Integer> citySignalCounts = new HashMap<>();
    private final List<String> traceMessages = new ArrayList<>();

    public SocialSignalResult(String platform) {
        this.platform = platform;
    }

    public void addSignal(String city, String text, int severity) {
        citySeverities.merge(city, severity, Math::max);
        citySignalCounts.merge(city, 1, Integer::sum);
        traceMessages.add(String.format("[%s] [%s] Signal: \"%s\" → Severity: %d", platform, city, truncate(text, 50), severity));
    }

    public void addGeneralSignal(String text, int severity) {
        int distributed = severity / 4;
        for (String city : List.of("Karachi", "Lahore", "Islamabad", "Peshawar", "Quetta")) {
            citySeverities.merge(city, distributed, Math::max);
        }
        traceMessages.add(String.format("[%s] [General] Signal: \"%s\" → Distributed: %d", platform, truncate(text, 50), distributed));
    }

    public int getSeverityForCity(String city) {
        return citySeverities.getOrDefault(city, 0);
    }

    public int getSignalCountForCity(String city) {
        return citySignalCounts.getOrDefault(city, 0);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
