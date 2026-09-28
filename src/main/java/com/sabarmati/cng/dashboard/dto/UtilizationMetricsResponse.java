package com.sabarmati.cng.dashboard.dto;

public class UtilizationMetricsResponse {

    private long totalBays;
    private long availableBays;
    private long occupiedBays;
    private long outOfServiceBays;
    private double utilizationPercentage;

    private long totalDispensers;
    private long availableDispensers;
    private long occupiedDispensers;

    public UtilizationMetricsResponse() {
    }

    public UtilizationMetricsResponse(long totalBays, long availableBays, long occupiedBays, long outOfServiceBays, double utilizationPercentage, long totalDispensers, long availableDispensers, long occupiedDispensers) {
        this.totalBays = totalBays;
        this.availableBays = availableBays;
        this.occupiedBays = occupiedBays;
        this.outOfServiceBays = outOfServiceBays;
        this.utilizationPercentage = utilizationPercentage;
        this.totalDispensers = totalDispensers;
        this.availableDispensers = availableDispensers;
        this.occupiedDispensers = occupiedDispensers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalBays;
        private long availableBays;
        private long occupiedBays;
        private long outOfServiceBays;
        private double utilizationPercentage;
        private long totalDispensers;
        private long availableDispensers;
        private long occupiedDispensers;

        public Builder totalBays(long totalBays) {
            this.totalBays = totalBays;
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

        public Builder outOfServiceBays(long outOfServiceBays) {
            this.outOfServiceBays = outOfServiceBays;
            return this;
        }

        public Builder utilizationPercentage(double utilizationPercentage) {
            this.utilizationPercentage = utilizationPercentage;
            return this;
        }

        public Builder totalDispensers(long totalDispensers) {
            this.totalDispensers = totalDispensers;
            return this;
        }

        public Builder availableDispensers(long availableDispensers) {
            this.availableDispensers = availableDispensers;
            return this;
        }

        public Builder occupiedDispensers(long occupiedDispensers) {
            this.occupiedDispensers = occupiedDispensers;
            return this;
        }

        public UtilizationMetricsResponse build() {
            return new UtilizationMetricsResponse(totalBays, availableBays, occupiedBays, outOfServiceBays, utilizationPercentage, totalDispensers, availableDispensers, occupiedDispensers);
        }
    }

    public long getTotalBays() {
        return totalBays;
    }

    public void setTotalBays(long totalBays) {
        this.totalBays = totalBays;
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

    public long getOutOfServiceBays() {
        return outOfServiceBays;
    }

    public void setOutOfServiceBays(long outOfServiceBays) {
        this.outOfServiceBays = outOfServiceBays;
    }

    public double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(double utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }

    public long getTotalDispensers() {
        return totalDispensers;
    }

    public void setTotalDispensers(long totalDispensers) {
        this.totalDispensers = totalDispensers;
    }

    public long getAvailableDispensers() {
        return availableDispensers;
    }

    public void setAvailableDispensers(long availableDispensers) {
        this.availableDispensers = availableDispensers;
    }

    public long getOccupiedDispensers() {
        return occupiedDispensers;
    }

    public void setOccupiedDispensers(long occupiedDispensers) {
        this.occupiedDispensers = occupiedDispensers;
    }
}
