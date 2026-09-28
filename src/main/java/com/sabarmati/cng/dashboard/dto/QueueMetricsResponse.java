package com.sabarmati.cng.dashboard.dto;

public class QueueMetricsResponse {

    private long currentQueueLength;
    private long averageWaitingTimeSeconds;
    private long maximumWaitingTimeSeconds;

    public QueueMetricsResponse() {
    }

    public QueueMetricsResponse(long currentQueueLength, long averageWaitingTimeSeconds, long maximumWaitingTimeSeconds) {
        this.currentQueueLength = currentQueueLength;
        this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
        this.maximumWaitingTimeSeconds = maximumWaitingTimeSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long currentQueueLength;
        private long averageWaitingTimeSeconds;
        private long maximumWaitingTimeSeconds;

        public Builder currentQueueLength(long currentQueueLength) {
            this.currentQueueLength = currentQueueLength;
            return this;
        }

        public Builder averageWaitingTimeSeconds(long averageWaitingTimeSeconds) {
            this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
            return this;
        }

        public Builder maximumWaitingTimeSeconds(long maximumWaitingTimeSeconds) {
            this.maximumWaitingTimeSeconds = maximumWaitingTimeSeconds;
            return this;
        }

        public QueueMetricsResponse build() {
            return new QueueMetricsResponse(currentQueueLength, averageWaitingTimeSeconds, maximumWaitingTimeSeconds);
        }
    }

    public long getCurrentQueueLength() {
        return currentQueueLength;
    }

    public void setCurrentQueueLength(long currentQueueLength) {
        this.currentQueueLength = currentQueueLength;
    }

    public long getAverageWaitingTimeSeconds() {
        return averageWaitingTimeSeconds;
    }

    public void setAverageWaitingTimeSeconds(long averageWaitingTimeSeconds) {
        this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
    }

    public long getMaximumWaitingTimeSeconds() {
        return maximumWaitingTimeSeconds;
    }

    public void setMaximumWaitingTimeSeconds(long maximumWaitingTimeSeconds) {
        this.maximumWaitingTimeSeconds = maximumWaitingTimeSeconds;
    }
}
