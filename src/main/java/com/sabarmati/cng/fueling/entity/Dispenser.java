package com.sabarmati.cng.fueling.entity;

import com.sabarmati.cng.station.entity.Station;
import jakarta.persistence.*;

@Entity
@Table(name = "sgl_dispensers")
public class Dispenser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bay_id", nullable = false)
    private FuelingBay bay;

    @Column(name = "dispenser_number", nullable = false)
    private String dispenserNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DispenserStatus status = DispenserStatus.AVAILABLE;

    public Dispenser() {
    }

    public Dispenser(Long id, Station station, FuelingBay bay, String dispenserNumber, DispenserStatus status) {
        this.id = id;
        this.station = station;
        this.bay = bay;
        this.dispenserNumber = dispenserNumber;
        this.status = status != null ? status : DispenserStatus.AVAILABLE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Station station;
        private FuelingBay bay;
        private String dispenserNumber;
        private DispenserStatus status = DispenserStatus.AVAILABLE;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder station(Station station) {
            this.station = station;
            return this;
        }

        public Builder bay(FuelingBay bay) {
            this.bay = bay;
            return this;
        }

        public Builder dispenserNumber(String dispenserNumber) {
            this.dispenserNumber = dispenserNumber;
            return this;
        }

        public Builder status(DispenserStatus status) {
            if (status != null) this.status = status;
            return this;
        }

        public Dispenser build() {
            return new Dispenser(id, station, bay, dispenserNumber, status);
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

    public FuelingBay getBay() {
        return bay;
    }

    public void setBay(FuelingBay bay) {
        this.bay = bay;
    }

    public String getDispenserNumber() {
        return dispenserNumber;
    }

    public void setDispenserNumber(String dispenserNumber) {
        this.dispenserNumber = dispenserNumber;
    }

    public DispenserStatus getStatus() {
        return status;
    }

    public void setStatus(DispenserStatus status) {
        this.status = status;
    }
}
