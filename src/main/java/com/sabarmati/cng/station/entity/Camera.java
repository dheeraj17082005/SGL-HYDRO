package com.sabarmati.cng.station.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sgl_cameras")
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private StationZone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_type", nullable = false)
    private CameraType cameraType;

    @Column(name = "camera_identifier", nullable = false)
    private String cameraIdentifier;

    @Column(nullable = false)
    private Boolean active = true;

    public Camera() {
    }

    public Camera(Long id, Station station, StationZone zone, CameraType cameraType, String cameraIdentifier, Boolean active) {
        this.id = id;
        this.station = station;
        this.zone = zone;
        this.cameraType = cameraType;
        this.cameraIdentifier = cameraIdentifier;
        this.active = active != null ? active : true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Station station;
        private StationZone zone;
        private CameraType cameraType;
        private String cameraIdentifier;
        private Boolean active = true;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder station(Station station) {
            this.station = station;
            return this;
        }

        public Builder zone(StationZone zone) {
            this.zone = zone;
            return this;
        }

        public Builder cameraType(CameraType cameraType) {
            this.cameraType = cameraType;
            return this;
        }

        public Builder cameraIdentifier(String cameraIdentifier) {
            this.cameraIdentifier = cameraIdentifier;
            return this;
        }

        public Builder active(Boolean active) {
            if (active != null) this.active = active;
            return this;
        }

        public Camera build() {
            return new Camera(id, station, zone, cameraType, cameraIdentifier, active);
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

    public StationZone getZone() {
        return zone;
    }

    public void setZone(StationZone zone) {
        this.zone = zone;
    }

    public CameraType getCameraType() {
        return cameraType;
    }

    public void setCameraType(CameraType cameraType) {
        this.cameraType = cameraType;
    }

    public String getCameraIdentifier() {
        return cameraIdentifier;
    }

    public void setCameraIdentifier(String cameraIdentifier) {
        this.cameraIdentifier = cameraIdentifier;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
