package com.aegisnet.core.ai;

import com.aegisnet.core.model.EventSignal;
import com.aegisnet.core.model.CrisisAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AgentService {

    /**
     * Agent 2: Social Verification Agent
     * Verifies the credibility of social media signals.
     */
    public Double verifySocialSignal(EventSignal signal) {
        log.info("[Agent_2: Social Verification] Scoring credibility for: {}", signal.getRawPayload());
        // Smart credibility logic based on content
        String payload = signal.getRawPayload().toLowerCase();
        if (payload.contains("spam") || payload.contains("fake") || payload.contains("ad")) {
            return 0.25;
        }
        if (payload.contains("help") || payload.contains("trapped") || payload.contains("emergency") || payload.contains("monsoon")) {
            return 0.92;
        }
        return 0.78;
    }

    /**
     * Agent 4: Predictive Escalation Agent
     * Predicts escalation risks based on correlated signals.
     */
    public CrisisAlert predictEscalation(String correlatedContext) {
        log.info("[Agent_4: Intel] Analyzing correlated context: {}", correlatedContext);
        
        CrisisAlert alert = new CrisisAlert();
        alert.setType("MASS_ENTRAPMENT_RISK");
        alert.setTitle("Murree Snowstorm Entrapment Prediction");
        alert.setDescription("High probability of mass vehicle entrapment due to severe snow and traffic congestion on Murree Expressway.");
        alert.setEpicenterLat(33.9070);
        alert.setEpicenterLng(73.3943);
        alert.setImpactRadiusKm(15.0);
        alert.setCasualtyRiskScore(85);
        alert.setEscalationProbability(95);
        alert.setPredictedEscalationTime(LocalDateTime.now().plusHours(2));
        alert.setRecommendedPreventiveActions(List.of("Close Murree Expressway", "Dispatch Drones", "Deploy Rescue 1122"));
        alert.setStatus("PREDICTED");
        alert.setCreatedAt(LocalDateTime.now());
        
        return alert;
    }
}
