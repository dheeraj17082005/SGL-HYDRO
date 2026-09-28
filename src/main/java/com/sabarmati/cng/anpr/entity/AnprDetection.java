package com.sabarmati.cng.anpr.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sgl_anpr_detections")
public class AnprDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "registration_number", nullable = false)
    private String registrationNumber;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AnprDetection() {
    }

    public AnprDetection(Long id, Long stationId, Long cameraId, String registrationNumber, LocalDateTime detectedAt, LocalDateTime createdAt) {
        this.id = id;
        this.stationId = stationId;
        this.cameraId = cameraId;
        this.registrationNumber = registrationNumber;
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long stationId;
        private Long cameraId;
        private String registrationNumber;
        private LocalDateTime detectedAt = LocalDateTime.now();
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

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

        public Builder createdAt(LocalDateTime createdAt) {
            if (createdAt != null) this.createdAt = createdAt;
            return this;
        }

        public AnprDetection build() {
            return new AnprDetection(id, stationId, cameraId, registrationNumber, detectedAt, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
