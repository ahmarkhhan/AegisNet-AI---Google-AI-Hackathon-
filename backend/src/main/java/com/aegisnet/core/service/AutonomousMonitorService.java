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

        pushTrace("[Antigravity Workspace] Nigehban AI Agent Swarm — ONLINE");
        pushTrace("[Antigravity Workspace] Loading ReAct Agent System Prompts...");
        pushTrace("[Antigravity Workspace] Agents Online: Meteorologist | SocialVerifier | NewsIntel | GDACSAgent | Coordinator");
        log.info("=== Nigehban AI Antigravity Monitor: READY ===");
    }

    // ─── AGENT 1: WEATHER  ────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 300_000, initialDelay = 3_000)
    public void weatherCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Antigravity Agent: Meteorologist] Reasoning: Routine check for environmental anomalies in 5 cities.", ts));
        pushTrace(String.format("[%s] [Antigravity Agent: Meteorologist] Action: Tool_Call_OpenMeteoAPI()", ts));

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
    @Scheduled(fixedDelay = 300_000, initialDelay = 12_000)
    public void newsCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Antigravity Agent: NewsIntel] Reasoning: Scanning global media for Pakistan crisis context.", ts));
        pushTrace(String.format("[%s] [Antigravity Agent: NewsIntel] Action: Tool_Call_GDELT_Project()", ts));

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
    @Scheduled(fixedDelay = 300_000, initialDelay = 15_000)
    public void socialSignalsCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Antigravity Agent: SocialVerifier] Reasoning: Checking decentralized networks for panic signals.", ts));
        pushTrace(String.format("[%s] [Antigravity Agent: SocialVerifier] Action: Tool_Call_BlueskyMastodon_API()", ts));

        var bskyResult = blueskyService.fetchBlueskySignals();
        var mastodonResult = mastodonService.fetchMastodonSignals();

        crisisAgent.injectSocialSignals(bskyResult, mastodonResult);

        bskyResult.getTraceMessages().forEach(m -> pushTrace("[Agent_4: Social] [BSKY] " + m));
        mastodonResult.getTraceMessages().forEach(m -> pushTrace("[Agent_4: Social] [MAST] " + m));
        
        broadcastAll(crisisAgent.getActiveEvents(), ts, "Social");
    }

    // ─── AGENT 5: ADVANCED INTEL (HDX, FIRMS, PMD) ────────────────────────────
    @Scheduled(fixedDelay = 600_000, initialDelay = 20_000)
    public void advancedIntelCycle() {
        String ts = ts();
        pushTrace(String.format("[%s] [Antigravity Agent: Coordinator] Reasoning: Correlating advanced intelligence (FIRMS/PMD/HDX).", ts));
        pushTrace(String.format("[%s] [Antigravity Agent: Coordinator] Action: Multi_Tool_Invocation(NASA_FIRMS, PMD_Alerts, HDX)", ts));

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
        pushTrace(String.format("[%s] [Antigravity Agent: GDACSAgent] Reasoning: Scanning EU disaster feed for Pakistan bounding box.", ts));
        pushTrace(String.format("[%s] [Antigravity Agent: GDACSAgent] Action: Tool_Call_GDACS_RSS()", ts));

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
        messagingTemplate.convertAndSend("/topic/city-threats", java.util.Objects.requireNonNull(threats));
        messagingTemplate.convertAndSend("/topic/crisis-events", java.util.Objects.requireNonNull(events));

        long wx  = events.stream().filter(e -> e.getSource().contains("OPEN-METEO")).count();
        long gd  = events.stream().filter(e -> e.getSource().contains("GDELT")).count();
        long gc  = events.stream().filter(e -> e.getSource().contains("GDACS")).count();
        log.debug("Event breakdown - OpenMeteo: {}, GDELT: {}, GDACS: {}", wx, gd, gc);

        if (!events.isEmpty()) {
            pushTrace(String.format("[%s] [Antigravity Coordinator] %d live events verified. Action: Routing to EOC Tactical Dashboard.", ts, events.size()));
        } else {
            pushTrace(String.format("[%s] [Antigravity Coordinator] Reasoning: No anomalous patterns detected. Sleeping.", ts));
        }
    }

    // ─── DASHBOARD STATS BROADCAST  ──────────────────────────────────────────
    @Scheduled(fixedDelay = 15_000, initialDelay = 8_000)
    public void broadcastDashboardStats() {
        var events = crisisAgent.getActiveEvents();
        int totalEvents = events.size();
        long totalAffected = events.stream().mapToLong(CrisisEvent::getAffectedPopulation).sum();
        int dispatches = crisisAgent.getDispatchManifests().size();
        int signalsProcessed = crisisAgent.getSignalsProcessedCount();

        // Compute dynamic response time from active events
        double avgResponseMin = events.isEmpty() ? 0 : events.stream()
            .filter(e -> e.getDetectedAt() != null)
            .mapToDouble(e -> java.time.Duration.between(e.getDetectedAt(), java.time.LocalDateTime.now()).toSeconds() / 60.0)
            .average().orElse(0);

        // System readiness: 100% minus penalty for high-severity events
        long criticalCount = events.stream().filter(e -> "CRITICAL".equals(e.getCriticality())).count();
        double readiness = Math.max(75.0, 100.0 - (criticalCount * 5.0) - (totalEvents * 1.5));

        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("activeAlerts", totalEvents);
        stats.put("signalsProcessed", signalsProcessed);
        stats.put("avgResponseTimeMinutes", Math.round(avgResponseMin * 10.0) / 10.0);
        stats.put("systemReadinessPercent", Math.round(readiness * 10.0) / 10.0);
        stats.put("totalAffectedPopulation", totalAffected);
        stats.put("activeAgents", 6);
        stats.put("dispatchesExecuted", dispatches);
        stats.put("totalCrisisEvents", totalEvents);

        messagingTemplate.convertAndSend("/topic/dashboard-stats", stats);
    }

    private void pushTrace(String msg) {
        messagingTemplate.convertAndSend("/topic/traces", java.util.Objects.requireNonNull(msg));
    }

    private String ts() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private record CityDef(String name, double lat, double lng, String risk) {}
}

