package com.aegisnet.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GDACS REAL-TIME DISASTER SERVICE
 *
 * Pulls live disaster alerts from the EU's Global Disaster Alert and Coordination System.
 * No API key. No registration. Real-time earthquake, flood, cyclone, drought data.
 * Bounding box filter: Pakistan + 100km surroundings (lat 20-38, lng 58-80).
 *
 * Source: https://www.gdacs.org/xml/rss.xml
 * Alert levels: Red (critical), Orange (severe), Green (low)
 */
@Service
@Slf4j
public class GdacsService {

    private final RestTemplate restTemplate;
    private static final String GDACS_RSS = "https://www.gdacs.org/xml/rss.xml";

    // Pakistan + 300km bounding box (catches border-region events)
    private static final double LAT_MIN = 19.0, LAT_MAX = 40.0;
    private static final double LNG_MIN = 56.0, LNG_MAX = 82.0;

    // Pakistan iso3 and neighbouring countries to catch regional disasters
    private static final Set<String> RELEVANT_ISO3 = Set.of(
        "PAK", "AFG", "IND", "IRN", "CHN"
    );

    public GdacsService() {
        this.restTemplate = new RestTemplate();
    }

    public GdacsResult fetchDisasters() {
        GdacsResult result = new GdacsResult();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AegisNet-CrisisIntelligence/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(GDACS_RSS, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[GDACS] HTTP {}", response.getStatusCode());
                return result;
            }

            // Strip BOM at byte level — GDACS RSS has UTF-8 BOM (0xEF, 0xBB, 0xBF)
            String xml = response.getBody();
            if (xml == null) return result;

            // Remove any leading whitespace or BOM characters
            xml = xml.strip();
            if (xml.charAt(0) == '\uFEFF') xml = xml.substring(1);
            // Also strip any non-XML leading characters
            int xmlStart = xml.indexOf("<?xml");
            if (xmlStart < 0) xmlStart = xml.indexOf("<rss");
            if (xmlStart > 0) xml = xml.substring(xmlStart);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            int total = items.getLength();
            int matched = 0;

            for (int i = 0; i < total; i++) {
                Element item = (Element) items.item(i);

                // Get lat/lng from geo:lat / geo:long elements
                double lat = getDouble(item, "geo:lat",  "lat");
                double lng = getDouble(item, "geo:long", "long");

                // Filter: must be in Pakistan+surrounding region OR be a Pakistan/neighbor country
                String iso3 = getText(item, "gdacs:iso3");
                String country = getText(item, "gdacs:country").toLowerCase();
                boolean byCoords  = inBounds(lat, lng);
                boolean byCountry = RELEVANT_ISO3.stream().anyMatch(iso -> iso3.contains(iso))
                    || country.contains("pakistan") || country.contains("afghanistan");
                if (!byCoords && !byCountry) continue;

                String title       = getText(item, "title");
                String desc        = getText(item, "description");
                String link        = getText(item, "link");
                String pubDate     = getText(item, "pubDate");
                String eventType   = getText(item, "gdacs:eventtype");
                String alertLevel  = getText(item, "gdacs:alertlevel");
                String alertScore  = getText(item, "gdacs:alertscore");
                String eventName   = getText(item, "gdacs:eventname");
                String eventId     = getText(item, "gdacs:eventid");
                String countryText = desc + " " + title;

                DisasterAlert alert = new DisasterAlert();
                alert.title      = title;
                alert.description = desc;
                alert.url        = link;
                alert.pubDate    = pubDate;
                alert.eventType  = eventType;
                alert.alertLevel = alertLevel;
                alert.alertScore = parseScore(alertScore);
                alert.eventName  = eventName;
                alert.eventId    = eventId;
                alert.latitude   = lat;
                alert.longitude  = lng;
                alert.severity   = alertToSeverity(alertLevel, parseScore(alertScore));
                alert.crisisType = eventTypeToCrisisType(eventType);
                alert.area       = extractArea(title + " " + desc);

                result.alerts.add(alert);
                result.traceMessages.add(String.format(
                    "[GDACS] %s %s | %s | Score:%.0f | %.2f,%.2f",
                    alertLevel.toUpperCase(), eventType, trunc(title, 45), alert.alertScore, lat, lng));
                matched++;
            }

            log.info("[GDACS] Scanned {} global events, {} in Pakistan region", total, matched);

        } catch (Exception e) {
            log.error("[GDACS] Error: {}", e.getMessage());
        }

        return result;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean inBounds(double lat, double lng) {
        return lat != Double.MAX_VALUE && lng != Double.MAX_VALUE
            && lat >= LAT_MIN && lat <= LAT_MAX
            && lng >= LNG_MIN && lng <= LNG_MAX;
    }

    private int alertToSeverity(String level, double score) {
        int base = switch (level.toLowerCase()) {
            case "red"    -> 75;
            case "orange" -> 50;
            default       -> 25;  // green
        };
        return (int) Math.min(base + score * 0.2, 100);
    }

    private String eventTypeToCrisisType(String et) {
        if (et == null) return "CRISIS_ALERT";
        return switch (et.toUpperCase()) {
            case "EQ"   -> "EARTHQUAKE";
            case "FL"   -> "FLOOD";
            case "TC"   -> "CYCLONE";
            case "DR"   -> "DROUGHT";
            case "WF"   -> "WILDFIRE";
            case "VO"   -> "VOLCANO";
            default     -> "CRISIS_ALERT";
        };
    }

    private String extractArea(String text) {
        String s = text.toLowerCase();
        if (s.contains("pakistan"))      return "Pakistan";
        if (s.contains("sindh"))         return "Sindh";
        if (s.contains("punjab"))        return "Punjab";
        if (s.contains("balochistan"))   return "Balochistan";
        if (s.contains("kpk")||s.contains("khyber")) return "KPK";
        if (s.contains("gilgit"))        return "Gilgit";
        if (s.contains("kashmir"))       return "Muzaffarabad";
        if (s.contains("karachi"))       return "Karachi";
        if (s.contains("lahore"))        return "Lahore";
        if (s.contains("islamabad"))     return "Islamabad";
        if (s.contains("peshawar"))      return "Peshawar";
        if (s.contains("quetta"))        return "Quetta";
        return "Pakistan";
    }

    private String getText(Element el, String tagName) {
        NodeList nl = el.getElementsByTagNameNS("*", tagName.contains(":") ? tagName.split(":")[1] : tagName);
        if (nl == null || nl.getLength() == 0)
            nl = el.getElementsByTagName(tagName);
        if (nl == null || nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }

    private double getDouble(Element el, String... tags) {
        for (String tag : tags) {
            String val = getText(el, tag);
            if (!val.isEmpty()) {
                try { return Double.parseDouble(val); } catch (NumberFormatException ignored) {}
            }
        }
        return Double.MAX_VALUE;
    }

    private double parseScore(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private String trunc(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ─── Data model ─────────────────────────────────────────────────────────

    public static class GdacsResult {
        public List<DisasterAlert> alerts       = new ArrayList<>();
        public List<String>        traceMessages = new ArrayList<>();
    }

    public static class DisasterAlert {
        public String title, description, url, pubDate;
        public String eventType, alertLevel, eventName, eventId, crisisType, area;
        public double alertScore, latitude, longitude;
        public int    severity;
    }
}
