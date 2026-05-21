package com.aegisnet.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * RELIEFWEB DISASTER INTELLIGENCE SERVICE
 *
 * Fetches REAL, verified disaster reports from UN OCHA ReliefWeb API.
 * Uses POST with JSON filter body — the correct ReliefWeb v1 API pattern.
 *
 * 100% free, no API key required.
 */
@Service
@Slf4j
public class ReliefWebService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    private static final String BASE = "https://api.reliefweb.int/v1/";

    public ReliefWebService() {
        this.restTemplate = new RestTemplate();
    }

    public ReliefWebResult fetchDisasterIntelligence() {
        ReliefWebResult result = new ReliefWebResult();
        fetchDisasters(result);
        fetchReports(result);
        return result;
    }

    // ─── Active Disasters ────────────────────────────────────────────────────

    private void fetchDisasters(ReliefWebResult result) {
        String url = BASE + "disasters?appname=aegisnet-ai&limit=10&sort[]=date:desc";

        // JSON filter body
        String body = """
            {
              "filter": {
                "operator": "AND",
                "conditions": [
                  { "field": "country.iso3", "value": "PAK" },
                  { "field": "status", "value": ["alert","ongoing"], "operator": "OR" }
                ]
              },
              "fields": {
                "include": ["name","date.created","status","type.name","glide","url","description-html"]
              }
            }
            """;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "AegisNet-CrisisIntelligence/1.0");
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[ReliefWeb] Disasters: HTTP {}", response.getStatusCode());
                return;
            }

            JsonNode data = objectMapper.readTree(response.getBody()).path("data");
            if (!data.isArray()) return;

            for (JsonNode item : data) {
                JsonNode f = item.path("fields");
                String name     = f.path("name").asText("");
                String status   = f.path("status").asText("");
                String glide    = f.path("glide").asText("N/A");
                String rwUrl    = f.path("url").asText("");
                String date     = f.path("date").path("created").asText("");
                String typeName = "";
                JsonNode types  = f.path("type");
                if (types.isArray() && types.size() > 0)
                    typeName = types.get(0).path("name").asText("");

                if (name.isEmpty()) continue;

                DisasterRecord dr = new DisasterRecord();
                dr.name        = name;
                dr.status      = status;
                dr.type        = typeName;
                dr.crisisType  = mapType(typeName);
                dr.glide       = glide;
                dr.url         = rwUrl;
                dr.dateCreated = date;
                dr.area        = extractArea(name);
                result.disasters.add(dr);
                result.traceMessages.add(
                    String.format("[DISASTER] %s | %s | Status: %s", trunc(name, 55), typeName, status));
            }
            log.info("[ReliefWeb] {} active disasters for Pakistan", result.disasters.size());

        } catch (Exception e) {
            log.error("[ReliefWeb] Disasters error: {}", e.getMessage());
        }
    }

    // ─── Recent Reports ───────────────────────────────────────────────────────

    private void fetchReports(ReliefWebResult result) {
        String url = BASE + "reports?appname=aegisnet-ai&limit=10&sort[]=date:desc";

        String body = """
            {
              "filter": {
                "operator": "AND",
                "conditions": [
                  { "field": "country.iso3", "value": "PAK" },
                  { "field": "theme.name",
                    "value": ["Disaster Management","Natural Disasters and Floods",
                              "Water Sanitation Hygiene","Food and Nutrition","Shelter and Non-Food Items"],
                    "operator": "OR" }
                ]
              },
              "fields": {
                "include": ["title","date.created","source.name","theme.name","disaster.name",
                            "disaster_type.name","url","body-html"]
              }
            }
            """;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "AegisNet-CrisisIntelligence/1.0");
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[ReliefWeb] Reports: HTTP {}", response.getStatusCode());
                return;
            }

            JsonNode data = objectMapper.readTree(response.getBody()).path("data");
            if (!data.isArray()) return;

            for (JsonNode item : data) {
                JsonNode f     = item.path("fields");
                String title   = f.path("title").asText("");
                String rwUrl   = f.path("url").asText("");
                String date    = f.path("date").path("created").asText("");

                JsonNode srcs  = f.path("source");
                String source  = srcs.isArray() && srcs.size() > 0
                    ? srcs.get(0).path("name").asText("") : "";

                List<String> themes = new ArrayList<>();
                if (f.path("theme").isArray())
                    for (JsonNode t : f.path("theme")) themes.add(t.path("name").asText(""));

                if (title.isEmpty()) continue;

                ReportRecord rr = new ReportRecord();
                rr.title       = title;
                rr.source      = source;
                rr.url         = rwUrl;
                rr.dateCreated = date;
                rr.themes      = themes;
                rr.area        = extractArea(title);
                rr.severity    = reportSeverity(title, themes);
                result.reports.add(rr);
                result.traceMessages.add(
                    String.format("[REPORT] \"%s\" — %s", trunc(title, 55), source));
            }
            log.info("[ReliefWeb] {} reports for Pakistan", result.reports.size());

        } catch (Exception e) {
            log.error("[ReliefWeb] Reports error: {}", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String extractArea(String text) {
        String s = text.toLowerCase();
        if (s.contains("sindh"))                              return "Sindh";
        if (s.contains("balochistan")||s.contains("baluchistan")) return "Balochistan";
        if (s.contains("punjab"))                            return "Punjab";
        if (s.contains("khyber")||s.contains("kpk"))        return "KPK";
        if (s.contains("gilgit"))                            return "Gilgit";
        if (s.contains("kashmir")||s.contains("ajk"))       return "AJK";
        if (s.contains("karachi"))                           return "Karachi";
        if (s.contains("lahore"))                            return "Lahore";
        if (s.contains("islamabad"))                         return "Islamabad";
        if (s.contains("peshawar"))                          return "Peshawar";
        if (s.contains("quetta"))                            return "Quetta";
        if (s.contains("swat"))                              return "Swat Valley";
        if (s.contains("murree"))                            return "Murree";
        if (s.contains("chitral"))                           return "Chitral";
        if (s.contains("sukkur"))                            return "Sukkur";
        if (s.contains("jacobabad"))                         return "Jacobabad";
        return "Pakistan";
    }

    private String mapType(String t) {
        if (t == null) return "CRISIS_ALERT";
        String l = t.toLowerCase();
        if (l.contains("flood"))      return "FLOOD";
        if (l.contains("earthquake")) return "EARTHQUAKE";
        if (l.contains("cyclone")||l.contains("storm")) return "CYCLONE";
        if (l.contains("drought"))    return "DROUGHT";
        if (l.contains("heat"))       return "HEATWAVE";
        if (l.contains("landslide")||l.contains("mudslide")) return "LANDSLIDE";
        if (l.contains("cold")||l.contains("snow")) return "SNOWSTORM";
        if (l.contains("epidemic")||l.contains("disease")) return "EPIDEMIC";
        return "CRISIS_ALERT";
    }

    private int reportSeverity(String title, List<String> themes) {
        int s = 28;
        String l = title.toLowerCase();
        if (l.contains("flash update")||l.contains("situation report")) s += 18;
        if (l.contains("emergency"))   s += 15;
        if (l.contains("death")||l.contains("killed")||l.contains("casualties")) s += 22;
        if (l.contains("displaced")||l.contains("evacuated")) s += 14;
        if (l.contains("critical")||l.contains("severe"))     s += 10;
        if (themes.stream().anyMatch(t -> t.toLowerCase().contains("food"))) s += 8;
        return Math.min(s, 100);
    }

    private String trunc(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ─── Result containers ────────────────────────────────────────────────────

    public static class ReliefWebResult {
        public List<DisasterRecord> disasters    = new ArrayList<>();
        public List<ReportRecord>   reports      = new ArrayList<>();
        public List<String>         traceMessages = new ArrayList<>();
    }

    public static class DisasterRecord {
        public String name, status, type, crisisType, glide, url, dateCreated, area;
    }

    public static class ReportRecord {
        public String title, source, url, dateCreated, area;
        public List<String> themes;
        public int severity;
        public String disasterName;
    }
}
