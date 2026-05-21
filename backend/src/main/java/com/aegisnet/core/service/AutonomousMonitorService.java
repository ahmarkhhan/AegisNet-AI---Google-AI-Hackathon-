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
    private final BlueskyService         blueskyService;
    private final MastodonService        mastodonService;
    private final HdxService             hdxService;
    private final FirmsService           firmsService;
    private final PmdAlertService        pmdService;
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

        pushTrace("[System] Nigehban AI Crisis Intelligence System — ONLINE");
        pushTrace("[System] Sources: Meteo | GDELT | GDACS | Bluesky | Mastodon | HDX | FIRMS | PMD");
        pushTrace("[System] Polling: Weather 30s | Social 60s | News 90s | Advanced 120s | GDACS 5m");
        log.info("=== Nigehban AI Autonomous Monitor: READY ===");
    }

    // ─── AGENT 1: WEATHER  ────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 30_000, initialDelay = 3_000)
    public void weatherCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Agent_1: Weather] Fetching live data — 5 cities…", ts));

        for (CityDef c : CITIES) {
            openMeteoService.fetchEnvironmentalDataForCity(cityThreats.get(c.name));
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }

        for (CityDef c : CITIES) {
            CityThreatLevel ct = cityThreats.get(c.name);
            double p = ct.getPrecipitationMm();
            double t = ct.getTemperatureC();
            double w = ct.getWindSpeedKmh();
            int aqi = ct.getUsAqi();
            double flood = ct.getRiverDischargeM3s();
            
            if (p > 0 || t >= 42 || t <= 1 || w >= 40 || aqi >= 150 || flood >= 1000) {
                pushTrace(String.format("[Agent_1: Environ] %s: %.1f°C | %.1fmm | %d AQI | %.0fm³/s flow",
                    c.name, t, p, aqi, flood));
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

    // ─── AGENT 4: DECENTRALIZED SOCIAL SIGNALS ────────────────────────────────
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void socialSignalsCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Agent_4: Social] Polling Bluesky and Mastodon...", ts));

        var bskyResult = blueskyService.fetchBlueskySignals();
        var mastodonResult = mastodonService.fetchMastodonSignals();

        crisisAgent.injectSocialSignals(bskyResult, mastodonResult);

        bskyResult.getTraceMessages().forEach(m -> pushTrace("[Agent_4: Social] [BSKY] " + m));
        mastodonResult.getTraceMessages().forEach(m -> pushTrace("[Agent_4: Social] [MAST] " + m));
        
        broadcastAll(crisisAgent.getActiveEvents(), ts, "Social");
    }

    // ─── AGENT 5: ADVANCED INTEL (HDX, FIRMS, PMD) ────────────────────────────
    @Scheduled(fixedDelay = 120_000, initialDelay = 20_000)
    public void advancedIntelCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Agent_5: Intel] Polling HDX, NASA FIRMS, and PMD Alerts...", ts));

        var hdxResult = hdxService.fetchHdxSignals();
        var firmsResult = firmsService.fetchFirmsSignals();
        var pmdResult = pmdService.fetchPmdAlerts();

        crisisAgent.injectSocialSignals(hdxResult, firmsResult); // Reusing social injector for generic string matching
        crisisAgent.injectSocialSignals(pmdResult, new com.aegisnet.core.service.SocialSignalResult("stub"));

        hdxResult.getTraceMessages().forEach(m -> pushTrace("[Agent_5: Intel] [HDX] " + m));
        firmsResult.getTraceMessages().forEach(m -> pushTrace("[Agent_5: Intel] [FIRMS] " + m));
        pmdResult.getTraceMessages().forEach(m -> pushTrace("[Agent_5: Intel] [PMD] " + m));
        
        broadcastAll(crisisAgent.getActiveEvents(), ts, "AdvancedIntel");
    }

    // ─── AGENT 6: GDACS EU REAL-TIME DISASTERS  ───────────────────────────────
    @Scheduled(fixedDelay = 300_000, initialDelay = 5_000)
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
