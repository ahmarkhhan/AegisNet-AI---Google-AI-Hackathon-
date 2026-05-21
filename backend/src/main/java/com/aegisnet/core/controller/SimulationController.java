package com.aegisnet.core.controller;

import com.aegisnet.core.service.MockIngestionService;
import com.aegisnet.core.service.CrisisIntelligenceAgent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Simulation controller for manual demo triggers.
 * The primary data flow is now autonomous via AutonomousMonitorService.
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SimulationController {

    private final MockIngestionService mockIngestionService;
    private final CrisisIntelligenceAgent crisisAgent;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/weather")
    public ResponseEntity<String> triggerWeatherAnomaly() {
        mockIngestionService.simulateMurreeWeatherAnomaly();
        return ResponseEntity.ok("Weather Anomaly Simulated");
    }

    @PostMapping("/social")
    public ResponseEntity<String> triggerSocialPanic() {
        mockIngestionService.simulateSocialPanic();
        return ResponseEntity.ok("Social Panic Simulated");
    }

    @PostMapping("/report")
    public ResponseEntity<String> submitCitizenReport(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String severity = (String) body.get("severity");
        int affectedCount = body.get("affectedCount") != null ? ((Number) body.get("affectedCount")).intValue() : 0;
        double lat = body.get("latitude") != null ? ((Number) body.get("latitude")).doubleValue() : 30.3753;
        double lng = body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : 69.3451;

        crisisAgent.submitCitizenReport(type, title, description, severity, affectedCount, lat, lng);
        
        // Broadcast
        messagingTemplate.convertAndSend("/topic/crisis-events", crisisAgent.getActiveEvents());
        messagingTemplate.convertAndSend("/topic/traces", "📱 [Citizen Feed] Direct citizen alert processed: " + title);
        
        return ResponseEntity.ok("Citizen Report Ingested and Broadcasted");
    }
}
