package com.sabarmati.cng.vehicle.dto;

import com.sabarmati.cng.vehicle.entity.RegistrationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VehicleResponse {

    private Long id;
    private String registrationNumber;
    private String vehicleType;
    private String ownerName;
    private RegistrationStatus registrationStatus;
    private LocalDate registrationExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private HydroTestCertificateDto hydroTestCertificate;

    public VehicleResponse() {
    }

    public VehicleResponse(Long id, String registrationNumber, String vehicleType, String ownerName, RegistrationStatus registrationStatus, LocalDate registrationExpiry, LocalDateTime createdAt, LocalDateTime updatedAt, HydroTestCertificateDto hydroTestCertificate) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.registrationStatus = registrationStatus;
        this.registrationExpiry = registrationExpiry;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.hydroTestCertificate = hydroTestCertificate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String registrationNumber;
        private String vehicleType;
        private String ownerName;
        private RegistrationStatus registrationStatus;
        private LocalDate registrationExpiry;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private HydroTestCertificateDto hydroTestCertificate;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder vehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public Builder registrationStatus(RegistrationStatus registrationStatus) {
            this.registrationStatus = registrationStatus;
            return this;
        }

        public Builder registrationExpiry(LocalDate registrationExpiry) {
            this.registrationExpiry = registrationExpiry;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder hydroTestCertificate(HydroTestCertificateDto hydroTestCertificate) {
            this.hydroTestCertificate = hydroTestCertificate;
            return this;
        }

        public VehicleResponse build() {
            return new VehicleResponse(id, registrationNumber, vehicleType, ownerName, registrationStatus, registrationExpiry, createdAt, updatedAt, hydroTestCertificate);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public LocalDate getRegistrationExpiry() {
        return registrationExpiry;
    }

    public void setRegistrationExpiry(LocalDate registrationExpiry) {
        this.registrationExpiry = registrationExpiry;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public HydroTestCertificateDto getHydroTestCertificate() {
        return hydroTestCertificate;
    }

    public void setHydroTestCertificate(HydroTestCertificateDto hydroTestCertificate) {
        this.hydroTestCertificate = hydroTestCertificate;
    }
}
