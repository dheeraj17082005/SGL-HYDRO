package com.sabarmati.cng.dashboard.dto;

public class StationDashboardResponse {

    private Long stationId;
    private String stationName;
    private long vehiclesInQueue;
    private long vehiclesFueling;
    private long availableBays;
    private long occupiedBays;
    private long completedJourneysToday;
    private long blockedVehiclesToday;
    private long averageWaitingTimeSeconds;
    private long averageFuelingDurationSeconds;

    public StationDashboardResponse() {
    }

    public StationDashboardResponse(Long stationId, String stationName, long vehiclesInQueue, long vehiclesFueling, long availableBays, long occupiedBays, long completedJourneysToday, long blockedVehiclesToday, long averageWaitingTimeSeconds, long averageFuelingDurationSeconds) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.vehiclesInQueue = vehiclesInQueue;
        this.vehiclesFueling = vehiclesFueling;
        this.availableBays = availableBays;
        this.occupiedBays = occupiedBays;
        this.completedJourneysToday = completedJourneysToday;
        this.blockedVehiclesToday = blockedVehiclesToday;
        this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
        this.averageFuelingDurationSeconds = averageFuelingDurationSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long stationId;
        private String stationName;
        private long vehiclesInQueue;
        private long vehiclesFueling;
        private long availableBays;
        private long occupiedBays;
        private long completedJourneysToday;
        private long blockedVehiclesToday;
        private long averageWaitingTimeSeconds;
        private long averageFuelingDurationSeconds;

        public Builder stationId(Long stationId) {
            this.stationId = stationId;
            return this;
        }

        public Builder stationName(String stationName) {
            this.stationName = stationName;
            return this;
        }

        public Builder vehiclesInQueue(long vehiclesInQueue) {
            this.vehiclesInQueue = vehiclesInQueue;
            return this;
        }

        public Builder vehiclesFueling(long vehiclesFueling) {
            this.vehiclesFueling = vehiclesFueling;
            return this;
        }

        public Builder availableBays(long availableBays) {
            this.availableBays = availableBays;
            return this;
        }

        public Builder occupiedBays(long occupiedBays) {
            this.occupiedBays = occupiedBays;
            return this;
        }

        public Builder completedJourneysToday(long completedJourneysToday) {
            this.completedJourneysToday = completedJourneysToday;
            return this;
        }

        public Builder blockedVehiclesToday(long blockedVehiclesToday) {
            this.blockedVehiclesToday = blockedVehiclesToday;
            return this;
        }

        public Builder averageWaitingTimeSeconds(long averageWaitingTimeSeconds) {
            this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
            return this;
        }

        public Builder averageFuelingDurationSeconds(long averageFuelingDurationSeconds) {
            this.averageFuelingDurationSeconds = averageFuelingDurationSeconds;
            return this;
        }

        public StationDashboardResponse build() {
            return new StationDashboardResponse(stationId, stationName, vehiclesInQueue, vehiclesFueling, availableBays, occupiedBays, completedJourneysToday, blockedVehiclesToday, averageWaitingTimeSeconds, averageFuelingDurationSeconds);
        }
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public long getVehiclesInQueue() {
        return vehiclesInQueue;
    }

    public void setVehiclesInQueue(long vehiclesInQueue) {
        this.vehiclesInQueue = vehiclesInQueue;
    }

    public long getVehiclesFueling() {
        return vehiclesFueling;
    }

    public void setVehiclesFueling(long vehiclesFueling) {
        this.vehiclesFueling = vehiclesFueling;
    }

    public long getAvailableBays() {
        return availableBays;
    }

    public void setAvailableBays(long availableBays) {
        this.availableBays = availableBays;
    }

    public long getOccupiedBays() {
        return occupiedBays;
    }

    public void setOccupiedBays(long occupiedBays) {
        this.occupiedBays = occupiedBays;
    }

    public long getCompletedJourneysToday() {
        return completedJourneysToday;
    }

    public void setCompletedJourneysToday(long completedJourneysToday) {
        this.completedJourneysToday = completedJourneysToday;
    }

    public long getBlockedVehiclesToday() {
        return blockedVehiclesToday;
    }

    public void setBlockedVehiclesToday(long blockedVehiclesToday) {
        this.blockedVehiclesToday = blockedVehiclesToday;
    }

    public long getAverageWaitingTimeSeconds() {
        return averageWaitingTimeSeconds;
    }

    public void setAverageWaitingTimeSeconds(long averageWaitingTimeSeconds) {
        this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
    }

    public long getAverageFuelingDurationSeconds() {
        return averageFuelingDurationSeconds;
    }

    public void setAverageFuelingDurationSeconds(long averageFuelingDurationSeconds) {
        this.averageFuelingDurationSeconds = averageFuelingDurationSeconds;
    }
}
