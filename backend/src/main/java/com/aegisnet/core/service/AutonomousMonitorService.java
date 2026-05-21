package com.aegisnet.core.service;

import com.aegisnet.core.model.CityThreatLevel;
import com.aegisnet.core.model.CrisisEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AUTONOMOUS MONITOR SERVICE — THREE LIVE DATA PIPELINES
 *
 *  Agent 1: Open-Meteo weather    — every 30 s   (real-time meteorological data)
 *  Agent 3: GDELT news            — every 90 s   (global crisis media monitoring)
 *  Agent 6: GDACS (EU JRC)        — every 5 min  (real-time disaster alert feed)
 *
 * Reddit and ReliefWeb removed. Only authoritative, no-auth data sources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutonomousMonitorService {

    private final OpenMeteoService       openMeteoService;
    private final GdeltService           gdeltService;
    private final GdacsService           gdacsService;
    private final CrisisIntelligenceAgent crisisAgent;
    private final SimpMessagingTemplate  messagingTemplate;

    private final Map<String, CityThreatLevel> cityThreats = new ConcurrentHashMap<>();

    private static final List<CityDef> CITIES = List.of(
        new CityDef("Karachi",   24.8607, 67.0011, "Flooding, Heatwaves, Cyclones"),
        new CityDef("Lahore",    31.5204, 74.3587, "Smog, Monsoon Flooding, Heatwaves"),
        new CityDef("Islamabad", 33.6844, 73.0479, "Earthquakes, Landslides, Winter Storms"),
        new CityDef("Peshawar",  34.0151, 71.5249, "Earthquakes, Flash Floods"),
        new CityDef("Quetta",    30.1798, 66.9750, "Earthquakes, Drought, Extreme Cold")
    );

    @PostConstruct
    public void init() {
        for (CityDef c : CITIES)
            cityThreats.put(c.name, new CityThreatLevel(c.name, c.lat, c.lng, c.risk));

        pushTrace("[System] AegisNet Crisis Intelligence — ONLINE");
        pushTrace("[System] Sources: Open-Meteo | GDELT | GDACS (EU Joint Research Centre)");
        pushTrace("[System] Polling: Weather 30s | News 90s | GDACS Disasters 5min");
        log.info("=== AegisNet Autonomous Monitor: READY ===");
    }

    // ─── AGENT 1: WEATHER  ────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 30_000, initialDelay = 3_000)
    public void weatherCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Agent_1: Weather] Fetching live data — 5 cities…", ts));

        for (CityDef c : CITIES) {
            openMeteoService.fetchWeatherForCity(cityThreats.get(c.name));
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }

        // Log any notable readings
        for (CityDef c : CITIES) {
            CityThreatLevel ct = cityThreats.get(c.name);
            double p = ct.getPrecipitationMm();
            double t = ct.getTemperatureC();
            double w = ct.getWindSpeedKmh();
            if (p > 0 || t >= 42 || t <= 1 || w >= 40) {
                pushTrace(String.format("[Agent_1] %s: %.1f°C | %.1fmm rain | %.0fkm/h wind",
                    c.name, t, p, w));
            }
        }

        List<CrisisEvent> events = crisisAgent.analyzeWeatherData(cityThreats);
        broadcastAll(events, ts, "Weather");
    }

    // ─── AGENT 3: GDELT NEWS  ────────────────────────────────────────────────
    @Scheduled(fixedDelay = 90_000, initialDelay = 12_000)
    public void newsCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Agent_3: GDELT] Scanning Pakistan crisis news…", ts));

        GdeltService.GdeltResult result = gdeltService.fetchCrisisNews();

        for (CityDef c : CITIES) {
            cityThreats.get(c.name).setNewsSeverity(result.getSeverityForCity(c.name));
            cityThreats.get(c.name).setActiveNewsSignals(result.getArticleCountForCity(c.name));
        }

        crisisAgent.injectNewsEvents(result);

        if (result.getTraceMessages().isEmpty()) {
            pushTrace(String.format("[%s] [Agent_3: GDELT] No crisis articles this cycle.", ts));
        } else {
            result.getTraceMessages().forEach(m -> pushTrace("[Agent_3: GDELT] " + m));
            pushTrace(String.format("[%s] [Agent_3: GDELT] %d articles processed.", ts, result.getTraceMessages().size()));
        }

        broadcastAll(crisisAgent.getActiveEvents(), ts, "GDELT");
    }

    // ─── AGENT 6: GDACS EU REAL-TIME DISASTERS  ───────────────────────────────
    @Scheduled(fixedDelay = 300_000, initialDelay = 5_000)   // every 5 minutes
    public void gdacsCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Agent_6: GDACS] Scanning EU disaster feed (Pakistan region)…", ts));

        GdacsService.GdacsResult result = gdacsService.fetchDisasters();
        crisisAgent.injectGdacsEvents(result);

        if (result.alerts.isEmpty()) {
            pushTrace(String.format("[%s] [Agent_6: GDACS] No disasters in Pakistan bounding box.", ts));
        } else {
            result.traceMessages.forEach(m -> pushTrace("[Agent_6: GDACS] " + m));
            pushTrace(String.format("[%s] [Agent_6: GDACS] %d disaster alerts ingested.", ts, result.alerts.size()));
        }

        broadcastAll(crisisAgent.getActiveEvents(), ts, "GDACS");
    }

    // ─── HELPERS  ─────────────────────────────────────────────────────────────
    private void broadcastAll(List<CrisisEvent> events, String ts, String agent) {
        // City threats
        List<CityThreatLevel> threats = new ArrayList<>();
        for (CityDef c : CITIES) {
            cityThreats.get(c.name).computeThreatLevel();
            threats.add(cityThreats.get(c.name));
        }
        messagingTemplate.convertAndSend("/topic/city-threats", threats);
        messagingTemplate.convertAndSend("/topic/crisis-events", events);

        long wx  = events.stream().filter(e -> e.getSource().contains("OPEN-METEO")).count();
        long gd  = events.stream().filter(e -> e.getSource().contains("GDELT")).count();
        long gc  = events.stream().filter(e -> e.getSource().contains("GDACS")).count();

        if (!events.isEmpty()) {
            pushTrace(String.format("[%s] [Agent_4: Intel] %d live events │ Weather:%d News:%d GDACS:%d",
                ts, events.size(), wx, gd, gc));
        } else {
            pushTrace(String.format("[%s] [Agent_4: Intel] No anomalies. All conditions nominal.", ts));
        }
    }

    private void pushTrace(String msg) {
        messagingTemplate.convertAndSend("/topic/traces", msg);
    }

    private String ts() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private record CityDef(String name, double lat, double lng, String risk) {}
}
