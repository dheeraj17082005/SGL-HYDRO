package com.sabarmati.cng.dashboard;

import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.journey.entity.ComplianceStatus;
import com.sabarmati.cng.journey.entity.JourneyStatus;
import com.sabarmati.cng.journey.entity.VehicleJourney;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.entity.Vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private FuelingBayRepository fuelingBayRepository;

    @Autowired
    private VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private com.sabarmati.cng.anpr.repository.AnprDetectionRepository anprDetectionRepository;

    @Autowired
    private com.sabarmati.cng.alert.repository.AlertRepository alertRepository;

    @Autowired
    private com.sabarmati.cng.audit.repository.AuditLogRepository auditLogRepository;

    @Autowired
    private com.sabarmati.cng.station.repository.CameraRepository cameraRepository;

    @Autowired
    private com.sabarmati.cng.vehicle.repository.VehicleRepository vehicleRepository;

    private Station station;

    @BeforeEach
    void setUp() {
        vehicleJourneyRepository.deleteAll();
        anprDetectionRepository.deleteAll();
        alertRepository.deleteAll();
        auditLogRepository.deleteAll();
        fuelingBayRepository.deleteAll();
        cameraRepository.deleteAll();
        stationRepository.deleteAll();
        vehicleRepository.deleteAll();

        station = Station.builder()
                .name("SGL Dashboard Station")
                .code("SGL-DASH-01")
                .address("Dashboard Road")
                .active(true)
                .build();
        station = stationRepository.save(station);

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber("GJ01DASH01")
                .vehicleType("AUTO")
                .ownerName("Test Owner")
                .registrationStatus(com.sabarmati.cng.vehicle.entity.RegistrationStatus.VALID)
                .registrationExpiry(java.time.LocalDate.now().plusYears(1))
                .build();
        vehicle = vehicleRepository.save(vehicle);

        FuelingBay bay1 = FuelingBay.builder().station(station).bayNumber(1).status(BayStatus.AVAILABLE).build();
        FuelingBay bay2 = FuelingBay.builder().station(station).bayNumber(2).status(BayStatus.OCCUPIED).build();
        fuelingBayRepository.save(bay1);
        fuelingBayRepository.save(bay2);

        VehicleJourney journey1 = VehicleJourney.builder()
                .station(station)
                .vehicle(vehicle)
                .status(JourneyStatus.IN_QUEUE)
                .complianceStatus(ComplianceStatus.ELIGIBLE)
                .entryTime(LocalDateTime.now().minusMinutes(10))
                .queueEntryTime(LocalDateTime.now().minusMinutes(8))
                .build();
        vehicleJourneyRepository.save(journey1);
    }

    @Test
    void getStationDashboard_returnsFullDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.stationId", is(station.getId().intValue())))
                .andExpect(jsonPath("$.data.vehiclesInQueue", is(1)))
                .andExpect(jsonPath("$.data.availableBays", is(1)))
                .andExpect(jsonPath("$.data.occupiedBays", is(1)));
    }

    @Test
    void getQueueMetrics_returnsQueueData() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/metrics/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentQueueLength", is(1)));
    }

    @Test
    void getThroughputMetrics_returnsThroughputData() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/metrics/throughput"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedJourneysToday", notNullValue()));
    }

    @Test
    void getUtilizationMetrics_returnsUtilizationData() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/metrics/utilization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBays", is(2)))
                .andExpect(jsonPath("$.data.occupiedBays", is(1)))
                .andExpect(jsonPath("$.data.availableBays", is(1)));
    }

    @Test
    void getComplianceMetrics_returnsComplianceData() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/metrics/compliance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligibleVehicles", is(1)));
    }
}
