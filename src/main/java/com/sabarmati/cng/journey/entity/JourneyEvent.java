package com.sabarmati.cng.journey.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sgl_journey_events")
public class JourneyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private VehicleJourney journey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public JourneyEvent() {
    }

    public JourneyEvent(Long id, VehicleJourney journey, EventType eventType, LocalDateTime timestamp, String metadata) {
        this.id = id;
        this.journey = journey;
        this.eventType = eventType;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private VehicleJourney journey;
        private EventType eventType;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String metadata;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder journey(VehicleJourney journey) {
            this.journey = journey;
            return this;
        }

        public Builder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            if (timestamp != null) this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public JourneyEvent build() {
            return new JourneyEvent(id, journey, eventType, timestamp, metadata);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleJourney getJourney() {
        return journey;
    }

    public void setJourney(VehicleJourney journey) {
        this.journey = journey;
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
