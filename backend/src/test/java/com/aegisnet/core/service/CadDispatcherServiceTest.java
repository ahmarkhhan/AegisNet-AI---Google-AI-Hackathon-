package com.aegisnet.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SuppressWarnings("null")
public class CadDispatcherServiceTest {

    private CadDispatcherService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() throws Exception {
        service = new CadDispatcherService();
        service.setPrimaryCadUrl("http://mock-cad/dispatch");
        service.setFallbackWebhookUrl("http://mock-fallback/webhook");

        // Inject RestTemplate from CadDispatcherService to set up MockRestServiceServer
        Field restTemplateField = CadDispatcherService.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) restTemplateField.get(service);

        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testPrimaryCadSuccess() {
        // Arrange
        mockServer.expect(requestTo("http://mock-cad/dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"status\":\"DISPATCHED\"}", MediaType.APPLICATION_JSON));

        // Act
        boolean result = service.dispatchToExternalCad(
                "CITIZEN-12345",
                "Rescue 1122",
                "Murree",
                33.9070,
                73.3943,
                3,
                75
        );

        // Assert
        assertTrue(result);
        mockServer.verify();
    }

    @Test
    public void testPrimaryCadFails_FallbackSuccess() {
        // Arrange
        // Primary returns 500 error
        mockServer.expect(requestTo("http://mock-cad/dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // Fallback returns 200 success
        mockServer.expect(requestTo("http://mock-fallback/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"status\":\"FALLBACK_OK\"}", MediaType.APPLICATION_JSON));

        // Act
        boolean result = service.dispatchToExternalCad(
                "CITIZEN-12345",
                "Rescue 1122",
                "Murree",
                33.9070,
                73.3943,
                3,
                75
        );

        // Assert
        assertTrue(result);
        mockServer.verify();
    }

    @Test
    public void testBothEndpointsFail() {
        // Arrange
        // Primary returns 500 error
        mockServer.expect(requestTo("http://mock-cad/dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // Fallback returns 500 error
        mockServer.expect(requestTo("http://mock-fallback/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // Act
        boolean result = service.dispatchToExternalCad(
                "CITIZEN-12345",
                "Rescue 1122",
                "Murree",
                33.9070,
                73.3943,
                3,
                75
        );

        // Assert
        assertFalse(result);
        mockServer.verify();
    }
}
