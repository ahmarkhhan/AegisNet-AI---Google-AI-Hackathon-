package com.aegisnet.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Bluesky Social Intelligence Service
 * Monitors decentralized real-time chatter on Bluesky AT Protocol without authentication.
 */
@Service
@Slf4j
public class BlueskyService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BSKY_SEARCH_API =
            "https://public.api.bsky.app/xrpc/app.bsky.feed.searchPosts?q=%s&limit=25";

    private static final List<String> QUERIES = List.of(
            "Pakistan emergency", "Karachi rain", "Lahore smog",
            "Islamabad protest", "Quetta blast", "Peshawar earthquake",
            "highway blocked", "power outage Pakistan"
    );

    // City keywords for mapping articles to cities
    private static final Map<String, List<String>> CITY_KEYWORDS = Map.of(
            "Karachi", List.of("karachi", "sindh", "clifton", "korangi"),
            "Lahore", List.of("lahore", "punjab", "gulberg", "model town"),
            "Islamabad", List.of("islamabad", "rawalpindi", "murree"),
            "Peshawar", List.of("peshawar", "kpk", "khyber", "swat"),
            "Quetta", List.of("quetta", "balochistan", "chaman")
    );

    public SocialSignalResult fetchBlueskySignals() {
        String query = QUERIES.get((int)((System.currentTimeMillis() / 60_000)) % QUERIES.size());
        String url = String.format(BSKY_SEARCH_API, encodeQuery(query));
        SocialSignalResult result = new SocialSignalResult("Bluesky");

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode posts = root.path("posts");

            if (posts.isMissingNode() || !posts.isArray()) return result;

            for (JsonNode post : posts) {
                String text = post.path("record").path("text").asText("").toLowerCase();
                if (text.isEmpty()) continue;

                int severity = calculateSeverity(text);
                String matchedCity = mapToCity(text);

                if (matchedCity != null) {
                    result.addSignal(matchedCity, text, severity);
                } else if (severity > 30) {
                    result.addGeneralSignal(text, severity);
                }
            }
            log.info("[Bluesky] Processed signals for query '{}': {}", query, result.getCitySeverities());
        } catch (Exception e) {
            log.warn("[Bluesky] Failed to fetch: {}", e.getMessage());
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

    private String encodeQuery(String query) {
        return query.replace(" ", "%20");
    }
}
