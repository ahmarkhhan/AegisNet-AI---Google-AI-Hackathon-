package com.aegisnet.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

/**
 * Pakistan Meteorological Department (PMD) Alert Service
 * Monitors the official PMD CAP (Common Alerting Protocol) RSS feed for official advisories.
 */
@Service
@Slf4j
public class PmdAlertService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PMD_RSS_URL = "https://cap-sources.s3.amazonaws.com/pk-pmd-en/rss.xml";

    public SocialSignalResult fetchPmdAlerts() {
        SocialSignalResult result = new SocialSignalResult("PMD_Alerts");

        try {
            String xmlData = restTemplate.getForObject(PMD_RSS_URL, String.class);
            if (xmlData == null || xmlData.isEmpty()) return result;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlData.getBytes()));
            
            NodeList items = document.getElementsByTagName("item");

            // Check for actual alerts
            for (int i = 0; i < items.getLength(); i++) {
                String title = items.item(i).getChildNodes().item(1).getTextContent();
                String desc = items.item(i).getChildNodes().item(3).getTextContent();
                
                int severity = calculateSeverity(title + " " + desc);
                
                if (desc.toLowerCase().contains("karachi") || desc.toLowerCase().contains("sindh")) {
                    result.addSignal("Karachi", "PMD Advisory: " + title, severity);
                } else if (desc.toLowerCase().contains("lahore") || desc.toLowerCase().contains("punjab")) {
                    result.addSignal("Lahore", "PMD Advisory: " + title, severity);
                } else if (desc.toLowerCase().contains("islamabad") || desc.toLowerCase().contains("rawalpindi")) {
                    result.addSignal("Islamabad", "PMD Advisory: " + title, severity);
                } else if (desc.toLowerCase().contains("peshawar") || desc.toLowerCase().contains("kpk")) {
                    result.addSignal("Peshawar", "PMD Advisory: " + title, severity);
                } else if (desc.toLowerCase().contains("quetta") || desc.toLowerCase().contains("balochistan")) {
                    result.addSignal("Quetta", "PMD Advisory: " + title, severity);
                } else {
                    result.addGeneralSignal("PMD National Advisory: " + title, severity);
                }
            }
            
            log.info("[PMD] Processed official advisories: {}", result.getCitySeverities());
        } catch (Exception e) {
            log.warn("[PMD] Failed to fetch alerts: {}", e.getMessage());
        }

        return result;
    }
    
    private int calculateSeverity(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("red") || lower.contains("severe") || lower.contains("cyclone")) return 85;
        if (lower.contains("orange") || lower.contains("heavy") || lower.contains("flood")) return 65;
        if (lower.contains("yellow") || lower.contains("advisory") || lower.contains("heat")) return 45;
        return 30;
    }
}
