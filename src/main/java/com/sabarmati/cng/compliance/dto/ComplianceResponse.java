package com.sabarmati.cng.compliance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ComplianceResponse {

    private String registrationNumber;
    private ComplianceResult result;
    private boolean eligible;
    private String reason;

    private String ownerName;
    private String vehicleType;
    private String registrationStatus;
    private LocalDate registrationExpiry;

    private String certificateNumber;
    private LocalDate hydroTestExpiry;
    private String hydroTestStatus;
    private LocalDateTime verifiedAt = LocalDateTime.now();

    public ComplianceResponse() {
    }

    public ComplianceResponse(String registrationNumber, ComplianceResult result, boolean eligible, String reason, String ownerName, String vehicleType, String registrationStatus, LocalDate registrationExpiry, String certificateNumber, LocalDate hydroTestExpiry, String hydroTestStatus, LocalDateTime verifiedAt) {
        this.registrationNumber = registrationNumber;
        this.result = result;
        this.eligible = eligible;
        this.reason = reason;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.registrationStatus = registrationStatus;
        this.registrationExpiry = registrationExpiry;
        this.certificateNumber = certificateNumber;
        this.hydroTestExpiry = hydroTestExpiry;
        this.hydroTestStatus = hydroTestStatus;
        this.verifiedAt = verifiedAt != null ? verifiedAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registrationNumber;
        private ComplianceResult result;
        private boolean eligible;
        private String reason;

        private String ownerName;
        private String vehicleType;
        private String registrationStatus;
        private LocalDate registrationExpiry;

        private String certificateNumber;
        private LocalDate hydroTestExpiry;
        private String hydroTestStatus;
        private LocalDateTime verifiedAt = LocalDateTime.now();

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder result(ComplianceResult result) {
            this.result = result;
            return this;
        }

        public Builder eligible(boolean eligible) {
            this.eligible = eligible;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public Builder vehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
            return this;
        }

        public Builder registrationStatus(String registrationStatus) {
            this.registrationStatus = registrationStatus;
            return this;
        }

        public Builder registrationExpiry(LocalDate registrationExpiry) {
            this.registrationExpiry = registrationExpiry;
            return this;
        }

        public Builder certificateNumber(String certificateNumber) {
            this.certificateNumber = certificateNumber;
            return this;
        }

        public Builder hydroTestExpiry(LocalDate hydroTestExpiry) {
            this.hydroTestExpiry = hydroTestExpiry;
            return this;
        }

        public Builder hydroTestStatus(String hydroTestStatus) {
            this.hydroTestStatus = hydroTestStatus;
            return this;
        }

        public Builder verifiedAt(LocalDateTime verifiedAt) {
            if (verifiedAt != null) this.verifiedAt = verifiedAt;
            return this;
        }

        public ComplianceResponse build() {
            return new ComplianceResponse(registrationNumber, result, eligible, reason, ownerName, vehicleType, registrationStatus, registrationExpiry, certificateNumber, hydroTestExpiry, hydroTestStatus, verifiedAt);
        }
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public ComplianceResult getResult() {
        return result;
    }

    public void setResult(ComplianceResult result) {
        this.result = result;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public LocalDate getRegistrationExpiry() {
        return registrationExpiry;
    }

    public void setRegistrationExpiry(LocalDate registrationExpiry) {
        this.registrationExpiry = registrationExpiry;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDate getHydroTestExpiry() {
        return hydroTestExpiry;
    }

    public void setHydroTestExpiry(LocalDate hydroTestExpiry) {
        this.hydroTestExpiry = hydroTestExpiry;
    }

    public String getHydroTestStatus() {
        return hydroTestStatus;
    }

    public void setHydroTestStatus(String hydroTestStatus) {
        this.hydroTestStatus = hydroTestStatus;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
