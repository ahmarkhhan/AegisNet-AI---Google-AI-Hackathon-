package com.aegisnet.core.ai;

import com.aegisnet.core.model.EventSignal;
import com.aegisnet.core.model.CrisisAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Map;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class AgentService {

    @Autowired
    private GeminiReasoningService geminiService;

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
        log.info("[Agent_4: Gemini 2.0 Escalation Predictor] Analyzing context: {}", correlatedContext);
        
        // Pass to Gemini flash
        Map<String, Object> assessment = geminiService.predictEscalation(
            "MULTIPLE_SIGNALS", 75, "Unknown Zone", 100000, true, "Unknown", Collections.singletonList(correlatedContext)
        );

        int prob = ((Number) assessment.getOrDefault("escalationProbability", 95)).intValue();
        String verdict = (String) assessment.getOrDefault("verdict", "IMMEDIATE_DISPATCH");
        String reasoning = (String) assessment.getOrDefault("reasoning", "High probability of mass entanglement.");

        CrisisAlert alert = new CrisisAlert();
        alert.setType("MASS_ENTRAPMENT_RISK");
        alert.setTitle("Dynamic Escalation Prediction: " + verdict);
        alert.setDescription(reasoning + " Context: " + correlatedContext);
        alert.setEpicenterLat(33.9070);
        alert.setEpicenterLng(73.3943);
        alert.setImpactRadiusKm(15.0);
        alert.setCasualtyRiskScore(85);
        alert.setEscalationProbability(prob);
        alert.setPredictedEscalationTime(LocalDateTime.now().plusHours(2));
        alert.setRecommendedPreventiveActions(List.of("Dispatch Drones", "Deploy Rescue 1122"));
        alert.setStatus("PREDICTED");
        alert.setCreatedAt(LocalDateTime.now());
        
        return alert;
    }
}
