package com.sabarmati.cng.alert.controller;

import com.sabarmati.cng.alert.entity.Alert;
import com.sabarmati.cng.alert.service.AlertService;
import com.sabarmati.cng.common.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Page<Alert>>> getAllAlerts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Alert> alerts = alertService.getAllAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts, "Alerts fetched successfully"));
    }

    @GetMapping("/stations/{stationId}/alerts")
    public ResponseEntity<ApiResponse<Page<Alert>>> getAlertsByStation(
            @PathVariable Long stationId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Alert> alerts = alertService.getAlertsByStation(stationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts, "Station alerts fetched successfully"));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<ApiResponse<Alert>> getAlertById(@PathVariable Long id) {
        Alert alert = alertService.getAlertById(id);
        return ResponseEntity.ok(ApiResponse.success(alert, "Alert fetched successfully"));
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<ApiResponse<Alert>> resolveAlert(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "system";
        Alert resolvedAlert = alertService.resolveAlert(id, username);
        return ResponseEntity.ok(ApiResponse.success(resolvedAlert, "Alert resolved successfully"));
    }
}
