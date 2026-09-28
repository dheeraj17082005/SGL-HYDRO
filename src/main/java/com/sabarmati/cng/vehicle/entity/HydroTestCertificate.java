package com.sabarmati.cng.vehicle.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "sgl_hydro_test_certificates")
public class HydroTestCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "certificate_number", nullable = false)
    private String certificateNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HydroTestStatus status = HydroTestStatus.VALID;

    @Column(name = "issuing_authority", nullable = false)
    private String issuingAuthority;

    public HydroTestCertificate() {
    }

    public HydroTestCertificate(Long id, Vehicle vehicle, String certificateNumber, LocalDate issueDate, LocalDate expiryDate, HydroTestStatus status, String issuingAuthority) {
        this.id = id;
        this.vehicle = vehicle;
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
        private Vehicle vehicle;
        private String certificateNumber;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private HydroTestStatus status = HydroTestStatus.VALID;
        private String issuingAuthority;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder vehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
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

        public HydroTestCertificate build() {
            return new HydroTestCertificate(id, vehicle, certificateNumber, issueDate, expiryDate, status, issuingAuthority);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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
