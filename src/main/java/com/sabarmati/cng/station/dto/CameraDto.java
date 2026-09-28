package com.sabarmati.cng.station.dto;

import com.sabarmati.cng.station.entity.CameraType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CameraDto {
    private Long id;
    private Long zoneId;

    @NotNull(message = "Camera type is required")
    private CameraType cameraType;

    @NotBlank(message = "Camera identifier is required")
    private String cameraIdentifier;

    private Boolean active = true;

    public CameraDto() {
    }

    public CameraDto(Long id, Long zoneId, CameraType cameraType, String cameraIdentifier, Boolean active) {
        this.id = id;
        this.zoneId = zoneId;
        this.cameraType = cameraType;
        this.cameraIdentifier = cameraIdentifier;
        this.active = active != null ? active : true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long zoneId;
        private CameraType cameraType;
        private String cameraIdentifier;
        private Boolean active = true;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder zoneId(Long zoneId) {
            this.zoneId = zoneId;
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

        public CameraDto build() {
            return new CameraDto(id, zoneId, cameraType, cameraIdentifier, active);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
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
