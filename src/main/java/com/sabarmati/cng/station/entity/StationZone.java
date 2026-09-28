package com.sabarmati.cng.station.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sgl_station_zones")
public class StationZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    private ZoneType zoneType;

    private String name;

    public StationZone() {
    }

    public StationZone(Long id, Station station, ZoneType zoneType, String name) {
        this.id = id;
        this.station = station;
        this.zoneType = zoneType;
        this.name = name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Station station;
        private ZoneType zoneType;
        private String name;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder station(Station station) {
            this.station = station;
            return this;
        }

        public Builder zoneType(ZoneType zoneType) {
            this.zoneType = zoneType;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public StationZone build() {
            return new StationZone(id, station, zoneType, name);
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

    public ZoneType getZoneType() {
        return zoneType;
    }

    public void setZoneType(ZoneType zoneType) {
        this.zoneType = zoneType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
