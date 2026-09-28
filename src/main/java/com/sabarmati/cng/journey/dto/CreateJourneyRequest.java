package com.sabarmati.cng.journey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateJourneyRequest {

    @NotNull(message = "Station ID is required")
    private Long stationId;

    @NotBlank(message = "Vehicle registration number is required")
    private String registrationNumber;

    public CreateJourneyRequest() {
    }

    public CreateJourneyRequest(Long stationId, String registrationNumber) {
        this.stationId = stationId;
        this.registrationNumber = registrationNumber;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long stationId;
        private String registrationNumber;

        public Builder stationId(Long stationId) {
            this.stationId = stationId;
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public CreateJourneyRequest build() {
            return new CreateJourneyRequest(stationId, registrationNumber);
        }
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
