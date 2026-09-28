package com.sabarmati.cng.integration;

import com.sabarmati.cng.vehicle.entity.RegistrationStatus;
import com.sabarmati.cng.vehicle.entity.Vehicle;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "vehicle.verification.provider", havingValue = "mock", matchIfMissing = true)
public class MockVehicleRegistrationClient implements VehicleRegistrationClient {

    private final VehicleRepository vehicleRepository;

    public MockVehicleRegistrationClient(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public VehicleRegistrationResult verifyRegistration(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return VehicleRegistrationResult.builder()
                    .registrationNumber(registrationNumber)
                    .found(false)
                    .valid(false)
                    .build();
        }

        String cleanRegNumber = registrationNumber.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        Optional<Vehicle> vehicleOpt = vehicleRepository.findByRegistrationNumber(cleanRegNumber);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            boolean isNotExpired = v.getRegistrationExpiry() != null && !v.getRegistrationExpiry().isBefore(LocalDate.now());
            boolean isValidStatus = v.getRegistrationStatus() == RegistrationStatus.VALID;

            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(true)
                    .valid(isValidStatus && isNotExpired)
                    .vehicleType(v.getVehicleType())
                    .ownerName(v.getOwnerName())
                    .registrationExpiry(v.getRegistrationExpiry())
                    .build();
        }

        // Deterministic mock pattern matching for unknown/unregistered/invalid plates
        if (cleanRegNumber.contains("UNREG") || cleanRegNumber.contains("UNKNOWN") || cleanRegNumber.startsWith("GJK5")) {
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(false)
                    .valid(false)
                    .build();
        }

        if (cleanRegNumber.endsWith("INV") || cleanRegNumber.endsWith("INVALID") || cleanRegNumber.endsWith("9999")) {
            return VehicleRegistrationResult.builder()
                    .registrationNumber(cleanRegNumber)
                    .found(true)
                    .valid(false)
                    .vehicleType("AUTO")
                    .ownerName("Unknown Owner")
                    .registrationExpiry(LocalDate.now().minusDays(10))
                    .build();
        }

        // Default valid registration for known/valid plates (e.g. GJ01AB1234, GJ01EXPHYDRO, etc.)
        return VehicleRegistrationResult.builder()
                .registrationNumber(cleanRegNumber)
                .found(true)
                .valid(true)
                .vehicleType("AUTO")
                .ownerName("Verified Owner")
                .registrationExpiry(LocalDate.now().plusYears(1))
                .build();
    }
}
