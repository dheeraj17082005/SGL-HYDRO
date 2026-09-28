package com.sabarmati.cng.integration;

import com.sabarmati.cng.vehicle.entity.HydroTestCertificate;
import com.sabarmati.cng.vehicle.entity.HydroTestStatus;
import com.sabarmati.cng.vehicle.entity.Vehicle;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MockHydroTestVerificationClient implements HydroTestVerificationClient {

    private final VehicleRepository vehicleRepository;
    private final Map<String, HydroTestVerificationResult> mockDataset = new HashMap<>();

    public MockHydroTestVerificationClient(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
        initialize100VehiclesDataset();
    }

    private void initialize100VehiclesDataset() {
        // 1. Generate 75 VALID Hydro-Test Vehicle Records (MH12DE1001 - MH12DE1075)
        for (int i = 1; i <= 75; i++) {
            String plate = String.format("MH12DE%04d", 1000 + i);
            mockDataset.put(plate, HydroTestVerificationResult.builder()
                    .certificateNumber(String.format("PESO-CERT-2024-%04d", 5000 + i))
                    .cylinderSerialNumber(String.format("CYL-CNG-MH12-%04d", 8000 + i))
                    .found(true)
                    .valid(true)
                    .hydroTestDate(LocalDate.now().minusMonths(6).minusDays(i % 30))
                    .expiryDate(LocalDate.now().plusYears(2).plusMonths(6))
                    .issuingAuthority(String.format("SGL PESO Certified Station #%03d", (i % 5) + 101))
                    .build());
        }

        // 2. Generate 15 EXPIRED Hydro-Test Vehicle Records (MH12DE1076 - MH12DE1090)
        for (int i = 76; i <= 90; i++) {
            String plate = String.format("MH12DE%04d", 1000 + i);
            mockDataset.put(plate, HydroTestVerificationResult.builder()
                    .certificateNumber(String.format("PESO-EXPIRED-2021-%04d", 3000 + i))
                    .cylinderSerialNumber(String.format("CYL-CNG-EXP-%04d", 7000 + i))
                    .found(true)
                    .valid(false)
                    .hydroTestDate(LocalDate.now().minusYears(3).minusMonths(2))
                    .expiryDate(LocalDate.now().minusMonths((i % 6) + 1))
                    .issuingAuthority("SGL PESO Authorized Center #104")
                    .build());
        }

        // 3. Generate 5 DUE SOON Hydro-Test Vehicle Records (MH12DE1091 - MH12DE1095)
        for (int i = 91; i <= 95; i++) {
            String plate = String.format("MH12DE%04d", 1000 + i);
            mockDataset.put(plate, HydroTestVerificationResult.builder()
                    .certificateNumber(String.format("PESO-DUESOON-2021-%04d", 4000 + i))
                    .cylinderSerialNumber(String.format("CYL-CNG-DUE-%04d", 6000 + i))
                    .found(true)
                    .valid(true)
                    .hydroTestDate(LocalDate.now().minusYears(3).plusDays(10 + (i % 10)))
                    .expiryDate(LocalDate.now().plusDays((i % 15) + 5))
                    .issuingAuthority("SGL PESO Testing Lab Pune")
                    .build());
        }

        // 4. Generate 5 MISSING Hydro-Test Vehicle Records (MH12DE1096 - MH12DE1100)
        for (int i = 96; i <= 100; i++) {
            String plate = String.format("MH12DE%04d", 1000 + i);
            mockDataset.put(plate, HydroTestVerificationResult.builder()
                    .found(false)
                    .valid(false)
                    .build());
        }

        // 5. Add standard test presets
        mockDataset.put("MH12DE1433", HydroTestVerificationResult.builder()
                .certificateNumber("PESO-CYL-88219-2024")
                .cylinderSerialNumber("CYL-CNG-MH12-1433")
                .found(true)
                .valid(true)
                .hydroTestDate(LocalDate.of(2024, 3, 15))
                .expiryDate(LocalDate.of(2027, 3, 14))
                .issuingAuthority("SGL PESO Testing Center #402")
                .build());

        mockDataset.put("GJ01AB1234", HydroTestVerificationResult.builder()
                .certificateNumber("PESO-CYL-99410-2024")
                .cylinderSerialNumber("CYL-CNG-GJ01-1234")
                .found(true)
                .valid(true)
                .hydroTestDate(LocalDate.of(2024, 1, 10))
                .expiryDate(LocalDate.of(2027, 1, 9))
                .issuingAuthority("SGL PESO Testing Station Ahmedabad")
                .build());

        mockDataset.put("EXPHYDRO", HydroTestVerificationResult.builder()
                .certificateNumber("PESO-EXP-99999")
                .cylinderSerialNumber("CYL-CNG-EXP-999")
                .found(true)
                .valid(false)
                .hydroTestDate(LocalDate.of(2020, 6, 1))
                .expiryDate(LocalDate.now().minusMonths(1))
                .issuingAuthority("SGL Authorized Center")
                .build());

        mockDataset.put("NOHYDRO", HydroTestVerificationResult.builder()
                .found(false)
                .valid(false)
                .build());
    }

    @Override
    public HydroTestVerificationResult verify(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return HydroTestVerificationResult.builder()
                    .found(false)
                    .valid(false)
                    .build();
        }

        String cleanRegNumber = registrationNumber.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        // 1. Check database first for custom registered vehicles
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByRegistrationNumber(cleanRegNumber);
        if (vehicleOpt.isPresent() && vehicleOpt.get().getHydroTestCertificate() != null) {
            HydroTestCertificate cert = vehicleOpt.get().getHydroTestCertificate();
            boolean isNotExpired = cert.getExpiryDate() != null && !cert.getExpiryDate().isBefore(LocalDate.now());
            boolean isValidStatus = cert.getStatus() == HydroTestStatus.VALID;

            return HydroTestVerificationResult.builder()
                    .certificateNumber(cert.getCertificateNumber())
                    .cylinderSerialNumber("CYL-CNG-DB-" + cleanRegNumber)
                    .found(true)
                    .valid(isValidStatus && isNotExpired)
                    .hydroTestDate(cert.getIssueDate() != null ? cert.getIssueDate() : LocalDate.now().minusYears(1))
                    .expiryDate(cert.getExpiryDate())
                    .issuingAuthority(cert.getIssuingAuthority() != null ? cert.getIssuingAuthority() : "PESO Accredited Center")
                    .build();
        }

        // 2. Check 100-vehicle mock dataset
        if (mockDataset.containsKey(cleanRegNumber)) {
            return mockDataset.get(cleanRegNumber);
        }

        // 3. Fallback pattern checks
        if (cleanRegNumber.contains("NOHYDRO") || cleanRegNumber.contains("MISSING") || cleanRegNumber.contains("UNREG")) {
            return HydroTestVerificationResult.builder()
                    .found(false)
                    .valid(false)
                    .build();
        }

        if (cleanRegNumber.contains("EXPHYDRO") || cleanRegNumber.contains("EXPIRED")) {
            return HydroTestVerificationResult.builder()
                    .certificateNumber("PESO-EXP-AUTO-" + cleanRegNumber)
                    .cylinderSerialNumber("CYL-CNG-EXP-" + cleanRegNumber)
                    .found(true)
                    .valid(false)
                    .hydroTestDate(LocalDate.now().minusYears(3).minusMonths(6))
                    .expiryDate(LocalDate.now().minusMonths(1))
                    .issuingAuthority("SGL PESO Testing Station")
                    .build();
        }

        // 4. Default valid hydro test certificate for unrecognized plates
        return HydroTestVerificationResult.builder()
                .certificateNumber("PESO-MOCK-" + cleanRegNumber)
                .cylinderSerialNumber("CYL-CNG-" + cleanRegNumber)
                .found(true)
                .valid(true)
                .hydroTestDate(LocalDate.now().minusMonths(8))
                .expiryDate(LocalDate.now().plusYears(2).plusMonths(4))
                .issuingAuthority("SGL PESO Authorized Station #402")
                .build();
    }
}
