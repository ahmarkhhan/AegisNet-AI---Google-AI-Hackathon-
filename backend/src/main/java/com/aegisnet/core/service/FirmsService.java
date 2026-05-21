package com.aegisnet.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * NASA FIRMS (Fire Information for Resource Management System) Service
 * Fetches near-real-time active fire and thermal anomaly data via public CSV endpoints.
 */
@Service
@Slf4j
public class FirmsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FIRMS_SOUTH_ASIA_CSV = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/modis-c6.1/csv/MODIS_C6_1_South_Asia_24h.csv";

    public SocialSignalResult fetchFirmsSignals() {
        SocialSignalResult result = new SocialSignalResult("NASA_FIRMS");

        try {
            String csvData = restTemplate.getForObject(FIRMS_SOUTH_ASIA_CSV, String.class);
            if (csvData == null || csvData.isEmpty()) return result;

            String[] lines = csvData.split("\n");
            
            int pakFireCount = 0;

            for (int i = 1; i < lines.length; i++) {
                String[] cols = lines[i].split(",");
                if (cols.length < 3) continue;

                try {
                    double lat = Double.parseDouble(cols[0]);
                    double lon = Double.parseDouble(cols[1]);

                    // Rough bounding box for Pakistan
                    if (lat >= 23.6 && lat <= 37.0 && lon >= 60.8 && lon <= 77.8) {
                        pakFireCount++;
                        
                        // Map specific coordinates to cities roughly
                        if (lat > 31.0 && lat < 32.0 && lon > 73.0 && lon < 75.0) {
                            result.addSignal("Lahore", "Thermal anomaly detected nearby (FIRMS)", 60);
                        } else if (lat > 33.0 && lat < 34.5 && lon > 72.0 && lon < 74.0) {
                            result.addSignal("Islamabad", "Thermal anomaly detected nearby (FIRMS)", 60);
                        } else if (lat > 24.0 && lat < 26.0 && lon > 66.0 && lon < 68.0) {
                            result.addSignal("Karachi", "Thermal anomaly detected nearby (FIRMS)", 60);
                        } else if (lat > 29.0 && lat < 31.0 && lon > 66.0 && lon < 68.0) {
                            result.addSignal("Quetta", "Thermal anomaly detected nearby (FIRMS)", 60);
                        } else if (lat > 33.5 && lat < 34.5 && lon > 71.0 && lon < 72.0) {
                            result.addSignal("Peshawar", "Thermal anomaly detected nearby (FIRMS)", 60);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (pakFireCount > 0) {
                log.info("[FIRMS] Detected {} thermal anomalies in Pakistan region.", pakFireCount);
            }
        } catch (Exception e) {
            log.warn("[FIRMS] Failed to fetch data: {}", e.getMessage());
        }

        return result;
    }
}
