package com.sabarmati.cng.anpr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class AnprDetectionRequest {

    @NotNull(message = "Station ID is required")
    private Long stationId;

    @NotNull(message = "Camera ID is required")
    private Long cameraId;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    private LocalDateTime detectedAt;

    public AnprDetectionRequest() {
    }

    public AnprDetectionRequest(Long stationId, Long cameraId, String registrationNumber, LocalDateTime detectedAt) {
        this.stationId = stationId;
        this.cameraId = cameraId;
        this.registrationNumber = registrationNumber;
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long stationId;
        private Long cameraId;
        private String registrationNumber;
        private LocalDateTime detectedAt = LocalDateTime.now();

        public Builder stationId(Long stationId) {
            this.stationId = stationId;
            return this;
        }

        public Builder cameraId(Long cameraId) {
            this.cameraId = cameraId;
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder detectedAt(LocalDateTime detectedAt) {
            if (detectedAt != null) this.detectedAt = detectedAt;
            return this;
        }

        public AnprDetectionRequest build() {
            return new AnprDetectionRequest(stationId, cameraId, registrationNumber, detectedAt);
        }
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public Long getCameraId() {
        return cameraId;
    }

    public void setCameraId(Long cameraId) {
        this.cameraId = cameraId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}
