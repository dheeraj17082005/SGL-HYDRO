package com.sabarmati.cng.dashboard.controller;

import com.sabarmati.cng.common.dto.ApiResponse;
import com.sabarmati.cng.dashboard.dto.*;
import com.sabarmati.cng.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stations/{stationId}")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StationDashboardResponse>> getStationDashboard(@PathVariable Long stationId) {
        StationDashboardResponse response = dashboardService.getStationDashboard(stationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Station dashboard metrics fetched successfully"));
    }

    @GetMapping("/metrics/queue")
    public ResponseEntity<ApiResponse<QueueMetricsResponse>> getQueueMetrics(@PathVariable Long stationId) {
        QueueMetricsResponse response = dashboardService.getQueueMetrics(stationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Queue metrics fetched successfully"));
    }

    @GetMapping("/metrics/throughput")
    public ResponseEntity<ApiResponse<ThroughputMetricsResponse>> getThroughputMetrics(@PathVariable Long stationId) {
        ThroughputMetricsResponse response = dashboardService.getThroughputMetrics(stationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Throughput metrics fetched successfully"));
    }

    @GetMapping("/metrics/utilization")
    public ResponseEntity<ApiResponse<UtilizationMetricsResponse>> getUtilizationMetrics(@PathVariable Long stationId) {
        UtilizationMetricsResponse response = dashboardService.getUtilizationMetrics(stationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Utilization metrics fetched successfully"));
    }

    @GetMapping("/metrics/compliance")
    public ResponseEntity<ApiResponse<ComplianceMetricsResponse>> getComplianceMetrics(@PathVariable Long stationId) {
        ComplianceMetricsResponse response = dashboardService.getComplianceMetrics(stationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Compliance metrics fetched successfully"));
    }
}
