package com.sabarmati.cng.vehicle.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sgl_vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    private RegistrationStatus registrationStatus = RegistrationStatus.VALID;

    @Column(name = "registration_expiry", nullable = false)
    private LocalDate registrationExpiry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToOne(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private HydroTestCertificate hydroTestCertificate;

    public Vehicle() {
    }

    public Vehicle(Long id, String registrationNumber, String vehicleType, String ownerName, RegistrationStatus registrationStatus, LocalDate registrationExpiry, LocalDateTime createdAt, LocalDateTime updatedAt, HydroTestCertificate hydroTestCertificate) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.registrationStatus = registrationStatus != null ? registrationStatus : RegistrationStatus.VALID;
        this.registrationExpiry = registrationExpiry;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
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
        private RegistrationStatus registrationStatus = RegistrationStatus.VALID;
        private LocalDate registrationExpiry;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private HydroTestCertificate hydroTestCertificate;

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
            if (registrationStatus != null) this.registrationStatus = registrationStatus;
            return this;
        }

        public Builder registrationExpiry(LocalDate registrationExpiry) {
            this.registrationExpiry = registrationExpiry;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            if (createdAt != null) this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            if (updatedAt != null) this.updatedAt = updatedAt;
            return this;
        }

        public Builder hydroTestCertificate(HydroTestCertificate hydroTestCertificate) {
            this.hydroTestCertificate = hydroTestCertificate;
            return this;
        }

        public Vehicle build() {
            return new Vehicle(id, registrationNumber, vehicleType, ownerName, registrationStatus, registrationExpiry, createdAt, updatedAt, hydroTestCertificate);
        }
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
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

    public HydroTestCertificate getHydroTestCertificate() {
        return hydroTestCertificate;
    }

    public void setHydroTestCertificate(HydroTestCertificate hydroTestCertificate) {
        this.hydroTestCertificate = hydroTestCertificate;
    }
}
