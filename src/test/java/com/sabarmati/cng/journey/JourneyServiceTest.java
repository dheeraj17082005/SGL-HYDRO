package com.sabarmati.cng.journey;

import com.sabarmati.cng.common.exception.InvalidStateTransitionException;
import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.journey.dto.AssignBayRequest;
import com.sabarmati.cng.journey.dto.CreateJourneyRequest;
import com.sabarmati.cng.journey.dto.VehicleJourneyResponse;
import com.sabarmati.cng.journey.entity.ComplianceStatus;
import com.sabarmati.cng.journey.entity.JourneyStatus;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.journey.service.JourneyService;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.dto.HydroTestCertificateDto;
import com.sabarmati.cng.vehicle.dto.VehicleRequest;
import com.sabarmati.cng.vehicle.entity.HydroTestStatus;
import com.sabarmati.cng.vehicle.entity.RegistrationStatus;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import com.sabarmati.cng.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JourneyServiceTest {

    @Autowired
    private JourneyService journeyService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private FuelingBayRepository fuelingBayRepository;

    @Autowired
    private VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Station station;
    private FuelingBay availableBay;
    private FuelingBay outOfServiceBay;

    @BeforeEach
    void setUp() {
        vehicleJourneyRepository.deleteAll();
        fuelingBayRepository.deleteAll();
        stationRepository.deleteAll();
        vehicleRepository.deleteAll();

        station = Station.builder()
                .name("Test Station Alpha")
                .code("ALPHA-01")
                .address("Ahmedabad")
                .build();
        station = stationRepository.save(station);

        availableBay = FuelingBay.builder()
                .station(station)
                .bayNumber(1)
                .status(BayStatus.AVAILABLE)
                .build();
        availableBay = fuelingBayRepository.save(availableBay);

        outOfServiceBay = FuelingBay.builder()
                .station(station)
                .bayNumber(2)
                .status(BayStatus.OUT_OF_SERVICE)
                .build();
        outOfServiceBay = fuelingBayRepository.save(outOfServiceBay);
    }

    @Test
    void enterQueue_eligibleVehicle_success() {
        // Register compliant vehicle
        vehicleService.createVehicle(VehicleRequest.builder()
                .registrationNumber("GJ01EV0001")
                .vehicleType("AUTO")
                .ownerName("Test Owner")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("CERT-001")
                        .issueDate(LocalDate.now().minusMonths(1))
                        .expiryDate(LocalDate.now().plusYears(1))
                        .status(HydroTestStatus.VALID)
                        .issuingAuthority("PESO")
                        .build())
                .build());

        // Create Journey
        VehicleJourneyResponse initResp = journeyService.createJourney(CreateJourneyRequest.builder()
                .stationId(station.getId())
                .registrationNumber("GJ01EV0001")
                .build());

        assertEquals(JourneyStatus.ENTERED, initResp.getStatus());
        assertEquals(ComplianceStatus.ELIGIBLE, initResp.getComplianceStatus());

        // Enter Queue
        VehicleJourneyResponse queueResp = journeyService.enterQueue(initResp.getId());

        assertEquals(JourneyStatus.IN_QUEUE, queueResp.getStatus());
        assertNotNull(queueResp.getQueueEntryTime());
    }

    @Test
    void enterQueue_nonCompliantVehicle_throwsException() {
        // Register vehicle with EXPIRED hydro test
        vehicleService.createVehicle(VehicleRequest.builder()
                .registrationNumber("GJ01EXP999")
                .vehicleType("CAR")
                .ownerName("Test Owner 2")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("CERT-EXP")
                        .issueDate(LocalDate.now().minusYears(3))
                        .expiryDate(LocalDate.now().minusDays(1))
                        .status(HydroTestStatus.EXPIRED)
                        .issuingAuthority("PESO")
                        .build())
                .build());

        VehicleJourneyResponse initResp = journeyService.createJourney(CreateJourneyRequest.builder()
                .stationId(station.getId())
                .registrationNumber("GJ01EXP999")
                .build());

        assertEquals(JourneyStatus.BLOCKED, initResp.getStatus());

        assertThrows(InvalidStateTransitionException.class, () -> journeyService.enterQueue(initResp.getId()));
    }

    @Test
    void assignBay_validQueueJourney_success() {
        vehicleService.createVehicle(VehicleRequest.builder()
                .registrationNumber("GJ01BAY100")
                .vehicleType("BUS")
                .ownerName("Bus Owner")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("CERT-100")
                        .issueDate(LocalDate.now().minusMonths(2))
                        .expiryDate(LocalDate.now().plusYears(1))
                        .status(HydroTestStatus.VALID)
                        .issuingAuthority("PESO")
                        .build())
                .build());

        VehicleJourneyResponse initResp = journeyService.createJourney(CreateJourneyRequest.builder()
                .stationId(station.getId())
                .registrationNumber("GJ01BAY100")
                .build());

        journeyService.enterQueue(initResp.getId());

        VehicleJourneyResponse assignResp = journeyService.assignBay(initResp.getId(), new AssignBayRequest(availableBay.getId()));

        assertEquals(JourneyStatus.BAY_ASSIGNED, assignResp.getStatus());
        assertEquals(availableBay.getId(), assignResp.getAssignedBayId());

        FuelingBay updatedBay = fuelingBayRepository.findById(availableBay.getId()).orElseThrow();
        assertEquals(BayStatus.OCCUPIED, updatedBay.getStatus());
    }

    @Test
    void assignBay_unavailableBay_throwsException() {
        vehicleService.createVehicle(VehicleRequest.builder()
                .registrationNumber("GJ01BAD200")
                .vehicleType("CAR")
                .ownerName("Car Owner")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("CERT-200")
                        .issueDate(LocalDate.now().minusMonths(2))
                        .expiryDate(LocalDate.now().plusYears(1))
                        .status(HydroTestStatus.VALID)
                        .issuingAuthority("PESO")
                        .build())
                .build());

        VehicleJourneyResponse initResp = journeyService.createJourney(CreateJourneyRequest.builder()
                .stationId(station.getId())
                .registrationNumber("GJ01BAD200")
                .build());

        journeyService.enterQueue(initResp.getId());

        assertThrows(InvalidStateTransitionException.class, () ->
                journeyService.assignBay(initResp.getId(), new AssignBayRequest(outOfServiceBay.getId()))
        );
    }

    @Test
    void completeLifecycle_startFuelingCompleteFuelingReleaseBayAndExit() {
        vehicleService.createVehicle(VehicleRequest.builder()
                .registrationNumber("GJ01FULL300")
                .vehicleType("AUTO")
                .ownerName("Full Lifecycle Owner")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("CERT-300")
                        .issueDate(LocalDate.now().minusMonths(2))
                        .expiryDate(LocalDate.now().plusYears(1))
                        .status(HydroTestStatus.VALID)
                        .issuingAuthority("PESO")
                        .build())
                .build());

        VehicleJourneyResponse j1 = journeyService.createJourney(CreateJourneyRequest.builder()
                .stationId(station.getId())
                .registrationNumber("GJ01FULL300")
                .build());

        j1 = journeyService.enterQueue(j1.getId());
        j1 = journeyService.assignBay(j1.getId(), new AssignBayRequest(availableBay.getId()));

        // Start Fueling
        j1 = journeyService.startFueling(j1.getId());
        assertEquals(JourneyStatus.FUELING, j1.getStatus());
        assertNotNull(j1.getFuelingStartTime());

        // Complete Fueling
        j1 = journeyService.completeFueling(j1.getId());
        assertEquals(JourneyStatus.FUELING_COMPLETED, j1.getStatus());
        assertNotNull(j1.getFuelingEndTime());

        // Check Bay Released
        FuelingBay releasedBay = fuelingBayRepository.findById(availableBay.getId()).orElseThrow();
        assertEquals(BayStatus.AVAILABLE, releasedBay.getStatus());

        // Exit Station
        j1 = journeyService.exitStation(j1.getId());
        assertEquals(JourneyStatus.EXITED, j1.getStatus());
        assertNotNull(j1.getExitTime());
    }

    @Test
    void invalidStateTransitions_rejectedWithException() {
        vehicleService.createVehicle(VehicleRequest.builder()
                .registrationNumber("GJ01INVALID400")
                .vehicleType("TRUCK")
                .ownerName("Truck Owner")
                .registrationStatus(RegistrationStatus.VALID)
                .registrationExpiry(LocalDate.now().plusYears(1))
                .hydroTestCertificate(HydroTestCertificateDto.builder()
                        .certificateNumber("CERT-400")
                        .issueDate(LocalDate.now().minusMonths(2))
                        .expiryDate(LocalDate.now().plusYears(1))
                        .status(HydroTestStatus.VALID)
                        .issuingAuthority("PESO")
                        .build())
                .build());

        VehicleJourneyResponse j1 = journeyService.createJourney(CreateJourneyRequest.builder()
                .stationId(station.getId())
                .registrationNumber("GJ01INVALID400")
                .build());

        // ENTERED -> FUELING fails
        assertThrows(InvalidStateTransitionException.class, () -> journeyService.startFueling(j1.getId()));

        // ENTERED -> EXITED fails
        assertThrows(InvalidStateTransitionException.class, () -> journeyService.exitStation(j1.getId()));
    }
}
