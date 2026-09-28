package com.sabarmati.cng.dashboard.dto;

public class ComplianceMetricsResponse {

    private long totalVehiclesProcessedToday;
    private long eligibleVehicles;
    private long blockedVehicles;
    private long expiredHydroTestCount;
    private long invalidRegistrationCount;
    private long missingHydroTestCount;

    public ComplianceMetricsResponse() {
    }

    public ComplianceMetricsResponse(long totalVehiclesProcessedToday, long eligibleVehicles, long blockedVehicles, long expiredHydroTestCount, long invalidRegistrationCount, long missingHydroTestCount) {
        this.totalVehiclesProcessedToday = totalVehiclesProcessedToday;
        this.eligibleVehicles = eligibleVehicles;
        this.blockedVehicles = blockedVehicles;
        this.expiredHydroTestCount = expiredHydroTestCount;
        this.invalidRegistrationCount = invalidRegistrationCount;
        this.missingHydroTestCount = missingHydroTestCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalVehiclesProcessedToday;
        private long eligibleVehicles;
        private long blockedVehicles;
        private long expiredHydroTestCount;
        private long invalidRegistrationCount;
        private long missingHydroTestCount;

        public Builder totalVehiclesProcessedToday(long totalVehiclesProcessedToday) {
            this.totalVehiclesProcessedToday = totalVehiclesProcessedToday;
            return this;
        }

        public Builder eligibleVehicles(long eligibleVehicles) {
            this.eligibleVehicles = eligibleVehicles;
            return this;
        }

        public Builder blockedVehicles(long blockedVehicles) {
            this.blockedVehicles = blockedVehicles;
            return this;
        }

        public Builder expiredHydroTestCount(long expiredHydroTestCount) {
            this.expiredHydroTestCount = expiredHydroTestCount;
            return this;
        }

        public Builder invalidRegistrationCount(long invalidRegistrationCount) {
            this.invalidRegistrationCount = invalidRegistrationCount;
            return this;
        }

        public Builder missingHydroTestCount(long missingHydroTestCount) {
            this.missingHydroTestCount = missingHydroTestCount;
            return this;
        }

        public ComplianceMetricsResponse build() {
            return new ComplianceMetricsResponse(totalVehiclesProcessedToday, eligibleVehicles, blockedVehicles, expiredHydroTestCount, invalidRegistrationCount, missingHydroTestCount);
        }
    }

    public long getTotalVehiclesProcessedToday() {
        return totalVehiclesProcessedToday;
    }

    public void setTotalVehiclesProcessedToday(long totalVehiclesProcessedToday) {
        this.totalVehiclesProcessedToday = totalVehiclesProcessedToday;
    }

    public long getEligibleVehicles() {
        return eligibleVehicles;
    }

    public void setEligibleVehicles(long eligibleVehicles) {
        this.eligibleVehicles = eligibleVehicles;
    }

    public long getBlockedVehicles() {
        return blockedVehicles;
    }

    public void setBlockedVehicles(long blockedVehicles) {
        this.blockedVehicles = blockedVehicles;
    }

    public long getExpiredHydroTestCount() {
        return expiredHydroTestCount;
    }

    public void setExpiredHydroTestCount(long expiredHydroTestCount) {
        this.expiredHydroTestCount = expiredHydroTestCount;
    }

    public long getInvalidRegistrationCount() {
        return invalidRegistrationCount;
    }

    public void setInvalidRegistrationCount(long invalidRegistrationCount) {
        this.invalidRegistrationCount = invalidRegistrationCount;
    }

    public long getMissingHydroTestCount() {
        return missingHydroTestCount;
    }

    public void setMissingHydroTestCount(long missingHydroTestCount) {
        this.missingHydroTestCount = missingHydroTestCount;
    }
}
