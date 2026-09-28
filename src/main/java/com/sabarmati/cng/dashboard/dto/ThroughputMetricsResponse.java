package com.sabarmati.cng.dashboard.dto;

public class ThroughputMetricsResponse {

    private long completedJourneysToday;
    private long completedJourneysLastHour;
    private long blockedVehiclesToday;

    public ThroughputMetricsResponse() {
    }

    public ThroughputMetricsResponse(long completedJourneysToday, long completedJourneysLastHour, long blockedVehiclesToday) {
        this.completedJourneysToday = completedJourneysToday;
        this.completedJourneysLastHour = completedJourneysLastHour;
        this.blockedVehiclesToday = blockedVehiclesToday;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long completedJourneysToday;
        private long completedJourneysLastHour;
        private long blockedVehiclesToday;

        public Builder completedJourneysToday(long completedJourneysToday) {
            this.completedJourneysToday = completedJourneysToday;
            return this;
        }

        public Builder completedJourneysLastHour(long completedJourneysLastHour) {
            this.completedJourneysLastHour = completedJourneysLastHour;
            return this;
        }

        public Builder blockedVehiclesToday(long blockedVehiclesToday) {
            this.blockedVehiclesToday = blockedVehiclesToday;
            return this;
        }

        public ThroughputMetricsResponse build() {
            return new ThroughputMetricsResponse(completedJourneysToday, completedJourneysLastHour, blockedVehiclesToday);
        }
    }

    public long getCompletedJourneysToday() {
        return completedJourneysToday;
    }

    public void setCompletedJourneysToday(long completedJourneysToday) {
        this.completedJourneysToday = completedJourneysToday;
    }

    public long getCompletedJourneysLastHour() {
        return completedJourneysLastHour;
    }

    public void setCompletedJourneysLastHour(long completedJourneysLastHour) {
        this.completedJourneysLastHour = completedJourneysLastHour;
    }

    public long getBlockedVehiclesToday() {
        return blockedVehiclesToday;
    }

    public void setBlockedVehiclesToday(long blockedVehiclesToday) {
        this.blockedVehiclesToday = blockedVehiclesToday;
    }
}
