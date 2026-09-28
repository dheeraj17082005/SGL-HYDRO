package com.sabarmati.cng.audit.controller;

import com.sabarmati.cng.audit.entity.AuditLog;
import com.sabarmati.cng.audit.service.AuditService;
import com.sabarmati.cng.common.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getAuditLogs(action, entityType, pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs, "Audit logs fetched successfully"));
    }

    @GetMapping("/stations/{stationId}/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getStationAuditLogs(
            @PathVariable Long stationId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLog> auditLogs = auditService.getStationAuditLogs(stationId, action, entityType, pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs, "Station audit logs fetched successfully"));
    }
}
