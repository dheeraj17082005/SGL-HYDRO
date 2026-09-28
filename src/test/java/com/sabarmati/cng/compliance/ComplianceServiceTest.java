package com.sabarmati.cng.compliance;

import com.sabarmati.cng.compliance.dto.ComplianceRequest;
import com.sabarmati.cng.compliance.dto.ComplianceResponse;
import com.sabarmati.cng.compliance.dto.ComplianceResult;
import com.sabarmati.cng.compliance.service.ComplianceService;
import com.sabarmati.cng.integration.HydroTestVerificationClient;
import com.sabarmati.cng.integration.HydroTestVerificationResult;
import com.sabarmati.cng.integration.VehicleRegistrationClient;
import com.sabarmati.cng.integration.VehicleRegistrationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ComplianceServiceTest {

    @Autowired
    private VehicleRegistrationClient vehicleRegistrationClient;

    @Autowired
    private HydroTestVerificationClient hydroTestVerificationClient;

    @Autowired
    private ComplianceService complianceService;

    @Test
    void verifyCompliance_eligibleVehicle_returnsEligible() {
        ComplianceResponse response = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01AB1234").build()
        );

        assertTrue(response.isEligible());
        assertEquals(ComplianceResult.ELIGIBLE, response.getResult());
        assertEquals("GJ01AB1234", response.getRegistrationNumber());
    }

    @Test
    void verifyCompliance_vehicleNotFound_returnsVehicleNotFound() {
        ComplianceResponse response = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ99UNREG9999").build()
        );

        assertFalse(response.isEligible());
        assertEquals(ComplianceResult.VEHICLE_NOT_FOUND, response.getResult());
    }

    @Test
    void verifyCompliance_invalidRegistration_returnsRegistrationInvalid() {
        ComplianceResponse response = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01INV").build()
        );

        assertFalse(response.isEligible());
        assertEquals(ComplianceResult.REGISTRATION_INVALID, response.getResult());
    }

    @Test
    void verifyCompliance_missingHydroTest_returnsHydroTestMissing() {
        ComplianceResponse response = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01NOHYDRO").build()
        );

        assertFalse(response.isEligible());
        assertEquals(ComplianceResult.HYDRO_TEST_MISSING, response.getResult());
    }

    @Test
    void verifyCompliance_expiredHydroTest_returnsHydroTestExpired() {
        ComplianceResponse response = complianceService.verifyCompliance(
                ComplianceRequest.builder().registrationNumber("GJ01EXPHYDRO").build()
        );

        assertFalse(response.isEligible());
        assertEquals(ComplianceResult.HYDRO_TEST_EXPIRED, response.getResult());
    }
}
