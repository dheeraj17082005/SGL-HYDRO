package com.sabarmati.cng.vehicle.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehicleVerificationResponse {

    private String registrationNumber;
    private boolean vehicleFound;
    private String vehicleType;
    private String fuelType;
    private String makeModel;
    private String ownerNameMasked;
    private String rtoOffice;
    private boolean registrationValid;
    private boolean insuranceValid;
    private boolean fitnessValid;
    private boolean pucValid;

    // PESO Hydro-Test Certificate Details
    private String hydroTestStatus; // UNKNOWN, VALID, EXPIRED, MISSING, DUE_SOON
    private String hydroTestCertNumber;
    private String cylinderSerialNo;
    private LocalDate hydroTestDate;
    private LocalDate hydroTestExpiry;
    private String testingStation;
    private Long validityDaysRemaining;

    // Overall Compliance Status
    private String complianceStatus; // ELIGIBLE, NOT_ELIGIBLE, VERIFICATION_UNAVAILABLE
    private List<String> reasons = new ArrayList<>();
    private String source; // RAPIDAPI, DEMO, MOCK
    private LocalDateTime verifiedAt = LocalDateTime.now();

    public VehicleVerificationResponse() {
    }

    public VehicleVerificationResponse(String registrationNumber, boolean vehicleFound, String vehicleType, String fuelType, String makeModel, String ownerNameMasked, String rtoOffice, boolean registrationValid, boolean insuranceValid, boolean fitnessValid, boolean pucValid, String hydroTestStatus, String hydroTestCertNumber, String cylinderSerialNo, LocalDate hydroTestDate, LocalDate hydroTestExpiry, String testingStation, Long validityDaysRemaining, String complianceStatus, List<String> reasons, String source, LocalDateTime verifiedAt) {
        this.registrationNumber = registrationNumber;
        this.vehicleFound = vehicleFound;
        this.vehicleType = vehicleType;
        this.fuelType = fuelType;
        this.makeModel = makeModel;
        this.ownerNameMasked = ownerNameMasked;
        this.rtoOffice = rtoOffice;
        this.registrationValid = registrationValid;
        this.insuranceValid = insuranceValid;
        this.fitnessValid = fitnessValid;
        this.pucValid = pucValid;
        this.hydroTestStatus = hydroTestStatus;
        this.hydroTestCertNumber = hydroTestCertNumber;
        this.cylinderSerialNo = cylinderSerialNo;
        this.hydroTestDate = hydroTestDate;
        this.hydroTestExpiry = hydroTestExpiry;
        this.testingStation = testingStation;
        this.validityDaysRemaining = validityDaysRemaining;
        this.complianceStatus = complianceStatus;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.source = source;
        this.verifiedAt = verifiedAt != null ? verifiedAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String registrationNumber;
        private boolean vehicleFound;
        private String vehicleType;
        private String fuelType;
        private String makeModel;
        private String ownerNameMasked;
        private String rtoOffice;
        private boolean registrationValid;
        private boolean insuranceValid = true;
        private boolean fitnessValid = true;
        private boolean pucValid = true;
        private String hydroTestStatus = "UNKNOWN";
        private String hydroTestCertNumber;
        private String cylinderSerialNo;
        private LocalDate hydroTestDate;
        private LocalDate hydroTestExpiry;
        private String testingStation;
        private Long validityDaysRemaining;
        private String complianceStatus = "NOT_ELIGIBLE";
        private List<String> reasons = new ArrayList<>();
        private String source = "MOCK";
        private LocalDateTime verifiedAt = LocalDateTime.now();

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder vehicleFound(boolean vehicleFound) {
            this.vehicleFound = vehicleFound;
            return this;
        }

        public Builder vehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
            return this;
        }

        public Builder fuelType(String fuelType) {
            this.fuelType = fuelType;
            return this;
        }

        public Builder makeModel(String makeModel) {
            this.makeModel = makeModel;
            return this;
        }

        public Builder ownerNameMasked(String ownerNameMasked) {
            this.ownerNameMasked = ownerNameMasked;
            return this;
        }

        public Builder rtoOffice(String rtoOffice) {
            this.rtoOffice = rtoOffice;
            return this;
        }

        public Builder registrationValid(boolean registrationValid) {
            this.registrationValid = registrationValid;
            return this;
        }

        public Builder insuranceValid(boolean insuranceValid) {
            this.insuranceValid = insuranceValid;
            return this;
        }

        public Builder fitnessValid(boolean fitnessValid) {
            this.fitnessValid = fitnessValid;
            return this;
        }

        public Builder pucValid(boolean pucValid) {
            this.pucValid = pucValid;
            return this;
        }

        public Builder hydroTestStatus(String hydroTestStatus) {
            this.hydroTestStatus = hydroTestStatus;
            return this;
        }

        public Builder hydroTestCertNumber(String hydroTestCertNumber) {
            this.hydroTestCertNumber = hydroTestCertNumber;
            return this;
        }

        public Builder cylinderSerialNo(String cylinderSerialNo) {
            this.cylinderSerialNo = cylinderSerialNo;
            return this;
        }

        public Builder hydroTestDate(LocalDate hydroTestDate) {
            this.hydroTestDate = hydroTestDate;
            return this;
        }

        public Builder hydroTestExpiry(LocalDate hydroTestExpiry) {
            this.hydroTestExpiry = hydroTestExpiry;
            return this;
        }

        public Builder testingStation(String testingStation) {
            this.testingStation = testingStation;
            return this;
        }

        public Builder validityDaysRemaining(Long validityDaysRemaining) {
            this.validityDaysRemaining = validityDaysRemaining;
            return this;
        }

        public Builder complianceStatus(String complianceStatus) {
            this.complianceStatus = complianceStatus;
            return this;
        }

        public Builder reasons(List<String> reasons) {
            if (reasons != null) this.reasons = reasons;
            return this;
        }

        public Builder addReason(String reason) {
            if (reason != null && !reason.isBlank()) {
                this.reasons.add(reason);
            }
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder verifiedAt(LocalDateTime verifiedAt) {
            if (verifiedAt != null) this.verifiedAt = verifiedAt;
            return this;
        }

        public VehicleVerificationResponse build() {
            return new VehicleVerificationResponse(
                    registrationNumber, vehicleFound, vehicleType, fuelType, makeModel, ownerNameMasked, rtoOffice,
                    registrationValid, insuranceValid, fitnessValid, pucValid,
                    hydroTestStatus, hydroTestCertNumber, cylinderSerialNo, hydroTestDate, hydroTestExpiry, testingStation, validityDaysRemaining,
                    complianceStatus, reasons, source, verifiedAt
            );
        }
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public boolean isVehicleFound() { return vehicleFound; }
    public void setVehicleFound(boolean vehicleFound) { this.vehicleFound = vehicleFound; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getMakeModel() { return makeModel; }
    public void setMakeModel(String makeModel) { this.makeModel = makeModel; }

    public String getOwnerNameMasked() { return ownerNameMasked; }
    public void setOwnerNameMasked(String ownerNameMasked) { this.ownerNameMasked = ownerNameMasked; }

    public String getRtoOffice() { return rtoOffice; }
    public void setRtoOffice(String rtoOffice) { this.rtoOffice = rtoOffice; }

    public boolean isRegistrationValid() { return registrationValid; }
    public void setRegistrationValid(boolean registrationValid) { this.registrationValid = registrationValid; }

    public boolean isInsuranceValid() { return insuranceValid; }
    public void setInsuranceValid(boolean insuranceValid) { this.insuranceValid = insuranceValid; }

    public boolean isFitnessValid() { return fitnessValid; }
    public void setFitnessValid(boolean fitnessValid) { this.fitnessValid = fitnessValid; }

    public boolean isPucValid() { return pucValid; }
    public void setPucValid(boolean pucValid) { this.pucValid = pucValid; }

    public String getHydroTestStatus() { return hydroTestStatus; }
    public void setHydroTestStatus(String hydroTestStatus) { this.hydroTestStatus = hydroTestStatus; }

    public String getHydroTestCertNumber() { return hydroTestCertNumber; }
    public void setHydroTestCertNumber(String hydroTestCertNumber) { this.hydroTestCertNumber = hydroTestCertNumber; }

    public String getCylinderSerialNo() { return cylinderSerialNo; }
    public void setCylinderSerialNo(String cylinderSerialNo) { this.cylinderSerialNo = cylinderSerialNo; }

    public LocalDate getHydroTestDate() { return hydroTestDate; }
    public void setHydroTestDate(LocalDate hydroTestDate) { this.hydroTestDate = hydroTestDate; }

    public LocalDate getHydroTestExpiry() { return hydroTestExpiry; }
    public void setHydroTestExpiry(LocalDate hydroTestExpiry) { this.hydroTestExpiry = hydroTestExpiry; }

    public String getTestingStation() { return testingStation; }
    public void setTestingStation(String testingStation) { this.testingStation = testingStation; }

    public Long getValidityDaysRemaining() { return validityDaysRemaining; }
    public void setValidityDaysRemaining(Long validityDaysRemaining) { this.validityDaysRemaining = validityDaysRemaining; }

    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
