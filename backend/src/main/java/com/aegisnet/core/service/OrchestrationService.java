package com.aegisnet.core.service;

import com.aegisnet.core.ai.AgentService;
import com.aegisnet.core.model.CrisisAlert;
import com.aegisnet.core.model.EventSignal;
import com.aegisnet.core.repository.CrisisAlertRepository;
import com.aegisnet.core.repository.EventSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class OrchestrationService {

    private final EventSignalRepository eventSignalRepository;
    private final CrisisAlertRepository crisisAlertRepository;
    private final AgentService agentService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 10000) // Run every 10 seconds
    public void processUnprocessedSignals() {
        List<EventSignal> unprocessed = eventSignalRepository.findByProcessedFalse();
        if (unprocessed.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed signals", unprocessed.size());

        for (EventSignal signal : unprocessed) {
            // 1. Social Verification (Agent 2)
            if ("SOCIAL_X".equals(signal.getSource()) || "SOCIAL_PANIC".equals(signal.getType())) {
                Double credibility = agentService.verifySocialSignal(signal);
                signal.setConfidence(credibility);
                log.info("Signal {} credibility scored: {}", signal.getId(), credibility);
            } else {
                signal.setConfidence(1.0); // Gov/Weather APIs are trusted
            }

            signal.setProcessed(true);
            eventSignalRepository.save(signal);
            
            // Push updated state to frontend
            messagingTemplate.convertAndSend("/topic/signals/processed", signal);
        }

        // 2. Crisis Correlation & Escalation Prediction (Agent 4)
        // If we have multiple high-severity signals in recent timeframe, trigger prediction
        if (unprocessed.size() >= 2) {
            log.info("Multiple signals detected. Triggering Predictive Escalation Agent.");
            String context = "Multiple severe signals received in Murree region: Heavy snow (SUPARCO) and Social Panic regarding traffic entrapment.";
            
            CrisisAlert alert = agentService.predictEscalation(context);
            crisisAlertRepository.save(alert);
            
            // Dispatch alerts
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            log.warn("CRITICAL ALERT GENERATED: {}", alert.getTitle());
            
            // 3. Trigger Drone Coordination (Agent 5) - Simulation
            triggerDroneDispatch(alert);
        }
    }
    
    private void triggerDroneDispatch(CrisisAlert alert) {
        log.info("Dispatching Autonomous Drones to epicenter {}, {}", alert.getEpicenterLat(), alert.getEpicenterLng());
        // We'd call DroneCoordinationAgent here
        messagingTemplate.convertAndSend("/topic/drones/dispatch", "Drone dispatched to " + alert.getTitle());
    }
}
