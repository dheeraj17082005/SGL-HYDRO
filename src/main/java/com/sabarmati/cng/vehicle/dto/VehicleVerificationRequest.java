package com.sabarmati.cng.vehicle.dto;

import jakarta.validation.constraints.NotBlank;

public class VehicleVerificationRequest {

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    public VehicleVerificationRequest() {
    }

    public VehicleVerificationRequest(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
