package com.sabarmati.cng.anpr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarmati.cng.anpr.dto.AnprDetectionRequest;
import com.sabarmati.cng.anpr.repository.AnprDetectionRepository;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Camera;
import com.sabarmati.cng.station.entity.CameraType;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.CameraRepository;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class AnprIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private FuelingBayRepository fuelingBayRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private AnprDetectionRepository anprDetectionRepository;

    private Station station;
    private Camera camera;
    private Station otherStation;

    @BeforeEach
    void setUp() {
        vehicleJourneyRepository.deleteAll();
        anprDetectionRepository.deleteAll();
        fuelingBayRepository.deleteAll();
        cameraRepository.deleteAll();
        stationRepository.deleteAll();
        vehicleRepository.deleteAll();

        station = Station.builder()
                .name("SGL CNG Station SG Highway")
                .code("SGL-AMD-002")
                .address("SG Highway, Ahmedabad")
                .latitude(23.0225)
                .longitude(72.5714)
                .active(true)
                .build();
        station = stationRepository.save(station);

        camera = Camera.builder()
                .station(station)
                .cameraType(CameraType.ANPR)
                .cameraIdentifier("CAM-ENTRY-01")
                .active(true)
                .build();
        camera = cameraRepository.save(camera);

        otherStation = Station.builder()
                .name("SGL CNG Station Gandhinagar")
                .code("SGL-GND-001")
                .address("Sector 11, Gandhinagar")
                .active(true)
                .build();
        otherStation = stationRepository.save(otherStation);
    }

    // 1. Valid ANPR detection & 6. ANPR detection creates journey & 7. Compliance-approved journey
    @Test
    void test1_validAnprDetection_createsComplianceApprovedJourney() throws Exception {
        AnprDetectionRequest request = AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01ANPR123")
                .detectedAt(LocalDateTime.now())
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.complianceStatus", is("ELIGIBLE")))
                .andExpect(jsonPath("$.data.journeyStatus", is("ENTERED")))
                .andExpect(jsonPath("$.data.message", is("Vehicle is eligible for fueling")))
                .andReturn().getResponse().getContentAsString();

        Long journeyId = objectMapper.readTree(responseStr).get("data").get("journeyId").asLong();

        // Check events on created journey (ENTRY_DETECTED, REGISTRATION_VERIFIED, HYDRO_TEST_VERIFIED, COMPLIANCE_APPROVED)
        mockMvc.perform(get("/api/v1/journeys/" + journeyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.complianceStatus", is("ELIGIBLE")))
                .andExpect(jsonPath("$.data.events[0].eventType", is("ENTRY_DETECTED")))
                .andExpect(jsonPath("$.data.events[1].eventType", is("REGISTRATION_VERIFIED")))
                .andExpect(jsonPath("$.data.events[2].eventType", is("HYDRO_TEST_VERIFIED")))
                .andExpect(jsonPath("$.data.events[3].eventType", is("COMPLIANCE_APPROVED")));

        assertEquals(1, anprDetectionRepository.findAll().size());
    }

    // 2. Unknown vehicle
    @Test
    void test2_unknownVehicle_blockedAndRejected() throws Exception {
        AnprDetectionRequest request = AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01UNREG9999")
                .detectedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.complianceStatus", is("NOT_ELIGIBLE")))
                .andExpect(jsonPath("$.data.journeyStatus", is("BLOCKED")));
    }

    // 3. Invalid registration
    @Test
    void test3_invalidRegistration_blockedAndRejected() throws Exception {
        AnprDetectionRequest request = AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01INV")
                .detectedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.complianceStatus", is("NOT_ELIGIBLE")))
                .andExpect(jsonPath("$.data.journeyStatus", is("BLOCKED")));
    }

    // 4. Expired hydro-test & 8. Compliance-rejected journey
    @Test
    void test4_expiredHydroTest_blockedAndRejected() throws Exception {
        AnprDetectionRequest request = AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01EXPHYDRO")
                .detectedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.complianceStatus", is("NOT_ELIGIBLE")))
                .andExpect(jsonPath("$.data.journeyStatus", is("BLOCKED")));
    }

    // 5. Missing hydro-test
    @Test
    void test5_missingHydroTest_blockedAndRejected() throws Exception {
        AnprDetectionRequest request = AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01NOHYDRO")
                .detectedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.complianceStatus", is("NOT_ELIGIBLE")))
                .andExpect(jsonPath("$.data.journeyStatus", is("BLOCKED")));
    }

    // 9. Duplicate ANPR detection returns existing active journey
    @Test
    void test9_duplicateAnprDetection_returnsExistingActiveJourney() throws Exception {
        AnprDetectionRequest request = AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01DUP100")
                .detectedAt(LocalDateTime.now())
                .build();

        // First detection
        String resp1 = mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.journeyStatus", is("ENTERED")))
                .andReturn().getResponse().getContentAsString();

        Long journeyId1 = objectMapper.readTree(resp1).get("data").get("journeyId").asLong();

        // Second detection of same vehicle at same station (duplicate read)
        String resp2 = mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message", containsString("Duplicate ANPR detection")))
                .andReturn().getResponse().getContentAsString();

        Long journeyId2 = objectMapper.readTree(resp2).get("data").get("journeyId").asLong();

        assertEquals(journeyId1, journeyId2);
        assertEquals(1, vehicleJourneyRepository.findAll().size());
        assertEquals(2, anprDetectionRepository.findAll().size());
    }

    // 10. Invalid station / camera
    @Test
    void test10_invalidStationOrCamera_returnsError() throws Exception {
        // Invalid station
        AnprDetectionRequest badStationReq = AnprDetectionRequest.builder()
                .stationId(9999L)
                .cameraId(camera.getId())
                .registrationNumber("GJ01AB1234")
                .build();

        mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badStationReq)))
                .andExpect(status().isNotFound());

        // Camera does not belong to station
        AnprDetectionRequest mismatchReq = AnprDetectionRequest.builder()
                .stationId(otherStation.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01AB1234")
                .build();

        mockMvc.perform(post("/api/v1/anpr/detections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));
    }
}
