-- Sabarmati Gas Limited (SGL) Smart CNG Station Database Schema V2
-- Add journey operational lifecycle timestamps

ALTER TABLE sgl_vehicle_journeys
ADD COLUMN IF NOT EXISTS queue_entry_time TIMESTAMP,
ADD COLUMN IF NOT EXISTS fueling_start_time TIMESTAMP,
ADD COLUMN IF NOT EXISTS fueling_end_time TIMESTAMP;
