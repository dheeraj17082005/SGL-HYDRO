package com.sabarmati.cng;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.journey.dto.AssignBayRequest;
import com.sabarmati.cng.journey.dto.CreateJourneyRequest;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.dto.HydroTestCertificateDto;
import com.sabarmati.cng.vehicle.dto.VehicleRequest;
import com.sabarmati.cng.vehicle.entity.HydroTestStatus;
import com.sabarmati.cng.vehicle.entity.RegistrationStatus;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private FuelingBayRepository fuelingBayRepository;

    @Autowired
    private VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Long stationId;
    private Long bay1Id;
    private Long bay2Id;

    @BeforeEach
    void setUp() {
        vehicleJourneyRepository.deleteAll();
        fuelingBayRepository.deleteAll();
        stationRepository.deleteAll();
        vehicleRepository.deleteAll();

        Station station = Station.builder()
                .name("SGL CNG Station Chandkheda")
                .code("SGL-AMD-001")
                .address("Chandkheda, Ahmedabad, Gujarat")
                .latitude(23.1122)
                .longitude(72.5833)
                .active(true)
                .build();
        station = stationRepository.save(station);
        stationId = station.getId();

        FuelingBay bay1 = FuelingBay.builder()
                .station(station)
                .bayNumber(1)
                .status(BayStatus.AVAILABLE)
                .build();
        bay1 = fuelingBayRepository.save(bay1);
        bay1Id = bay1.getId();

        FuelingBay bay2 = FuelingBay.builder()
                .station(station)
                .bayNumber(2)
                .status(BayStatus.OUT_OF_SERVICE)
                .build();
        bay2 = fuelingBayRepository.save(bay2);
        bay2Id = bay2.getId();
    }

    @Test
    void completeVehicleLifecycle_compliantVehicle_success() throws Exception {
        // 1. Register Compliant Vehicle
        VehicleRequest vehicleReq = VehicleRequest.builder()
                .registrationNumber("GJ01AB1234")
                .vehicleType("AUTO")
                .ownerName("Patel Jayesh")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(2))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("HYDRO-SGL-2025-001")
                        .issueDate(LocalDate.now().minusMonths(6))
                        .expiryDate(LocalDate.now().plusYears(1))
                        .status(HydroTestStatus.VALID)
                        .issuingAuthority("PESO Approved Testing Center")
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleReq)))
                .andExpect(status().isCreated());

        // 2. Initialize Journey (ENTERED)
        CreateJourneyRequest journeyReq = CreateJourneyRequest.builder()
                .stationId(stationId)
                .registrationNumber("GJ01AB1234")
                .build();

        String journeyRespStr = mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(journeyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("ENTERED")))
                .andExpect(jsonPath("$.data.complianceStatus", is("ELIGIBLE")))
                .andReturn().getResponse().getContentAsString();

        Long journeyId = objectMapper.readTree(journeyRespStr).get("data").get("id").asLong();

        // Invalid State Transition Check: ENTERED -> FUELING must fail
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/fueling/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));

        // 3. Enter Queue (ENTERED -> IN_QUEUE)
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_QUEUE")))
                .andExpect(jsonPath("$.data.queueEntryTime", notNullValue()));

        // 4. Fetch Station Queue
        mockMvc.perform(get("/api/v1/stations/" + stationId + "/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(journeyId.intValue())));

        // Unavailable Bay Rejection Check (Bay 2 is OUT_OF_SERVICE)
        AssignBayRequest badBayReq = new AssignBayRequest(bay2Id);
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/assign-bay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badBayReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));

        // 5. Assign Bay (IN_QUEUE -> BAY_ASSIGNED)
        AssignBayRequest assignReq = new AssignBayRequest(bay1Id);
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/assign-bay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("BAY_ASSIGNED")))
                .andExpect(jsonPath("$.data.assignedBayId", is(bay1Id.intValue())));

        // 6. Start Fueling (BAY_ASSIGNED -> FUELING)
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/fueling/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("FUELING")))
                .andExpect(jsonPath("$.data.fuelingStartTime", notNullValue()));

        // 7. Complete Fueling & Bay Release (FUELING -> FUELING_COMPLETED)
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/fueling/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("FUELING_COMPLETED")))
                .andExpect(jsonPath("$.data.fuelingEndTime", notNullValue()));

        // Invalid State Transition Check: EXITED / FUELING_COMPLETED -> FUELING must fail
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/fueling/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));

        // 8. Vehicle Exit (FUELING_COMPLETED -> EXITED)
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/exit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("EXITED")))
                .andExpect(jsonPath("$.data.exitTime", notNullValue()));

        // 9. Verify Journey Timeline Endpoint
        mockMvc.perform(get("/api/v1/journeys/" + journeyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("EXITED")))
                .andExpect(jsonPath("$.data.entryTime", notNullValue()))
                .andExpect(jsonPath("$.data.queueEntryTime", notNullValue()))
                .andExpect(jsonPath("$.data.fuelingStartTime", notNullValue()))
                .andExpect(jsonPath("$.data.fuelingEndTime", notNullValue()))
                .andExpect(jsonPath("$.data.exitTime", notNullValue()))
                .andExpect(jsonPath("$.data.events", hasSize(7)));
    }

    @Test
    void nonCompliantVehicle_cannotEnterQueueOrAssignBay() throws Exception {
        // Register Vehicle with EXPIRED Hydro-test
        VehicleRequest expiredReq = VehicleRequest.builder()
                .registrationNumber("GJ02CD5678")
                .vehicleType("CAR")
                .ownerName("Shah Amit")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("HYDRO-EXPIRED-001")
                        .issueDate(LocalDate.now().minusYears(4))
                        .expiryDate(LocalDate.now().minusMonths(1))
                        .status(HydroTestStatus.EXPIRED)
                        .issuingAuthority("Testing Center")
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expiredReq)))
                .andExpect(status().isCreated());

        // Initialize Journey (BLOCKED due to expired hydro test)
        CreateJourneyRequest journeyReq = CreateJourneyRequest.builder()
                .stationId(stationId)
                .registrationNumber("GJ02CD5678")
                .build();

        String journeyRespStr = mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(journeyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("BLOCKED")))
                .andExpect(jsonPath("$.data.complianceStatus", is("NOT_ELIGIBLE")))
                .andReturn().getResponse().getContentAsString();

        Long journeyId = objectMapper.readTree(journeyRespStr).get("data").get("id").asLong();

        // Attempt to Enter Queue -> Must Fail
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/queue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));

        // Attempt to Assign Bay -> Must Fail (BLOCKED -> BAY_ASSIGNED fails)
        AssignBayRequest assignReq = new AssignBayRequest(bay1Id);
        mockMvc.perform(post("/api/v1/journeys/" + journeyId + "/assign-bay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));
    }
}
