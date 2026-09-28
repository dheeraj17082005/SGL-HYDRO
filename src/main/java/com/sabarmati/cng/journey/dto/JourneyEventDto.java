package com.sabarmati.cng.journey.dto;

import com.sabarmati.cng.journey.entity.EventType;

import java.time.LocalDateTime;

public class JourneyEventDto {

    private Long id;
    private EventType eventType;
    private LocalDateTime timestamp;
    private String metadata;

    public JourneyEventDto() {
    }

    public JourneyEventDto(Long id, EventType eventType, LocalDateTime timestamp, String metadata) {
        this.id = id;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private EventType eventType;
        private LocalDateTime timestamp;
        private String metadata;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public JourneyEventDto build() {
            return new JourneyEventDto(id, eventType, timestamp, metadata);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
