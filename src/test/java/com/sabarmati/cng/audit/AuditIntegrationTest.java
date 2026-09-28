package com.sabarmati.cng.audit;

import com.sabarmati.cng.audit.entity.AuditLog;
import com.sabarmati.cng.audit.repository.AuditLogRepository;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.StationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "AUDITOR")
class AuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private com.sabarmati.cng.journey.repository.VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private com.sabarmati.cng.anpr.repository.AnprDetectionRepository anprDetectionRepository;

    @Autowired
    private com.sabarmati.cng.alert.repository.AlertRepository alertRepository;

    @Autowired
    private com.sabarmati.cng.fueling.repository.FuelingBayRepository fuelingBayRepository;

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
                .name("SGL Audit Test Station")
                .code("SGL-AUD-01")
                .address("Audit Road")
                .active(true)
                .build();
        station = stationRepository.save(station);

        AuditLog log1 = AuditLog.builder()
                .stationId(station.getId())
                .action("JOURNEY_CREATED")
                .entityType("VehicleJourney")
                .entityId("101")
                .details("Created vehicle journey")
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log1);

        AuditLog log2 = AuditLog.builder()
                .stationId(station.getId())
                .action("ALERT_RESOLVED")
                .entityType("Alert")
                .entityId("202")
                .details("Resolved alert")
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log2);
    }

    @Test
    void getAuditLogs_returnsAllAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content.length()", is(2)));
    }

    @Test
    void getAuditLogs_withActionFilter_returnsFilteredLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("action", "JOURNEY_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.content[0].action", is("JOURNEY_CREATED")));
    }

    @Test
    void getStationAuditLogs_returnsStationSpecificLogs() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(2)));
    }
}
