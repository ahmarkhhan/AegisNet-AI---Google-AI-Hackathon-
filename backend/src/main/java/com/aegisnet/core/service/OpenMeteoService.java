package com.aegisnet.core.service;

import com.aegisnet.core.model.CityThreatLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Open-Meteo Integration Service
 * Fetches REAL-TIME weather data from the Open-Meteo API (100% free, no API key).
 * Supports fetching weather for any arbitrary city coordinates.
 */
@Service
@Slf4j
public class OpenMeteoService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
            "&current=temperature_2m,precipitation,wind_speed_10m,relative_humidity_2m,apparent_temperature,weather_code" +
            "&timezone=Asia/Karachi";

    /**
     * Fetches live weather from Open-Meteo for a given city and populates its CityThreatLevel.
     */
    public void fetchWeatherForCity(CityThreatLevel city) {
        String url = String.format(OPEN_METEO_URL, city.getLatitude(), city.getLongitude());

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode current = root.path("current");

            double tempC = current.path("temperature_2m").asDouble();
            double precipMm = current.path("precipitation").asDouble();
            double windKmh = current.path("wind_speed_10m").asDouble();
            int humidity = current.path("relative_humidity_2m").asInt();
            int weatherCode = current.path("weather_code").asInt();

            city.setTemperatureC(tempC);
            city.setPrecipitationMm(precipMm);
            city.setWindSpeedKmh(windKmh);
            city.setHumidityPct(humidity);
            city.setWeatherCode(weatherCode);
            city.setWeatherSummary(String.format("%.1f°C | Rain: %.1fmm | Wind: %.0fkm/h | Humidity: %d%%",
                    tempC, precipMm, windKmh, humidity));

            int severity = calculateWeatherSeverity(tempC, precipMm, windKmh, humidity);
            city.setWeatherSeverity(severity);

            log.debug("[Weather] {} → {}°C, {}mm rain, {}km/h wind → Severity: {}/100",
                    city.getCityName(), tempC, precipMm, windKmh, severity);

        } catch (Exception e) {
            log.error("[Weather] Failed to fetch for {}: {}", city.getCityName(), e.getMessage());
            city.setWeatherSummary("DATA UNAVAILABLE");
            city.setWeatherSeverity(0);
        }
    }

    /**
     * Calculates severity score (0-100) from real weather conditions.
     * Tuned for Pakistan's diverse climate zones.
     */
    private int calculateWeatherSeverity(double tempC, double precipMm, double windKmh, int humidity) {
        int score = 0;

        // Extreme cold (mountain regions)
        if (tempC <= -10) score += 35;
        else if (tempC <= -5) score += 25;
        else if (tempC <= 0) score += 15;

        // Extreme heat (Sindh/Balochistan)
        if (tempC >= 50) score += 40;
        else if (tempC >= 45) score += 30;
        else if (tempC >= 42) score += 20;
        else if (tempC >= 38) score += 10;

        // Heavy precipitation (monsoon flooding)
        if (precipMm >= 50) score += 35;
        else if (precipMm >= 25) score += 25;
        else if (precipMm >= 10) score += 15;
        else if (precipMm >= 3) score += 5;

        // Strong winds
        if (windKmh >= 80) score += 25;
        else if (windKmh >= 50) score += 15;
        else if (windKmh >= 30) score += 10;
        else if (windKmh >= 15) score += 5;

        // High humidity + high temp = heatstroke risk
        if (tempC >= 35 && humidity >= 70) score += 10;

        return Math.min(score, 100);
    }
}
