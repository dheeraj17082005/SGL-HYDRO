package com.sabarmati.cng.vehicle.dto;

import com.sabarmati.cng.vehicle.entity.RegistrationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class VehicleRequest {

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    private RegistrationStatus registrationStatus = RegistrationStatus.VALID;

    @NotNull(message = "Registration expiry date is required")
    private LocalDate registrationExpiry;

    @Valid
    private HydroTestCertificateDto hydroTestCertificate;

    public VehicleRequest() {
    }

    public VehicleRequest(String registrationNumber, String vehicleType, String ownerName, RegistrationStatus registrationStatus, LocalDate registrationExpiry, HydroTestCertificateDto hydroTestCertificate) {
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.registrationStatus = registrationStatus != null ? registrationStatus : RegistrationStatus.VALID;
        this.registrationExpiry = registrationExpiry;
        this.hydroTestCertificate = hydroTestCertificate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registrationNumber;
        private String vehicleType;
        private String ownerName;
        private RegistrationStatus registrationStatus = RegistrationStatus.VALID;
        private LocalDate registrationExpiry;
        private HydroTestCertificateDto hydroTestCertificate;

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
            if (registrationStatus != null) this.registrationStatus = registrationStatus;
            return this;
        }

        public Builder registrationExpiry(LocalDate registrationExpiry) {
            this.registrationExpiry = registrationExpiry;
            return this;
        }

        public Builder hydroTestCertificate(HydroTestCertificateDto hydroTestCertificate) {
            this.hydroTestCertificate = hydroTestCertificate;
            return this;
        }

        public VehicleRequest build() {
            return new VehicleRequest(registrationNumber, vehicleType, ownerName, registrationStatus, registrationExpiry, hydroTestCertificate);
        }
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

    public HydroTestCertificateDto getHydroTestCertificate() {
        return hydroTestCertificate;
    }

    public void setHydroTestCertificate(HydroTestCertificateDto hydroTestCertificate) {
        this.hydroTestCertificate = hydroTestCertificate;
    }
}
