package com.sabarmati.cng.fueling.entity;

import com.sabarmati.cng.station.entity.Station;
import jakarta.persistence.*;

@Entity
@Table(name = "sgl_fueling_bays")
public class FuelingBay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "bay_number", nullable = false)
    private Integer bayNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BayStatus status = BayStatus.AVAILABLE;

    public FuelingBay() {
    }

    public FuelingBay(Long id, Station station, Integer bayNumber, BayStatus status) {
        this.id = id;
        this.station = station;
        this.bayNumber = bayNumber;
        this.status = status != null ? status : BayStatus.AVAILABLE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Station station;
        private Integer bayNumber;
        private BayStatus status = BayStatus.AVAILABLE;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder station(Station station) {
            this.station = station;
            return this;
        }

        public Builder bayNumber(Integer bayNumber) {
            this.bayNumber = bayNumber;
            return this;
        }

        public Builder status(BayStatus status) {
            if (status != null) this.status = status;
            return this;
        }

        public FuelingBay build() {
            return new FuelingBay(id, station, bayNumber, status);
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

    public Integer getBayNumber() {
        return bayNumber;
    }

    public void setBayNumber(Integer bayNumber) {
        this.bayNumber = bayNumber;
    }

    public BayStatus getStatus() {
        return status;
    }

    public void setStatus(BayStatus status) {
        this.status = status;
    }
}
