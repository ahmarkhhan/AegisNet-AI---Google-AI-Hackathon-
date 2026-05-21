package com.aegisnet.core.service;

import com.aegisnet.core.model.CityThreatLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Open-Meteo Integration Service
 * Fetches REAL-TIME weather, air quality, and flood data from Open-Meteo APIs.
 */
@Service
@Slf4j
public class OpenMeteoService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
            "&current=temperature_2m,precipitation,wind_speed_10m,relative_humidity_2m,weather_code" +
            "&timezone=Asia/Karachi";
            
    private static final String AQI_URL =
            "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%s&longitude=%s" +
            "&current=us_aqi,pm10,pm2_5";
            
    private static final String FLOOD_URL =
            "https://flood-api.open-meteo.com/v1/flood?latitude=%s&longitude=%s" +
            "&daily=river_discharge&forecast_days=1";

    public void fetchEnvironmentalDataForCity(CityThreatLevel city) {
        fetchWeather(city);
        fetchAirQuality(city);
        fetchFlood(city);
    }

    private void fetchWeather(CityThreatLevel city) {
        String url = String.format(WEATHER_URL, city.getLatitude(), city.getLongitude());
        try {
            JsonNode root = objectMapper.readTree(restTemplate.getForObject(url, String.class));
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

            int severity = calculateWeatherSeverity(tempC, precipMm, windKmh, humidity);
            city.setWeatherSeverity(severity);
        } catch (Exception e) {
            log.error("[Weather] Failed for {}: {}", city.getCityName(), e.getMessage());
        }
    }

    private void fetchAirQuality(CityThreatLevel city) {
        String url = String.format(AQI_URL, city.getLatitude(), city.getLongitude());
        try {
            JsonNode root = objectMapper.readTree(restTemplate.getForObject(url, String.class));
            JsonNode current = root.path("current");
            
            if (!current.isMissingNode()) {
                int aqi = current.path("us_aqi").asInt();
                double pm10 = current.path("pm10").asDouble();
                double pm25 = current.path("pm2_5").asDouble();
                
                city.setUsAqi(aqi);
                city.setPm10(pm10);
                city.setPm25(pm25);
                
                // US AQI Severity Mapping (0-50 Good, 51-100 Moderate, 101-150 Unhealthy Sensitive, 151-200 Unhealthy, 201-300 Very Unhealthy, 301+ Hazardous)
                int severity = 0;
                if (aqi >= 300) severity = 100;
                else if (aqi >= 200) severity = 80;
                else if (aqi >= 150) severity = 60;
                else if (aqi >= 100) severity = 30;
                
                city.setAqiSeverity(severity);
            }
        } catch (Exception e) {
            log.error("[AQI] Failed for {}: {}", city.getCityName(), e.getMessage());
        }
    }
    
    private void fetchFlood(CityThreatLevel city) {
        String url = String.format(FLOOD_URL, city.getLatitude(), city.getLongitude());
        try {
            JsonNode root = objectMapper.readTree(restTemplate.getForObject(url, String.class));
            JsonNode daily = root.path("daily");
            
            if (!daily.isMissingNode() && daily.path("river_discharge").isArray() && daily.path("river_discharge").size() > 0) {
                double discharge = daily.path("river_discharge").get(0).asDouble();
                city.setRiverDischargeM3s(discharge);
                
                // Very rough estimation. Major rivers in flood can be > 5000 m3/s. 
                int severity = 0;
                if (discharge > 10000) severity = 100;
                else if (discharge > 5000) severity = 80;
                else if (discharge > 2000) severity = 50;
                else if (discharge > 500) severity = 20;
                
                city.setFloodSeverity(severity);
            }
        } catch (Exception e) {
            log.error("[Flood] Failed for {}: {}", city.getCityName(), e.getMessage());
        }
    }

    private int calculateWeatherSeverity(double tempC, double precipMm, double windKmh, int humidity) {
        int score = 0;
        if (tempC <= -10) score += 35;
        else if (tempC <= -5) score += 25;
        else if (tempC <= 0) score += 15;
        if (tempC >= 50) score += 40;
        else if (tempC >= 45) score += 30;
        else if (tempC >= 42) score += 20;
        else if (tempC >= 38) score += 10;
        if (precipMm >= 50) score += 35;
        else if (precipMm >= 25) score += 25;
        else if (precipMm >= 10) score += 15;
        else if (precipMm >= 3) score += 5;
        if (windKmh >= 80) score += 25;
        else if (windKmh >= 50) score += 15;
        else if (windKmh >= 30) score += 10;
        else if (windKmh >= 15) score += 5;
        if (tempC >= 35 && humidity >= 70) score += 10;
        return Math.min(score, 100);
    }
}
