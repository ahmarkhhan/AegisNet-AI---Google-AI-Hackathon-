package com.aegisnet.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * GEMINI REASONING SERVICE — Real LLM-in-the-Loop
 *
 * Makes actual Google Gemini API calls for:
 *   1. Crisis severity assessment from citizen reports
 *   2. Escalation prediction with dynamic reasoning
 *   3. NLP entity extraction from free-text reports
 *
 * Falls back to deterministic heuristics if API key is absent or calls fail.
 */
@Service
@Slf4j
public class GeminiReasoningService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    /**
     * Assesses crisis severity by sending real context (citizen report + weather + recent events)
     * to Gemini and parsing a structured JSON response.
     *
     * @return A map with keys: severityScore (int 0-100), reasoning (String), crisisType (String), confidence (double)
     */
    public Map<String, Object> assessCrisisSeverity(String reportType, String description,
                                                      double lat, double lng, String zoneName,
                                                      int population, String weatherContext,
                                                      List<String> recentEventSummaries) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GeminiReasoning] No API key configured. Using deterministic fallback.");
            return fallbackSeverityAssessment(reportType, description);
        }

        String recentContext = recentEventSummaries.isEmpty()
                ? "No recent events in this zone."
                : String.join("\n", recentEventSummaries);

        String prompt = String.format("""
            You are a crisis intelligence analyst for Pakistan's National Disaster Management Authority.
            Analyze the following citizen report and context, then provide a structured severity assessment.

            CITIZEN REPORT:
            - Type: %s
            - Description: "%s"
            - Location: %s (%.4f, %.4f) — Population: %d

            ENVIRONMENTAL CONTEXT:
            %s

            RECENT EVENTS IN THIS ZONE:
            %s

            Respond ONLY with valid JSON (no markdown, no backticks):
            {
              "severityScore": <integer 0-100>,
              "reasoning": "<2-3 sentence explanation of your assessment>",
              "crisisType": "<one of: FLOOD, HEATWAVE, EARTHQUAKE, CYCLONE, RIOT, TERRORISM, LANDSLIDE, EPIDEMIC, TRAFFIC_BLOCKAGE, INFRASTRUCTURE_FAILURE, OTHER>",
              "confidence": <float 0.0-1.0>,
              "escalationFactors": ["<factor1>", "<factor2>"],
              "recommendedUrgency": "<IMMEDIATE|HIGH|MODERATE|LOW>"
            }
            """, reportType, description, zoneName, lat, lng, population, weatherContext, recentContext);

        try {
            JsonNode response = callGemini(prompt);
            String text = extractTextFromResponse(response);
            // Strip markdown code fence if Gemini wraps it
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
            log.info("[GeminiReasoning] Severity assessment completed. Score: {}, Type: {}", parsed.get("severityScore"), parsed.get("crisisType"));
            return parsed;
        } catch (Exception e) {
            log.error("[GeminiReasoning] Gemini severity call failed: {}. Using fallback.", e.getMessage());
            return fallbackSeverityAssessment(reportType, description);
        }
    }

    /**
     * Predicts escalation trajectory using Gemini with full situational context.
     *
     * @return A map with keys: escalationProbability (int), verdict (String), reasoning (String), timelineHours (int)
     */
    public Map<String, Object> predictEscalation(String crisisType, int currentSeverity,
                                                   String zoneName, int population,
                                                   boolean socialCorroboration,
                                                   String weatherSummary,
                                                   List<String> recentEventSummaries) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GeminiReasoning] No API key configured. Using deterministic fallback for escalation.");
            return fallbackEscalation(currentSeverity, socialCorroboration);
        }

        String recentContext = recentEventSummaries.isEmpty()
                ? "No recent events."
                : String.join("\n", recentEventSummaries);

        String prompt = String.format("""
            You are a predictive crisis analyst for Pakistan's NDMA Emergency Operations Center.
            Given the following active crisis, predict the escalation trajectory.

            ACTIVE CRISIS:
            - Type: %s
            - Current Severity: %d/100
            - Zone: %s (Population: %d)
            - Social media corroboration: %s
            - Weather: %s

            RECENT EVENTS IN AREA (last 2 hours):
            %s

            Consider: historical patterns, population density, infrastructure vulnerability, seasonal factors,
            and whether multiple concurrent crises compound the risk.

            Respond ONLY with valid JSON (no markdown):
            {
              "escalationProbability": <integer 0-99>,
              "verdict": "<IMMEDIATE_DISPATCH|DISPATCH_ADVISORY|ELEVATED_MONITORING|ROUTINE_MONITORING>",
              "reasoning": "<2-3 sentence explanation>",
              "timelineHours": <integer, estimated hours until peak severity>,
              "compoundingFactors": ["<factor1>", "<factor2>"]
            }
            """, crisisType, currentSeverity, zoneName, population,
                socialCorroboration ? "YES — multiple signals detected" : "NO — unverified report",
                weatherSummary, recentContext);

        try {
            JsonNode response = callGemini(prompt);
            String text = extractTextFromResponse(response);
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
            log.info("[GeminiReasoning] Escalation prediction: {}% — {}", parsed.get("escalationProbability"), parsed.get("verdict"));
            return parsed;
        } catch (Exception e) {
            log.error("[GeminiReasoning] Gemini escalation call failed: {}. Using fallback.", e.getMessage());
            return fallbackEscalation(currentSeverity, socialCorroboration);
        }
    }

    /**
     * Extracts crisis entities from free-text description using Gemini NLP.
     *
     * @return A map with keys: entities (List<String>), classification (String), sentimentUrgency (String)
     */
    public Map<String, Object> extractCrisisEntities(String description) {
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("entities", List.of("general_crisis"), "classification", "unclassified", "sentimentUrgency", "MEDIUM");
        }

        String prompt = String.format("""
            Extract crisis-relevant entities from this citizen emergency report. This may be in English, Urdu, or Roman Urdu.

            Report: "%s"

            Respond ONLY with valid JSON (no markdown):
            {
              "entities": ["<entity1>", "<entity2>"],
              "classification": "<NATURAL_DISASTER|INFRASTRUCTURE|SECURITY|PUBLIC_HEALTH|TRAFFIC|OTHER>",
              "sentimentUrgency": "<CRITICAL|HIGH|MEDIUM|LOW>",
              "keyPhrases": ["<phrase1>", "<phrase2>"]
            }
            """, description);

        try {
            JsonNode response = callGemini(prompt);
            String text = extractTextFromResponse(response);
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return objectMapper.readValue(text, Map.class);
        } catch (Exception e) {
            log.error("[GeminiReasoning] Entity extraction failed: {}", e.getMessage());
            return Map.of("entities", List.of("general_crisis"), "classification", "unclassified", "sentimentUrgency", "MEDIUM");
        }
    }

    // ═══ GEMINI REST API CALL ═══

    private JsonNode callGemini(String prompt) throws Exception {
        String url = GEMINI_URL + "?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", 0.3,
                "maxOutputTokens", 1024
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private String extractTextFromResponse(JsonNode response) {
        return response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");
    }

    // ═══ DETERMINISTIC FALLBACKS ═══

    private Map<String, Object> fallbackSeverityAssessment(String reportType, String description) {
        int baseSev = switch (reportType) {
            case "FLOOD_WATER", "FLOOD" -> 65;
            case "SEISMIC_ACTIVITY" -> 75;
            case "POLITICAL_RALLY" -> 45;
            case "TRAFFIC_BLOCKAGE" -> 30;
            case "ROAD_COLLAPSE" -> 60;
            default -> 50;
        };

        String lower = description != null ? description.toLowerCase() : "";
        if (lower.contains("trapped") || lower.contains("help") || lower.contains("emergency")) baseSev += 15;
        if (lower.contains("dead") || lower.contains("kill") || lower.contains("casualt")) baseSev += 20;
        baseSev = Math.min(100, baseSev);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("severityScore", baseSev);
        result.put("reasoning", String.format("Deterministic fallback: Type '%s' base=%d, keyword-adjusted to %d. (Gemini API unavailable)", reportType, baseSev - 15, baseSev));
        result.put("crisisType", reportType);
        result.put("confidence", 0.65);
        result.put("escalationFactors", List.of("keyword_match", "report_type_heuristic"));
        result.put("recommendedUrgency", baseSev >= 70 ? "IMMEDIATE" : baseSev >= 50 ? "HIGH" : "MODERATE");
        return result;
    }

    private Map<String, Object> fallbackEscalation(int currentSeverity, boolean socialCorroboration) {
        int prob = Math.min(99, currentSeverity + (socialCorroboration ? 12 : 5));
        String verdict = prob >= 75 ? "IMMEDIATE_DISPATCH" : prob >= 50 ? "DISPATCH_ADVISORY" : "ELEVATED_MONITORING";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("escalationProbability", prob);
        result.put("verdict", verdict);
        result.put("reasoning", String.format("Deterministic fallback: severity=%d + social=%s = %d%%. (Gemini API unavailable)", currentSeverity, socialCorroboration, prob));
        result.put("timelineHours", currentSeverity >= 70 ? 2 : 6);
        result.put("compoundingFactors", List.of("severity_level", socialCorroboration ? "social_corroboration" : "no_corroboration"));
        return result;
    }

    /**
     * Checks whether the Gemini API key is configured and live.
     */
    public boolean isGeminiAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
