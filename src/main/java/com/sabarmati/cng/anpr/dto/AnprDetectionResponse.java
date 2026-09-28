package com.sabarmati.cng.anpr.dto;

import com.sabarmati.cng.journey.entity.ComplianceStatus;
import com.sabarmati.cng.journey.entity.JourneyStatus;

public class AnprDetectionResponse {

    private Long journeyId;
    private String registrationNumber;
    private boolean plateDetected = true;
    private Double ocrConfidence;
    private String verificationSource = "MOCK";
    private boolean registrationVerified;
    private boolean hydroTestVerified;
    private ComplianceStatus complianceStatus;
    private JourneyStatus journeyStatus;
    private String message;

    public AnprDetectionResponse() {
    }

    public AnprDetectionResponse(Long journeyId, String registrationNumber, boolean plateDetected, Double ocrConfidence, String verificationSource, boolean registrationVerified, boolean hydroTestVerified, ComplianceStatus complianceStatus, JourneyStatus journeyStatus, String message) {
        this.journeyId = journeyId;
        this.registrationNumber = registrationNumber;
        this.plateDetected = plateDetected;
        this.ocrConfidence = ocrConfidence;
        this.verificationSource = verificationSource != null ? verificationSource : "MOCK";
        this.registrationVerified = registrationVerified;
        this.hydroTestVerified = hydroTestVerified;
        this.complianceStatus = complianceStatus;
        this.journeyStatus = journeyStatus;
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long journeyId;
        private String registrationNumber;
        private boolean plateDetected = true;
        private Double ocrConfidence = 0.95;
        private String verificationSource = "MOCK";
        private boolean registrationVerified;
        private boolean hydroTestVerified;
        private ComplianceStatus complianceStatus;
        private JourneyStatus journeyStatus;
        private String message;

        public Builder journeyId(Long journeyId) {
            this.journeyId = journeyId;
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder plateDetected(boolean plateDetected) {
            this.plateDetected = plateDetected;
            return this;
        }

        public Builder ocrConfidence(Double ocrConfidence) {
            this.ocrConfidence = ocrConfidence;
            return this;
        }

        public Builder verificationSource(String verificationSource) {
            this.verificationSource = verificationSource;
            return this;
        }

        public Builder registrationVerified(boolean registrationVerified) {
            this.registrationVerified = registrationVerified;
            return this;
        }

        public Builder hydroTestVerified(boolean hydroTestVerified) {
            this.hydroTestVerified = hydroTestVerified;
            return this;
        }

        public Builder complianceStatus(ComplianceStatus complianceStatus) {
            this.complianceStatus = complianceStatus;
            return this;
        }

        public Builder journeyStatus(JourneyStatus journeyStatus) {
            this.journeyStatus = journeyStatus;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public AnprDetectionResponse build() {
            return new AnprDetectionResponse(journeyId, registrationNumber, plateDetected, ocrConfidence, verificationSource, registrationVerified, hydroTestVerified, complianceStatus, journeyStatus, message);
        }
    }

    public Long getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(Long journeyId) {
        this.journeyId = journeyId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public boolean isPlateDetected() {
        return plateDetected;
    }

    public void setPlateDetected(boolean plateDetected) {
        this.plateDetected = plateDetected;
    }

    public Double getOcrConfidence() {
        return ocrConfidence;
    }

    public void setOcrConfidence(Double ocrConfidence) {
        this.ocrConfidence = ocrConfidence;
    }

    public String getVerificationSource() {
        return verificationSource;
    }

    public void setVerificationSource(String verificationSource) {
        this.verificationSource = verificationSource;
    }

    public boolean isRegistrationVerified() {
        return registrationVerified;
    }

    public void setRegistrationVerified(boolean registrationVerified) {
        this.registrationVerified = registrationVerified;
    }

    public boolean isHydroTestVerified() {
        return hydroTestVerified;
    }

    public void setHydroTestVerified(boolean hydroTestVerified) {
        this.hydroTestVerified = hydroTestVerified;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public JourneyStatus getJourneyStatus() {
        return journeyStatus;
    }

    public void setJourneyStatus(JourneyStatus journeyStatus) {
        this.journeyStatus = journeyStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
