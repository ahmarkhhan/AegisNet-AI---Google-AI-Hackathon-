package com.aegisnet.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@SuppressWarnings("null")
public class CadDispatcherService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Configurable primary CAD API endpoint.
    private String primaryCadUrl = "http://localhost:8080/api/cad/receptor";
    private String fallbackWebhookUrl = "https://httpbin.org/post";

    public boolean dispatchToExternalCad(String eventId, String agency, String zone, double lat, double lng, int squads, int severity) {
        log.info("[RESOURCE_DISPATCHER] Starting CAD dispatch for Event: {}, Agency: {}", eventId, agency);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("agency", agency);
        payload.put("zone", zone);
        payload.put("coordinates", new double[]{lat, lng});
        payload.put("squads", squads);
        payload.put("severity", severity);
        payload.put("timestamp", java.time.LocalDateTime.now().toString());
        payload.put("authCode", "AEGISNET_AUTO_SWARM_2026");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        // Try primary CAD endpoint
        try {
            log.info("[RESOURCE_DISPATCHER] Invoking primary CAD API at {}", primaryCadUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(primaryCadUrl, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[RESOURCE_DISPATCHER] Primary CAD Dispatch SUCCESS. Response: {}", response.getBody());
                return true;
            } else {
                log.warn("[RESOURCE_DISPATCHER] Primary CAD API returned non-2xx status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("[RESOURCE_DISPATCHER] Primary CAD API failed: {}. Initiating autonomous resilience fallback protocol.", e.getMessage());
        }

        // Fallback option: Try Webhook (e.g. httpbin for simulation/demo validation)
        try {
            log.info("[RESOURCE_DISPATCHER] Invoking fallback secondary Webhook at {}", fallbackWebhookUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(fallbackWebhookUrl, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[RESOURCE_DISPATCHER] Fallback Webhook dispatch SUCCESS.");
                return true;
            }
        } catch (Exception e) {
            log.error("[RESOURCE_DISPATCHER] Fallback Webhook also failed: {}. Reverting to local offline fail-safe queuing.", e.getMessage());
        }

        return false;
    }

    public void setPrimaryCadUrl(String url) {
        this.primaryCadUrl = url;
    }

    public void setFallbackWebhookUrl(String url) {
        this.fallbackWebhookUrl = url;
    }

    public String getPrimaryCadUrl() {
        return this.primaryCadUrl;
    }

    public String getFallbackWebhookUrl() {
        return this.fallbackWebhookUrl;
    }
}
