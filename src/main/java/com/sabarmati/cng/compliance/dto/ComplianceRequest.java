package com.sabarmati.cng.compliance.dto;

import jakarta.validation.constraints.NotBlank;

public class ComplianceRequest {

    @NotBlank(message = "Registration number is required for compliance verification")
    private String registrationNumber;

    private Long stationId;

    public ComplianceRequest() {
    }

    public ComplianceRequest(String registrationNumber, Long stationId) {
        this.registrationNumber = registrationNumber;
        this.stationId = stationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registrationNumber;
        private Long stationId;

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder stationId(Long stationId) {
            this.stationId = stationId;
            return this;
        }

        public ComplianceRequest build() {
            return new ComplianceRequest(registrationNumber, stationId);
        }
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }
}
