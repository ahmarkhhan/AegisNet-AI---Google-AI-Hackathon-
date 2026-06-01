package com.aegisnet.core.service;

import com.aegisnet.core.model.CrisisEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class AutonomousMonitorServiceTest {

    @Mock private OpenMeteoService openMeteoService;
    @Mock private GdeltService gdeltService;
    @Mock private GdacsService gdacsService;
    @Mock private BlueskyService blueskyService;
    @Mock private MastodonService mastodonService;
    @Mock private HdxService hdxService;
    @Mock private FirmsService firmsService;
    @Mock private PmdAlertService pmdService;
    @Mock private CrisisIntelligenceAgent crisisAgent;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private AutonomousMonitorService service;

    @BeforeEach
    public void setUp() {
        service = new AutonomousMonitorService(
            openMeteoService, gdeltService, gdacsService, blueskyService,
            mastodonService, hdxService, firmsService, pmdService,
            crisisAgent, messagingTemplate
        );
    }

    @Test
    public void testInit() {
        service.init();
        // Verifies messagingTemplate pushed the startup traces via WebSocket!
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/traces"), anyString());
    }

    @Test
    public void testBroadcastDashboardStats() {
        // Arrange
        List<CrisisEvent> activeEvents = new ArrayList<>();
        CrisisEvent event = new CrisisEvent();
        event.setId("EVENT_1");
        event.setType("MASS_ENTRAPMENT_RISK");
        event.setHeading("Murree Snowstorm");
        event.setArea("Murree");
        event.setCriticality("CRITICAL");
        event.setAffectedPopulation(2300);
        event.setDetectedAt(LocalDateTime.now().minusMinutes(15));
        event.setSource("OPEN-METEO");
        activeEvents.add(event);

        when(crisisAgent.getActiveEvents()).thenReturn(activeEvents);
        when(crisisAgent.getDispatchManifests()).thenReturn(new ArrayList<>());
        when(crisisAgent.getSignalsProcessedCount()).thenReturn(34);

        // Act
        service.broadcastDashboardStats();

        // Assert
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard-stats"), any(Object.class));
    }
}
