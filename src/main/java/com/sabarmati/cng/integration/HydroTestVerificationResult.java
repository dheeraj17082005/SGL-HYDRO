package com.sabarmati.cng.integration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class HydroTestVerificationResult {

    private String certificateNumber;
    private String cylinderSerialNumber;
    private boolean found;
    private boolean valid;
    private LocalDate hydroTestDate;
    private LocalDate expiryDate;
    private String issuingAuthority;
    private Long validityDaysRemaining;

    public HydroTestVerificationResult() {
    }

    public HydroTestVerificationResult(String certificateNumber, String cylinderSerialNumber, boolean found, boolean valid, LocalDate hydroTestDate, LocalDate expiryDate, String issuingAuthority, Long validityDaysRemaining) {
        this.certificateNumber = certificateNumber;
        this.cylinderSerialNumber = cylinderSerialNumber;
        this.found = found;
        this.valid = valid;
        this.hydroTestDate = hydroTestDate;
        this.expiryDate = expiryDate;
        this.issuingAuthority = issuingAuthority;
        this.validityDaysRemaining = validityDaysRemaining;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String certificateNumber;
        private String cylinderSerialNumber;
        private boolean found;
        private boolean valid;
        private LocalDate hydroTestDate;
        private LocalDate expiryDate;
        private String issuingAuthority;
        private Long validityDaysRemaining;

        public Builder certificateNumber(String certificateNumber) {
            this.certificateNumber = certificateNumber;
            return this;
        }

        public Builder cylinderSerialNumber(String cylinderSerialNumber) {
            this.cylinderSerialNumber = cylinderSerialNumber;
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

        public Builder hydroTestDate(LocalDate hydroTestDate) {
            this.hydroTestDate = hydroTestDate;
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            if (expiryDate != null) {
                this.validityDaysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
            }
            return this;
        }

        public Builder issuingAuthority(String issuingAuthority) {
            this.issuingAuthority = issuingAuthority;
            return this;
        }

        public Builder validityDaysRemaining(Long validityDaysRemaining) {
            this.validityDaysRemaining = validityDaysRemaining;
            return this;
        }

        public HydroTestVerificationResult build() {
            return new HydroTestVerificationResult(certificateNumber, cylinderSerialNumber, found, valid, hydroTestDate, expiryDate, issuingAuthority, validityDaysRemaining);
        }
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getCylinderSerialNumber() {
        return cylinderSerialNumber;
    }

    public void setCylinderSerialNumber(String cylinderSerialNumber) {
        this.cylinderSerialNumber = cylinderSerialNumber;
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

    public LocalDate getHydroTestDate() {
        return hydroTestDate;
    }

    public void setHydroTestDate(LocalDate hydroTestDate) {
        this.hydroTestDate = hydroTestDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        if (expiryDate != null) {
            this.validityDaysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        }
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public Long getValidityDaysRemaining() {
        return validityDaysRemaining;
    }

    public void setValidityDaysRemaining(Long validityDaysRemaining) {
        this.validityDaysRemaining = validityDaysRemaining;
    }
}

