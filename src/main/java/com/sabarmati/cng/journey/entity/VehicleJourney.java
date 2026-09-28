package com.sabarmati.cng.journey.entity;

import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.vehicle.entity.Vehicle;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sgl_vehicle_journeys")
public class VehicleJourney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime = LocalDateTime.now();

    @Column(name = "queue_entry_time")
    private LocalDateTime queueEntryTime;

    @Column(name = "fueling_start_time")
    private LocalDateTime fuelingStartTime;

    @Column(name = "fueling_end_time")
    private LocalDateTime fuelingEndTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyStatus status = JourneyStatus.ENTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", nullable = false)
    private ComplianceStatus complianceStatus = ComplianceStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_bay_id")
    private FuelingBay assignedBay;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JourneyEvent> events = new ArrayList<>();

    public VehicleJourney() {
    }

    public VehicleJourney(Long id, Station station, Vehicle vehicle, LocalDateTime entryTime, LocalDateTime queueEntryTime, LocalDateTime fuelingStartTime, LocalDateTime fuelingEndTime, LocalDateTime exitTime, JourneyStatus status, ComplianceStatus complianceStatus, FuelingBay assignedBay, List<JourneyEvent> events) {
        this.id = id;
        this.station = station;
        this.vehicle = vehicle;
        this.entryTime = entryTime != null ? entryTime : LocalDateTime.now();
        this.queueEntryTime = queueEntryTime;
        this.fuelingStartTime = fuelingStartTime;
        this.fuelingEndTime = fuelingEndTime;
        this.exitTime = exitTime;
        this.status = status != null ? status : JourneyStatus.ENTERED;
        this.complianceStatus = complianceStatus != null ? complianceStatus : ComplianceStatus.PENDING;
        this.assignedBay = assignedBay;
        this.events = events != null ? events : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Station station;
        private Vehicle vehicle;
        private LocalDateTime entryTime = LocalDateTime.now();
        private LocalDateTime queueEntryTime;
        private LocalDateTime fuelingStartTime;
        private LocalDateTime fuelingEndTime;
        private LocalDateTime exitTime;
        private JourneyStatus status = JourneyStatus.ENTERED;
        private ComplianceStatus complianceStatus = ComplianceStatus.PENDING;
        private FuelingBay assignedBay;
        private List<JourneyEvent> events = new ArrayList<>();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder station(Station station) {
            this.station = station;
            return this;
        }

        public Builder vehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
            return this;
        }

        public Builder entryTime(LocalDateTime entryTime) {
            if (entryTime != null) this.entryTime = entryTime;
            return this;
        }

        public Builder queueEntryTime(LocalDateTime queueEntryTime) {
            this.queueEntryTime = queueEntryTime;
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

        public Builder exitTime(LocalDateTime exitTime) {
            this.exitTime = exitTime;
            return this;
        }

        public Builder status(JourneyStatus status) {
            if (status != null) this.status = status;
            return this;
        }

        public Builder complianceStatus(ComplianceStatus complianceStatus) {
            if (complianceStatus != null) this.complianceStatus = complianceStatus;
            return this;
        }

        public Builder assignedBay(FuelingBay assignedBay) {
            this.assignedBay = assignedBay;
            return this;
        }

        public Builder events(List<JourneyEvent> events) {
            this.events = events;
            return this;
        }

        public VehicleJourney build() {
            return new VehicleJourney(id, station, vehicle, entryTime, queueEntryTime, fuelingStartTime, fuelingEndTime, exitTime, status, complianceStatus, assignedBay, events);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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

    public FuelingBay getAssignedBay() {
        return assignedBay;
    }

    public void setAssignedBay(FuelingBay assignedBay) {
        this.assignedBay = assignedBay;
    }

    public List<JourneyEvent> getEvents() {
        return events;
    }

    public void setEvents(List<JourneyEvent> events) {
        this.events = events;
    }
}
