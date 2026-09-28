package com.sabarmati.cng.station.dto;

import com.sabarmati.cng.station.entity.ZoneType;
import jakarta.validation.constraints.NotNull;

public class StationZoneDto {
    private Long id;

    @NotNull(message = "Zone type is required")
    private ZoneType zoneType;

    private String name;

    public StationZoneDto() {
    }

    public StationZoneDto(Long id, ZoneType zoneType, String name) {
        this.id = id;
        this.zoneType = zoneType;
        this.name = name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private ZoneType zoneType;
        private String name;

        public Builder id(Long id) {
            this.id = id;
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

        public StationZoneDto build() {
            return new StationZoneDto(id, zoneType, name);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
