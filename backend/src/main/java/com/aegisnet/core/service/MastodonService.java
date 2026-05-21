package com.aegisnet.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Mastodon Decentralized Intelligence Service
 * Polling mastodon.social for high-velocity public safety tags.
 */
@Service
@Slf4j
public class MastodonService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String MASTODON_TAG_API =
            "https://mastodon.social/api/v1/timelines/tag/%s?limit=20";

    private static final List<String> TAGS = List.of(
            "pakistan", "karachi", "lahore", "flood", "earthquake", "protest", "emergency"
    );

    // City keywords for mapping articles to cities
    private static final Map<String, List<String>> CITY_KEYWORDS = Map.of(
            "Karachi", List.of("karachi", "sindh", "clifton", "korangi"),
            "Lahore", List.of("lahore", "punjab", "gulberg", "model town"),
            "Islamabad", List.of("islamabad", "rawalpindi", "murree"),
            "Peshawar", List.of("peshawar", "kpk", "khyber", "swat"),
            "Quetta", List.of("quetta", "balochistan", "chaman")
    );

    public SocialSignalResult fetchMastodonSignals() {
        String tag = TAGS.get((int)((System.currentTimeMillis() / 60_000)) % TAGS.size());
        String url = String.format(MASTODON_TAG_API, tag);
        SocialSignalResult result = new SocialSignalResult("Mastodon");

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode statuses = objectMapper.readTree(jsonResponse);

            if (statuses.isMissingNode() || !statuses.isArray()) return result;

            for (JsonNode status : statuses) {
                String contentHtml = status.path("content").asText("");
                // Strip HTML tags for processing
                String text = contentHtml.replaceAll("<[^>]*>", "").toLowerCase();
                if (text.isEmpty()) continue;

                int severity = calculateSeverity(text);
                String matchedCity = mapToCity(text);

                if (matchedCity != null) {
                    result.addSignal(matchedCity, text, severity);
                } else if (severity > 30) {
                    result.addGeneralSignal(text, severity);
                }
            }
            log.info("[Mastodon] Processed signals for tag '{}': {}", tag, result.getCitySeverities());
        } catch (Exception e) {
            log.warn("[Mastodon] Failed to fetch: {}", e.getMessage());
        }
        return result;
    }

    private String mapToCity(String textLower) {
        for (Map.Entry<String, List<String>> entry : CITY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (textLower.contains(keyword)) return entry.getKey();
            }
        }
        return null;
    }

    private int calculateSeverity(String text) {
        int score = 0;
        if (text.contains("dead") || text.contains("killed") || text.contains("blast") || text.contains("bomb")) score += 45;
        if (text.contains("emergency") || text.contains("help") || text.contains("trapped")) score += 30;
        if (text.contains("protest") || text.contains("riot") || text.contains("fire")) score += 25;
        if (text.contains("flood") || text.contains("earthquake")) score += 20;
        if (text.contains("stuck") || text.contains("blocked") || text.contains("outage")) score += 15;
        return Math.min(score, 100);
    }
}
