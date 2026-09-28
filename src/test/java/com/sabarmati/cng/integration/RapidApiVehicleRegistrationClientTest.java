package com.sabarmati.cng.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RapidApiVehicleRegistrationClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private RapidApiVehicleRegistrationClient client;

    private static final String API_KEY = "test_key_12345";
    private static final String API_HOST = "vehicle-rc-information.p.rapidapi.com";
    private static final String API_URL = "https://vehicle-rc-information.p.rapidapi.com/";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new RapidApiVehicleRegistrationClient(API_KEY, API_HOST, API_URL, restTemplate);
    }

    @Test
    void verifyRegistration_validVehicleMatch_returnsFoundAndValid() {
        String jsonResponse = """
                {
                  "success": true,
                  "data": {
                    "registrationNo": "MH12DE1433",
                    "ownerName": "TEST OWNER",
                    "vehicleClass": "Motor Car(LMV)",
                    "fuelType": "PETROL",
                    "fitnessUpto": "27-Feb-2030",
                    "rcStatus": "ACTIVE"
                  }
                }
                """;

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-rapidapi-key", API_KEY))
                .andExpect(header("x-rapidapi-host", API_HOST))
                .andExpect(content().json("{\"registrationNo\":\"MH12DE1433\"}"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        VehicleRegistrationResult result = client.verifyRegistration("MH12DE1433");

        assertTrue(result.isFound());
        assertTrue(result.isValid());
        assertEquals("TEST OWNER", result.getOwnerName());
        assertEquals("Motor Car(LMV)", result.getVehicleType());
        assertEquals(LocalDate.of(2030, 2, 27), result.getRegistrationExpiry());
        assertEquals("RAPIDAPI", client.getProviderName());

        mockServer.verify();
    }

    @Test
    void verifyRegistration_unregisteredVehicle_returnsNotFound() {
        String jsonResponse = """
                {
                  "success": false,
                  "error": "Vahan with registrationNo GJ01AB1234 not found"
                }
                """;

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        VehicleRegistrationResult result = client.verifyRegistration("GJ01AB1234");

        assertFalse(result.isFound());
        assertFalse(result.isValid());

        mockServer.verify();
    }

    @Test
    void verifyRegistration_http404NotFound_returnsNotFound() {
        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        VehicleRegistrationResult result = client.verifyRegistration("GJ01UNKNOWN");

        assertFalse(result.isFound());
        assertFalse(result.isValid());

        mockServer.verify();
    }

    @Test
    void verifyRegistration_rateLimitExceeded429_returnsSafeUnverified() {
        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        VehicleRegistrationResult result = client.verifyRegistration("MH12DE1433");

        assertFalse(result.isFound());
        assertFalse(result.isValid());

        mockServer.verify();
    }

    @Test
    void verifyRegistration_authFailure401_returnsSafeUnverified() {
        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        VehicleRegistrationResult result = client.verifyRegistration("MH12DE1433");

        assertFalse(result.isFound());
        assertFalse(result.isValid());

        mockServer.verify();
    }
}
