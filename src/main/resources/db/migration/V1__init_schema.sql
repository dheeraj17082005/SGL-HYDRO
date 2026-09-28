-- Sabarmati Gas Limited (SGL) Smart CNG Station Database Schema V1

-- 1. STATIONS
CREATE TABLE IF NOT EXISTS sgl_stations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    address VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. STATION ZONES
CREATE TABLE IF NOT EXISTS sgl_station_zones (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    zone_type VARCHAR(50) NOT NULL,
    name VARCHAR(255)
);

-- 3. CAMERAS
CREATE TABLE IF NOT EXISTS sgl_cameras (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    zone_id BIGINT REFERENCES sgl_station_zones(id) ON DELETE SET NULL,
    camera_type VARCHAR(50) NOT NULL,
    camera_identifier VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4. FUELING BAYS
CREATE TABLE IF NOT EXISTS sgl_fueling_bays (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    bay_number INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE'
);

-- 5. DISPENSERS
CREATE TABLE IF NOT EXISTS sgl_dispensers (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    bay_id BIGINT NOT NULL REFERENCES sgl_fueling_bays(id) ON DELETE CASCADE,
    dispenser_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE'
);

-- 6. VEHICLES
CREATE TABLE IF NOT EXISTS sgl_vehicles (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(50) NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    registration_status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    registration_expiry DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. HYDRO TEST CERTIFICATES
CREATE TABLE IF NOT EXISTS sgl_hydro_test_certificates (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES sgl_vehicles(id) ON DELETE CASCADE,
    certificate_number VARCHAR(100) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    issuing_authority VARCHAR(255) NOT NULL
);

-- 8. VEHICLE JOURNEYS
CREATE TABLE IF NOT EXISTS sgl_vehicle_journeys (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES sgl_vehicles(id) ON DELETE CASCADE,
    entry_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ENTERED',
    compliance_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_bay_id BIGINT REFERENCES sgl_fueling_bays(id) ON DELETE SET NULL
);

-- 9. JOURNEY EVENTS
CREATE TABLE IF NOT EXISTS sgl_journey_events (
    id BIGSERIAL PRIMARY KEY,
    journey_id BIGINT NOT NULL REFERENCES sgl_vehicle_journeys(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

-- 10. ALERTS
CREATE TABLE IF NOT EXISTS sgl_alerts (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    vehicle_id BIGINT REFERENCES sgl_vehicles(id) ON DELETE SET NULL,
    journey_id BIGINT REFERENCES sgl_vehicle_journeys(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- 11. AUDIT LOGS
CREATE TABLE IF NOT EXISTS sgl_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT,
    user_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    details TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- MANDATORY INDEXES
CREATE INDEX IF NOT EXISTS idx_vehicle_reg_num ON sgl_vehicles(registration_number);
CREATE INDEX IF NOT EXISTS idx_journey_station_id ON sgl_vehicle_journeys(station_id);
CREATE INDEX IF NOT EXISTS idx_journey_vehicle_id ON sgl_vehicle_journeys(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_journey_entry_time ON sgl_vehicle_journeys(entry_time);
CREATE INDEX IF NOT EXISTS idx_event_journey_id ON sgl_journey_events(journey_id);
CREATE INDEX IF NOT EXISTS idx_alert_station_id ON sgl_alerts(station_id);
CREATE INDEX IF NOT EXISTS idx_alert_status ON sgl_alerts(status);
CREATE INDEX IF NOT EXISTS idx_audit_station_id ON sgl_audit_logs(station_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON sgl_audit_logs(timestamp);
