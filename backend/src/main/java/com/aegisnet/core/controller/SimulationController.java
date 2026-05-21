package com.aegisnet.core.controller;

import com.aegisnet.core.service.MockIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
