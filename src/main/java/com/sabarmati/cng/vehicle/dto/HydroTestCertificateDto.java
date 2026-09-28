package com.sabarmati.cng.vehicle.dto;

import com.sabarmati.cng.vehicle.entity.HydroTestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class HydroTestCertificateDto {

    private Long id;

    @NotBlank(message = "Certificate number is required")
    private String certificateNumber;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    private HydroTestStatus status = HydroTestStatus.VALID;

    @NotBlank(message = "Issuing authority is required")
    private String issuingAuthority;

    public HydroTestCertificateDto() {
    }

    public HydroTestCertificateDto(Long id, String certificateNumber, LocalDate issueDate, LocalDate expiryDate, HydroTestStatus status, String issuingAuthority) {
        this.id = id;
        this.certificateNumber = certificateNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.status = status != null ? status : HydroTestStatus.VALID;
        this.issuingAuthority = issuingAuthority;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String certificateNumber;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private HydroTestStatus status = HydroTestStatus.VALID;
        private String issuingAuthority;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder certificateNumber(String certificateNumber) {
            this.certificateNumber = certificateNumber;
            return this;
        }

        public Builder issueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder status(HydroTestStatus status) {
            if (status != null) this.status = status;
            return this;
        }

        public Builder issuingAuthority(String issuingAuthority) {
            this.issuingAuthority = issuingAuthority;
            return this;
        }

        public HydroTestCertificateDto build() {
            return new HydroTestCertificateDto(id, certificateNumber, issueDate, expiryDate, status, issuingAuthority);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public HydroTestStatus getStatus() {
        return status;
    }

    public void setStatus(HydroTestStatus status) {
        this.status = status;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }
}
