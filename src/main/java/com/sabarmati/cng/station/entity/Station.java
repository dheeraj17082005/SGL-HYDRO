package com.sabarmati.cng.station.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sgl_stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    private String address;
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StationZone> zones = new ArrayList<>();

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Camera> cameras = new ArrayList<>();

    public Station() {
    }

    public Station(Long id, String name, String code, String address, Double latitude, Double longitude, Boolean active, List<StationZone> zones, List<Camera> cameras) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active != null ? active : true;
        this.zones = zones != null ? zones : new ArrayList<>();
        this.cameras = cameras != null ? cameras : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String code;
        private String address;
        private Double latitude;
        private Double longitude;
        private Boolean active = true;
        private List<StationZone> zones = new ArrayList<>();
        private List<Camera> cameras = new ArrayList<>();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder active(Boolean active) {
            if (active != null) this.active = active;
            return this;
        }

        public Builder zones(List<StationZone> zones) {
            this.zones = zones;
            return this;
        }

        public Builder cameras(List<Camera> cameras) {
            this.cameras = cameras;
            return this;
        }

        public Station build() {
            return new Station(id, name, code, address, latitude, longitude, active, zones, cameras);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<StationZone> getZones() {
        return zones;
    }

    public void setZones(List<StationZone> zones) {
        this.zones = zones;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public void setCameras(List<Camera> cameras) {
        this.cameras = cameras;
    }
}
