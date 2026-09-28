package com.sabarmati.cng.integration;

import java.time.LocalDate;

public class VehicleRegistrationResult {

    private String registrationNumber;
    private boolean found;
    private boolean valid;
    private String vehicleType;
    private String ownerName;
    private LocalDate registrationExpiry;

    public VehicleRegistrationResult() {
    }

    public VehicleRegistrationResult(String registrationNumber, boolean found, boolean valid, String vehicleType, String ownerName, LocalDate registrationExpiry) {
        this.registrationNumber = registrationNumber;
        this.found = found;
        this.valid = valid;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.registrationExpiry = registrationExpiry;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registrationNumber;
        private boolean found;
        private boolean valid;
        private String vehicleType;
        private String ownerName;
        private LocalDate registrationExpiry;

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder found(boolean found) {
            this.found = found;
            return this;
        }

        public Builder valid(boolean valid) {
            this.valid = valid;
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

        public Builder registrationExpiry(LocalDate registrationExpiry) {
            this.registrationExpiry = registrationExpiry;
            return this;
        }

        public VehicleRegistrationResult build() {
            return new VehicleRegistrationResult(registrationNumber, found, valid, vehicleType, ownerName, registrationExpiry);
        }
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public LocalDate getRegistrationExpiry() {
        return registrationExpiry;
    }

    public void setRegistrationExpiry(LocalDate registrationExpiry) {
        this.registrationExpiry = registrationExpiry;
    }
}
