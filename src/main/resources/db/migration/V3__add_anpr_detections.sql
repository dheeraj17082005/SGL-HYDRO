-- Sabarmati Gas Limited (SGL) Smart CNG Station Database Schema V3
-- Create ANPR detections history table and indexes

CREATE TABLE IF NOT EXISTS sgl_anpr_detections (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES sgl_stations(id) ON DELETE CASCADE,
    camera_id BIGINT NOT NULL REFERENCES sgl_cameras(id) ON DELETE CASCADE,
    registration_number VARCHAR(50) NOT NULL,
    detected_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_anpr_station_id ON sgl_anpr_detections(station_id);
CREATE INDEX IF NOT EXISTS idx_anpr_camera_id ON sgl_anpr_detections(camera_id);
CREATE INDEX IF NOT EXISTS idx_anpr_reg_num ON sgl_anpr_detections(registration_number);
CREATE INDEX IF NOT EXISTS idx_anpr_detected_at ON sgl_anpr_detections(detected_at);
