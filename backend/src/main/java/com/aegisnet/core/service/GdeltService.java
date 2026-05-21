package com.aegisnet.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * GDELT Project Integration Service
 * Fetches REAL-TIME global crisis news from the GDELT DOC 2.0 API (100% free, no API key).
 * Maps articles to the 5 monitored Pakistani cities.
 */
@Service
@Slf4j
public class GdeltService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GDELT_DOC_API =
            "https://api.gdeltproject.org/api/v2/doc/doc?query=%s&mode=ArtList&maxrecords=%d&format=json&timespan=24h";

    // Rotate simple single-term queries to avoid GDELT keyword-length limits
    private static final List<String> GDELT_QUERIES = List.of(
            "Pakistan flood",
            "Pakistan earthquake",
            "Pakistan disaster emergency",
            "Pakistan flood rescue",
            "Pakistan storm casualties"
    );

    // City keywords for mapping articles to cities
    private static final Map<String, List<String>> CITY_KEYWORDS = Map.of(
            "Karachi", List.of("karachi", "sindh", "port qasim", "clifton", "korangi"),
            "Lahore", List.of("lahore", "punjab", "gulberg", "model town", "johar town"),
            "Islamabad", List.of("islamabad", "rawalpindi", "murree", "margalla", "capital"),
            "Peshawar", List.of("peshawar", "kpk", "khyber", "swat", "tribal"),
            "Quetta", List.of("quetta", "balochistan", "baluchistan", "chaman", "zhob")
    );

    /**
     * Fetches crisis news from GDELT and returns a map of city -> news severity score.
     * Rotates through simple queries to avoid API keyword rejection.
     */
    public GdeltResult fetchCrisisNews() {
        // Rotate query every 90s (by minute-pair)
        String query = GDELT_QUERIES.get((int)((System.currentTimeMillis() / 90_000)) % GDELT_QUERIES.size());
        String url = String.format(GDELT_DOC_API, encodeQuery(query), 15);

        GdeltResult result = new GdeltResult();

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode articles = root.path("articles");

            if (articles.isMissingNode() || !articles.isArray() || articles.isEmpty()) {
                log.info("[GDELT] No crisis articles found for Pakistan in last 24h.");
                return result;
            }

            for (JsonNode article : articles) {
                String title = article.path("title").asText("").toLowerCase();
                String domain = article.path("domain").asText("unknown");
                int severity = calculateNewsSeverity(title);

                // Map article to the most relevant city
                String matchedCity = mapToCity(title);
                if (matchedCity != null) {
                    result.addArticle(matchedCity, title, domain, severity);
                } else {
                    // General Pakistan news — distribute small weight to all cities
                    result.addGeneralArticle(title, domain, severity);
                }
            }

            log.info("[GDELT] Processed {} articles. City matches: {}", 
                    articles.size(), result.getCitySeverities());

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("429")) {
                log.warn("[GDELT] Rate limit hit (429). Will retry next cycle.");
            } else {
                log.error("[GDELT] Failed to fetch: {}", msg);
            }
        }

        return result;
    }

    private String mapToCity(String titleLower) {
        for (Map.Entry<String, List<String>> entry : CITY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (titleLower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private int calculateNewsSeverity(String title) {
        String lower = title.toLowerCase();
        int score = 20;
        if (lower.contains("dead") || lower.contains("killed") || lower.contains("casualties")) score += 35;
        if (lower.contains("emergency") || lower.contains("urgent")) score += 20;
        if (lower.contains("flood") || lower.contains("earthquake") || lower.contains("storm")) score += 15;
        if (lower.contains("rescue") || lower.contains("trapped")) score += 15;
        if (lower.contains("warning") || lower.contains("alert")) score += 10;
        return Math.min(score, 100);
    }

    private String encodeQuery(String query) {
        return query.replace(" ", "%20").replace("(", "%28").replace(")", "%29");
    }

    /**
     * Container for GDELT processing results.
     */
    public static class GdeltResult {
        private final Map<String, Integer> citySeverities = new HashMap<>();
        private final Map<String, Integer> cityArticleCounts = new HashMap<>();
        private final List<String> traceMessages = new ArrayList<>();

        public void addArticle(String city, String title, String domain, int severity) {
            citySeverities.merge(city, severity, Math::max);
            cityArticleCounts.merge(city, 1, Integer::sum);
            traceMessages.add(String.format("[%s] \"%s\" (%s) → Severity: %d", 
                    city, truncate(title, 50), domain, severity));
        }

        public void addGeneralArticle(String title, String domain, int severity) {
            int distributed = severity / 5; // Low weight for unmatched articles
            for (String city : List.of("Karachi", "Lahore", "Islamabad", "Peshawar", "Quetta")) {
                citySeverities.merge(city, distributed, Math::max);
            }
            traceMessages.add(String.format("[General] \"%s\" (%s) → Distributed: %d", 
                    truncate(title, 50), domain, distributed));
        }

        public int getSeverityForCity(String city) {
            return citySeverities.getOrDefault(city, 0);
        }

        public int getArticleCountForCity(String city) {
            return cityArticleCounts.getOrDefault(city, 0);
        }

        public Map<String, Integer> getCitySeverities() { return citySeverities; }
        public List<String> getTraceMessages() { return traceMessages; }

        private String truncate(String s, int max) {
            return s.length() > max ? s.substring(0, max) + "..." : s;
        }
    }
}
