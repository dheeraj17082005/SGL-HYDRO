package com.sabarmati.cng.integration;

public interface VehicleRegistrationClient {
    VehicleRegistrationResult verifyRegistration(String registrationNumber);
    default String getProviderName() {
        return "MOCK";
    }
}
