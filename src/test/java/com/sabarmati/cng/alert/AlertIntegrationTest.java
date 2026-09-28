package com.sabarmati.cng.alert;

import com.sabarmati.cng.alert.entity.Alert;
import com.sabarmati.cng.alert.entity.AlertSeverity;
import com.sabarmati.cng.alert.entity.AlertStatus;
import com.sabarmati.cng.alert.entity.AlertType;
import com.sabarmati.cng.alert.repository.AlertRepository;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "manager", roles = "STATION_MANAGER")
class AlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.sabarmati.cng.journey.repository.VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private com.sabarmati.cng.anpr.repository.AnprDetectionRepository anprDetectionRepository;

    @Autowired
    private com.sabarmati.cng.fueling.repository.FuelingBayRepository fuelingBayRepository;

    @Autowired
    private com.sabarmati.cng.station.repository.CameraRepository cameraRepository;

    @Autowired
    private com.sabarmati.cng.vehicle.repository.VehicleRepository vehicleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private StationRepository stationRepository;

    private Station station;
    private Alert alert;

    @BeforeEach
    void setUp() {
        vehicleJourneyRepository.deleteAll();
        anprDetectionRepository.deleteAll();
        alertRepository.deleteAll();
        fuelingBayRepository.deleteAll();
        cameraRepository.deleteAll();
        stationRepository.deleteAll();
        vehicleRepository.deleteAll();

        station = Station.builder()
                .name("SGL Alert Test Station")
                .code("SGL-ALT-01")
                .address("Alert Road")
                .active(true)
                .build();
        station = stationRepository.save(station);

        alert = Alert.builder()
                .stationId(station.getId())
                .type(AlertType.EXPIRED_HYDRO_TEST)
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .message("Expired hydro test detected")
                .createdAt(LocalDateTime.now())
                .build();
        alert = alertRepository.save(alert);
    }

    @Test
    void getAllAlerts_returnsPagedAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.content[0].message", is("Expired hydro test detected")));
    }

    @Test
    void getAlertsByStation_returnsStationAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/stations/" + station.getId() + "/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)));
    }

    @Test
    void getAlertById_returnsAlertDetails() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/" + alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(alert.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("OPEN")));
    }

    @Test
    void resolveAlert_updatesAlertStatusToResolved() throws Exception {
        mockMvc.perform(post("/api/v1/alerts/" + alert.getId() + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.resolvedAt", notNullValue()));
    }
}
