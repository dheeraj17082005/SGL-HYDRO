package com.sabarmati.cng.journey.dto;

import com.sabarmati.cng.journey.entity.ComplianceStatus;
import com.sabarmati.cng.journey.entity.JourneyStatus;

import java.time.LocalDateTime;
import java.util.List;

public class VehicleJourneyResponse {

    private Long id;
    private Long stationId;
    private String stationName;
    private Long vehicleId;
    private String registrationNumber;
    private String ownerName;
    private LocalDateTime entryTime;
    private LocalDateTime queueEntryTime;
    private Long waitingTimeSeconds;
    private LocalDateTime fuelingStartTime;
    private LocalDateTime fuelingEndTime;
    private Long fuelingDurationSeconds;
    private LocalDateTime exitTime;
    private JourneyStatus status;
    private ComplianceStatus complianceStatus;
    private Long assignedBayId;
    private Integer assignedBayNumber;
    private List<JourneyEventDto> events;

    public VehicleJourneyResponse() {
    }

    public VehicleJourneyResponse(Long id, Long stationId, String stationName, Long vehicleId, String registrationNumber, String ownerName, LocalDateTime entryTime, LocalDateTime queueEntryTime, Long waitingTimeSeconds, LocalDateTime fuelingStartTime, LocalDateTime fuelingEndTime, Long fuelingDurationSeconds, LocalDateTime exitTime, JourneyStatus status, ComplianceStatus complianceStatus, Long assignedBayId, Integer assignedBayNumber, List<JourneyEventDto> events) {
        this.id = id;
        this.stationId = stationId;
        this.stationName = stationName;
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber;
        this.ownerName = ownerName;
        this.entryTime = entryTime;
        this.queueEntryTime = queueEntryTime;
        this.waitingTimeSeconds = waitingTimeSeconds;
        this.fuelingStartTime = fuelingStartTime;
        this.fuelingEndTime = fuelingEndTime;
        this.fuelingDurationSeconds = fuelingDurationSeconds;
        this.exitTime = exitTime;
        this.status = status;
        this.complianceStatus = complianceStatus;
        this.assignedBayId = assignedBayId;
        this.assignedBayNumber = assignedBayNumber;
        this.events = events;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long stationId;
        private String stationName;
        private Long vehicleId;
        private String registrationNumber;
        private String ownerName;
        private LocalDateTime entryTime;
        private LocalDateTime queueEntryTime;
        private Long waitingTimeSeconds;
        private LocalDateTime fuelingStartTime;
        private LocalDateTime fuelingEndTime;
        private Long fuelingDurationSeconds;
        private LocalDateTime exitTime;
        private JourneyStatus status;
        private ComplianceStatus complianceStatus;
        private Long assignedBayId;
        private Integer assignedBayNumber;
        private List<JourneyEventDto> events;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder stationId(Long stationId) {
            this.stationId = stationId;
            return this;
        }

        public Builder stationName(String stationName) {
            this.stationName = stationName;
            return this;
        }

        public Builder vehicleId(Long vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public Builder entryTime(LocalDateTime entryTime) {
            this.entryTime = entryTime;
            return this;
        }

        public Builder queueEntryTime(LocalDateTime queueEntryTime) {
            this.queueEntryTime = queueEntryTime;
            return this;
        }

        public Builder waitingTimeSeconds(Long waitingTimeSeconds) {
            this.waitingTimeSeconds = waitingTimeSeconds;
            return this;
        }

        public Builder fuelingStartTime(LocalDateTime fuelingStartTime) {
            this.fuelingStartTime = fuelingStartTime;
            return this;
        }

        public Builder fuelingEndTime(LocalDateTime fuelingEndTime) {
            this.fuelingEndTime = fuelingEndTime;
            return this;
        }

        public Builder fuelingDurationSeconds(Long fuelingDurationSeconds) {
            this.fuelingDurationSeconds = fuelingDurationSeconds;
            return this;
        }

        public Builder exitTime(LocalDateTime exitTime) {
            this.exitTime = exitTime;
            return this;
        }

        public Builder status(JourneyStatus status) {
            this.status = status;
            return this;
        }

        public Builder complianceStatus(ComplianceStatus complianceStatus) {
            this.complianceStatus = complianceStatus;
            return this;
        }

        public Builder assignedBayId(Long assignedBayId) {
            this.assignedBayId = assignedBayId;
            return this;
        }

        public Builder assignedBayNumber(Integer assignedBayNumber) {
            this.assignedBayNumber = assignedBayNumber;
            return this;
        }

        public Builder events(List<JourneyEventDto> events) {
            this.events = events;
            return this;
        }

        public VehicleJourneyResponse build() {
            return new VehicleJourneyResponse(id, stationId, stationName, vehicleId, registrationNumber, ownerName, entryTime, queueEntryTime, waitingTimeSeconds, fuelingStartTime, fuelingEndTime, fuelingDurationSeconds, exitTime, status, complianceStatus, assignedBayId, assignedBayNumber, events);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getQueueEntryTime() {
        return queueEntryTime;
    }

    public void setQueueEntryTime(LocalDateTime queueEntryTime) {
        this.queueEntryTime = queueEntryTime;
    }

    public Long getWaitingTimeSeconds() {
        return waitingTimeSeconds;
    }

    public void setWaitingTimeSeconds(Long waitingTimeSeconds) {
        this.waitingTimeSeconds = waitingTimeSeconds;
    }

    public LocalDateTime getFuelingStartTime() {
        return fuelingStartTime;
    }

    public void setFuelingStartTime(LocalDateTime fuelingStartTime) {
        this.fuelingStartTime = fuelingStartTime;
    }

    public LocalDateTime getFuelingEndTime() {
        return fuelingEndTime;
    }

    public void setFuelingEndTime(LocalDateTime fuelingEndTime) {
        this.fuelingEndTime = fuelingEndTime;
    }

    public Long getFuelingDurationSeconds() {
        return fuelingDurationSeconds;
    }

    public void setFuelingDurationSeconds(Long fuelingDurationSeconds) {
        this.fuelingDurationSeconds = fuelingDurationSeconds;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public JourneyStatus getStatus() {
        return status;
    }

    public void setStatus(JourneyStatus status) {
        this.status = status;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public Long getAssignedBayId() {
        return assignedBayId;
    }

    public void setAssignedBayId(Long assignedBayId) {
        this.assignedBayId = assignedBayId;
    }

    public Integer getAssignedBayNumber() {
        return assignedBayNumber;
    }

    public void setAssignedBayNumber(Integer assignedBayNumber) {
        this.assignedBayNumber = assignedBayNumber;
    }

    public List<JourneyEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<JourneyEventDto> events) {
        this.events = events;
    }
}
