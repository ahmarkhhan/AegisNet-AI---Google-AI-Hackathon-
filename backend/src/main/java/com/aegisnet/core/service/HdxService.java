package com.aegisnet.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Humanitarian Data Exchange (HDX) Service
 * Pulls routine datasets like NDVI (drought) or displacement matrixes.
 * Uses synthetic data to mock complex HAPI authentication for the MVP.
 */
@Service
@Slf4j
public class HdxService {

    public SocialSignalResult fetchHdxSignals() {
        SocialSignalResult result = new SocialSignalResult("HDX_HAPI");

        try {
            // Mocking HDX API Response for Pakistan datasets (NDVI/Displacement)
            List<Map<String, Object>> datasets = List.of(
                    Map.of("city", "Karachi", "indicator", "urban_displacement", "severity", 45, "description", "Minor urban displacement due to infrastructure failure."),
                    Map.of("city", "Lahore", "indicator", "air_quality_displacement", "severity", 65, "description", "Spike in respiratory cases triggering localized medical displacement."),
                    Map.of("city", "Quetta", "indicator", "drought_ndvi", "severity", 80, "description", "Severe NDVI vegetation deficit indicating prolonged drought risk.")
            );

            for (Map<String, Object> data : datasets) {
                String city = (String) data.get("city");
                int severity = (int) data.get("severity");
                String desc = (String) data.get("description");
                
                result.addSignal(city, "HDX Dataset Alert: " + desc, severity);
            }
            log.info("[HDX] Processed humanitarian datasets: {}", result.getCitySeverities());
        } catch (Exception e) {
            log.warn("[HDX] Failed to fetch: {}", e.getMessage());
        }

        return result;
    }
}
