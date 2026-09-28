package com.sabarmati.cng.anpr;

import com.sabarmati.cng.anpr.dto.AnprDetectionRequest;
import com.sabarmati.cng.anpr.dto.AnprDetectionResponse;
import com.sabarmati.cng.anpr.repository.AnprDetectionRepository;
import com.sabarmati.cng.anpr.service.AnprService;
import com.sabarmati.cng.compliance.dto.ComplianceRequest;
import com.sabarmati.cng.compliance.dto.ComplianceResponse;
import com.sabarmati.cng.compliance.dto.ComplianceResult;
import com.sabarmati.cng.compliance.service.ComplianceService;
import com.sabarmati.cng.journey.entity.ComplianceStatus;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Camera;
import com.sabarmati.cng.station.entity.CameraType;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.CameraRepository;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.dto.VehicleVerificationRequest;
import com.sabarmati.cng.vehicle.dto.VehicleVerificationResponse;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import com.sabarmati.cng.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AnprComplianceConsistencyTest {

    @Autowired
    private ComplianceService complianceService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private AnprService anprService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private VehicleJourneyRepository vehicleJourneyRepository;

    @Autowired
    private AnprDetectionRepository anprDetectionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Station station;
    private Camera camera;

    @BeforeEach
    void setUp() {
        vehicleJourneyRepository.deleteAll();
        anprDetectionRepository.deleteAll();
        cameraRepository.deleteAll();
        stationRepository.deleteAll();
        vehicleRepository.deleteAll();

        station = Station.builder()
                .name("SGL Station 1")
                .code("SGL-01")
                .address("Ahmedabad")
                .latitude(23.0)
                .longitude(72.0)
                .active(true)
                .build();
        station = stationRepository.save(station);

        camera = Camera.builder()
                .station(station)
                .cameraType(CameraType.ANPR)
                .cameraIdentifier("CAM-01")
                .active(true)
                .build();
        camera = cameraRepository.save(camera);
    }

    @Test
    @DisplayName("Verify ComplianceResponse non-null statuses across all 5 vehicle states")
    void testComplianceResponseNonNullStatuses() {
        // 1. Valid
        ComplianceResponse validResp = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01AB1234").stationId(station.getId()).build()
        );
        assertTrue(validResp.isEligible());
        assertEquals(ComplianceResult.ELIGIBLE, validResp.getResult());
        assertEquals("VALID", validResp.getRegistrationStatus());
        assertEquals("VALID", validResp.getHydroTestStatus());

        // 2. Missing Hydro
        ComplianceResponse missingHydroResp = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01NOHYDRO").stationId(station.getId()).build()
        );
        assertFalse(missingHydroResp.isEligible());
        assertEquals(ComplianceResult.HYDRO_TEST_MISSING, missingHydroResp.getResult());
        assertEquals("VALID", missingHydroResp.getRegistrationStatus());
        assertEquals("UNKNOWN", missingHydroResp.getHydroTestStatus());

        // 3. Expired Hydro
        ComplianceResponse expHydroResp = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01EXPHYDRO").stationId(station.getId()).build()
        );
        assertFalse(expHydroResp.isEligible());
        assertEquals(ComplianceResult.HYDRO_TEST_EXPIRED, expHydroResp.getResult());
        assertEquals("VALID", expHydroResp.getRegistrationStatus());
        assertEquals("EXPIRED", expHydroResp.getHydroTestStatus());

        // 4. Invalid Reg
        ComplianceResponse invRegResp = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01INV").stationId(station.getId()).build()
        );
        assertFalse(invRegResp.isEligible());
        assertEquals(ComplianceResult.REGISTRATION_INVALID, invRegResp.getResult());
        assertEquals("INVALID", invRegResp.getRegistrationStatus());
        assertEquals("UNKNOWN", invRegResp.getHydroTestStatus());

        // 5. Not Found
        ComplianceResponse notFoundResp = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ99UNREG9999").stationId(station.getId()).build()
        );
        assertFalse(notFoundResp.isEligible());
        assertEquals(ComplianceResult.VEHICLE_NOT_FOUND, notFoundResp.getResult());
        assertEquals("INVALID", notFoundResp.getRegistrationStatus());
        assertEquals("UNKNOWN", notFoundResp.getHydroTestStatus());
    }

    @Test
    @DisplayName("Verify AnprService correctly maps registrationVerified and hydroTestVerified flags")
    void testAnprServiceStatusMapping() {
        // State 1: Valid Vehicle
        AnprDetectionResponse validDetection = anprService.processDetection(AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01AB1234")
                .detectedAt(LocalDateTime.now())
                .build());
        assertEquals(ComplianceStatus.ELIGIBLE, validDetection.getComplianceStatus());
        assertTrue(validDetection.isRegistrationVerified(), "Valid vehicle registration must be verified");
        assertTrue(validDetection.isHydroTestVerified(), "Valid vehicle hydro-test must be verified");

        // State 2: Missing Hydro Vehicle
        AnprDetectionResponse missingDetection = anprService.processDetection(AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01NOHYDRO")
                .detectedAt(LocalDateTime.now())
                .build());
        assertEquals(ComplianceStatus.NOT_ELIGIBLE, missingDetection.getComplianceStatus());
        assertTrue(missingDetection.isRegistrationVerified(), "RTO registration was valid, so registrationVerified must be true");
        assertFalse(missingDetection.isHydroTestVerified(), "Hydro-test is missing/unknown, so hydroTestVerified must be false");

        // State 3: Expired Hydro Vehicle
        AnprDetectionResponse expiredDetection = anprService.processDetection(AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01EXPHYDRO")
                .detectedAt(LocalDateTime.now())
                .build());
        assertEquals(ComplianceStatus.NOT_ELIGIBLE, expiredDetection.getComplianceStatus());
        assertTrue(expiredDetection.isRegistrationVerified(), "RTO registration was valid, so registrationVerified must be true");
        assertFalse(expiredDetection.isHydroTestVerified(), "Hydro-test is expired, so hydroTestVerified must be false");

        // State 4: Invalid Registration Vehicle
        AnprDetectionResponse invalidRegDetection = anprService.processDetection(AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ01INV")
                .detectedAt(LocalDateTime.now())
                .build());
        assertEquals(ComplianceStatus.NOT_ELIGIBLE, invalidRegDetection.getComplianceStatus());
        assertFalse(invalidRegDetection.isRegistrationVerified(), "Registration is invalid, so registrationVerified must be false");
        assertFalse(invalidRegDetection.isHydroTestVerified(), "Registration invalid, so hydroTestVerified must be false");

        // State 5: Unregistered Vehicle
        AnprDetectionResponse unregDetection = anprService.processDetection(AnprDetectionRequest.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber("GJ99UNREG9999")
                .detectedAt(LocalDateTime.now())
                .build());
        assertEquals(ComplianceStatus.NOT_ELIGIBLE, unregDetection.getComplianceStatus());
        assertFalse(unregDetection.isRegistrationVerified(), "Unregistered vehicle, so registrationVerified must be false");
        assertFalse(unregDetection.isHydroTestVerified(), "Unregistered vehicle, so hydroTestVerified must be false");
    }

    @Test
    @DisplayName("Verify Eligibility Agreement between ComplianceService and VehicleService on Boundary States")
    void testEligibilityAgreementOnBoundaryStates() {
        String[] boundaryPlates = {
                "GJ01AB1234",
                "GJ01NOHYDRO",
                "GJ01EXPHYDRO",
                "GJ01INV",
                "GJ99UNREG9999"
        };

        for (String plate : boundaryPlates) {
            ComplianceResponse compResp = complianceService.verifyCompliance(
                    ComplianceRequest.builder().registrationNumber(plate).stationId(station.getId()).build()
            );
            VehicleVerificationResponse vehResp = vehicleService.verifyVehicle(
                    new VehicleVerificationRequest(plate)
            );

            boolean compEligible = compResp.isEligible();
            boolean vehEligible = "ELIGIBLE".equalsIgnoreCase(vehResp.getComplianceStatus());

            assertEquals(compEligible, vehEligible,
                    "Eligibility mismatch for plate: " + plate + ". ComplianceService=" + compEligible + ", VehicleService=" + vehEligible);
        }
    }

    @Test
    @DisplayName("Verify Eligibility Agreement across all 100 vehicles in Mock dataset")
    void testEligibilityAgreementAcross100VehiclesDataset() {
        for (int i = 1; i <= 100; i++) {
            String plate = String.format("MH12DE%04d", 1000 + i);

            ComplianceResponse compResp = complianceService.verifyCompliance(
                    ComplianceRequest.builder().registrationNumber(plate).stationId(station.getId()).build()
            );
            VehicleVerificationResponse vehResp = vehicleService.verifyVehicle(
                    new VehicleVerificationRequest(plate)
            );

            boolean compEligible = compResp.isEligible();
            boolean vehEligible = "ELIGIBLE".equalsIgnoreCase(vehResp.getComplianceStatus());

            assertEquals(compEligible, vehEligible,
                    "Dataset plate " + plate + " eligibility discrepancy: ComplianceService=" + compEligible + " (" + compResp.getResult() + "), VehicleService=" + vehEligible + " (" + vehResp.getComplianceStatus() + ")");

            if (i <= 75) {
                // 1-75: Valid
                assertTrue(compEligible, plate + " should be ELIGIBLE");
                assertTrue(vehEligible, plate + " should be ELIGIBLE");
            } else if (i <= 90) {
                // 76-90: Expired
                assertFalse(compEligible, plate + " should be NOT_ELIGIBLE");
                assertFalse(vehEligible, plate + " should be NOT_ELIGIBLE");
            } else if (i <= 95) {
                // 91-95: Due Soon (still valid)
                assertTrue(compEligible, plate + " should be ELIGIBLE");
                assertTrue(vehEligible, plate + " should be ELIGIBLE");
            } else {
                // 96-100: Missing
                assertFalse(compEligible, plate + " should be NOT_ELIGIBLE");
                assertFalse(vehEligible, plate + " should be NOT_ELIGIBLE");
            }
        }
    }

    @Test
    @DisplayName("Verify discrepancy in hydroTestStatus representation between ComplianceService and VehicleService")
    void testHydroTestStatusRepresentationComparison() {
        // Missing hydro plate
        ComplianceResponse compMissing = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01NOHYDRO").stationId(station.getId()).build()
        );
        VehicleVerificationResponse vehMissing = vehicleService.verifyVehicle(
                new VehicleVerificationRequest("GJ01NOHYDRO")
        );

        // ComplianceService adheres to constraint 5 (returns UNKNOWN)
        assertEquals("UNKNOWN", compMissing.getHydroTestStatus(), "ComplianceService must return UNKNOWN for missing hydro");

        // VehicleService returns MISSING (documenting this specific behavior/divergence)
        assertNotNull(vehMissing.getHydroTestStatus(), "VehicleService must return a non-null hydroTestStatus");
    }
}
