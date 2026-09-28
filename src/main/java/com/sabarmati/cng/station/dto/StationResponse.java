package com.sabarmati.cng.station.dto;

import java.util.List;

public class StationResponse {

    private Long id;
    private String name;
    private String code;
    private String address;
    private Double latitude;
    private Double longitude;
    private Boolean active;
    private List<StationZoneDto> zones;
    private List<CameraDto> cameras;

    public StationResponse() {
    }

    public StationResponse(Long id, String name, String code, String address, Double latitude, Double longitude, Boolean active, List<StationZoneDto> zones, List<CameraDto> cameras) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active;
        this.zones = zones;
        this.cameras = cameras;
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
        private Boolean active;
        private List<StationZoneDto> zones;
        private List<CameraDto> cameras;

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
            this.active = active;
            return this;
        }

        public Builder zones(List<StationZoneDto> zones) {
            this.zones = zones;
            return this;
        }

        public Builder cameras(List<CameraDto> cameras) {
            this.cameras = cameras;
            return this;
        }

        public StationResponse build() {
            return new StationResponse(id, name, code, address, latitude, longitude, active, zones, cameras);
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

    public List<StationZoneDto> getZones() {
        return zones;
    }

    public void setZones(List<StationZoneDto> zones) {
        this.zones = zones;
    }

    public List<CameraDto> getCameras() {
        return cameras;
    }

    public void setCameras(List<CameraDto> cameras) {
        this.cameras = cameras;
    }
}
