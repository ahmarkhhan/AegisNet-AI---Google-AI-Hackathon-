package com.aegisnet.core.service;

import com.aegisnet.core.model.CrisisEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CrisisIntelligenceAgentTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CadDispatcherService cadDispatcherService;

    @InjectMocks
    private CrisisIntelligenceAgent crisisAgent;

    @Test
    public void testSubmitCitizenReport_HighSeverity_TriggersCadDispatch() throws InterruptedException {
        // Arrange
        String type = "FLOOD";
        String title = "Severe water accumulation";
        String description = "Nullah Lai water level rising rapidly, cars trapped near underpass.";
        String severity = "CRITICAL";
        int affectedCount = 500;
        double lat = 33.9070;
        double lng = 73.3943;

        when(cadDispatcherService.dispatchToExternalCad(
                anyString(),
                eq("Rescue 1122 + NDMA Flood Unit"),
                eq("Murree"),
                eq(lat),
                eq(lng),
                anyInt(),
                anyInt()
        )).thenReturn(true);

        // Act
        crisisAgent.submitCitizenReport(type, title, description, severity, affectedCount, lat, lng);

        // Wait up to 25 seconds for the async thread to complete using robust polling
        boolean called = false;
        for (int i = 0; i < 250; i++) {
            try {
                verify(cadDispatcherService, atLeastOnce()).dispatchToExternalCad(
                        anyString(),
                        eq("Rescue 1122 + NDMA Flood Unit"),
                        eq("Murree"),
                        eq(lat),
                        eq(lng),
                        anyInt(),
                        anyInt()
                );
                called = true;
                break;
            } catch (AssertionError e) {
                Thread.sleep(100);
            }
        }
        assertTrue(called, "cadDispatcherService.dispatchToExternalCad should have been called within timeout");

        // Wait an additional 2 seconds to let the final upsertEvent phase complete
        Thread.sleep(2000);

        List<CrisisEvent> activeEvents = crisisAgent.getActiveEvents();
        assertFalse(activeEvents.isEmpty(), "Active events should not be empty");
    }
}
