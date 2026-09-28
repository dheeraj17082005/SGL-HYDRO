package com.sabarmati.cng.compliance.service;

import com.sabarmati.cng.compliance.dto.ComplianceRequest;
import com.sabarmati.cng.compliance.dto.ComplianceResponse;
import com.sabarmati.cng.compliance.dto.ComplianceResult;
import com.sabarmati.cng.integration.HydroTestVerificationClient;
import com.sabarmati.cng.integration.HydroTestVerificationResult;
import com.sabarmati.cng.integration.VehicleRegistrationClient;
import com.sabarmati.cng.integration.VehicleRegistrationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ComplianceService {

    private final VehicleRegistrationClient vehicleRegistrationClient;
    private final HydroTestVerificationClient hydroTestVerificationClient;

    public ComplianceService(VehicleRegistrationClient vehicleRegistrationClient,
                             HydroTestVerificationClient hydroTestVerificationClient) {
        this.vehicleRegistrationClient = vehicleRegistrationClient;
        this.hydroTestVerificationClient = hydroTestVerificationClient;
    }

    @Transactional(readOnly = true)
    public ComplianceResponse verifyCompliance(ComplianceRequest request) {
        String cleanRegNumber = request.getRegistrationNumber() != null ?
                request.getRegistrationNumber().replaceAll("[^a-zA-Z0-9]", "").toUpperCase() : "";

        // 1. External / Mock Vehicle Registration Verification
        VehicleRegistrationResult regResult = vehicleRegistrationClient.verifyRegistration(cleanRegNumber);

        if (!regResult.isFound()) {
            return ComplianceResponse.builder()
                    .registrationNumber(cleanRegNumber)
                    .result(ComplianceResult.VEHICLE_NOT_FOUND)
                    .eligible(false)
                    .reason("Vehicle registration not found in mParivahan registry")
                    .registrationStatus("INVALID")
                    .hydroTestStatus("UNKNOWN")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (!regResult.isValid()) {
            return ComplianceResponse.builder()
                    .registrationNumber(cleanRegNumber)
                    .result(ComplianceResult.REGISTRATION_INVALID)
                    .eligible(false)
                    .reason("Vehicle registration is invalid or expired")
                    .ownerName(regResult.getOwnerName())
                    .vehicleType(regResult.getVehicleType())
                    .registrationStatus("INVALID")
                    .registrationExpiry(regResult.getRegistrationExpiry())
                    .hydroTestStatus("UNKNOWN")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        // 2. External / Mock Hydro-Test Certificate Verification
        HydroTestVerificationResult hydroResult = hydroTestVerificationClient.verify(cleanRegNumber);

        if (!hydroResult.isFound()) {
            return ComplianceResponse.builder()
                    .registrationNumber(cleanRegNumber)
                    .result(ComplianceResult.HYDRO_TEST_MISSING)
                    .eligible(false)
                    .reason("Hydro-test certificate is missing for vehicle")
                    .ownerName(regResult.getOwnerName())
                    .vehicleType(regResult.getVehicleType())
                    .registrationStatus("VALID")
                    .registrationExpiry(regResult.getRegistrationExpiry())
                    .hydroTestStatus("UNKNOWN")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (!hydroResult.isValid()) {
            return ComplianceResponse.builder()
                    .registrationNumber(cleanRegNumber)
                    .result(ComplianceResult.HYDRO_TEST_EXPIRED)
                    .eligible(false)
                    .reason("CNG cylinder hydro-test certificate is expired or invalid")
                    .ownerName(regResult.getOwnerName())
                    .vehicleType(regResult.getVehicleType())
                    .registrationStatus("VALID")
                    .registrationExpiry(regResult.getRegistrationExpiry())
                    .certificateNumber(hydroResult.getCertificateNumber())
                    .hydroTestExpiry(hydroResult.getExpiryDate())
                    .hydroTestStatus("EXPIRED")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        // All compliance checks passed
        return ComplianceResponse.builder()
                .registrationNumber(cleanRegNumber)
                .result(ComplianceResult.ELIGIBLE)
                .eligible(true)
                .reason("Vehicle registration and CNG cylinder hydro-test are valid")
                .ownerName(regResult.getOwnerName())
                .vehicleType(regResult.getVehicleType())
                .registrationStatus("VALID")
                .registrationExpiry(regResult.getRegistrationExpiry())
                .certificateNumber(hydroResult.getCertificateNumber())
                .hydroTestExpiry(hydroResult.getExpiryDate())
                .hydroTestStatus("VALID")
                .verifiedAt(LocalDateTime.now())
                .build();
    }
}
