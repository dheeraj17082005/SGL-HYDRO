package com.sabarmati.cng.audit.service;

import com.sabarmati.cng.audit.entity.AuditLog;
import com.sabarmati.cng.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(Long stationId, String userId, String action, String entityType, String entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .stationId(stationId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String action, String entityType, Pageable pageable) {
        String filterAction = action != null ? action : "";
        String filterEntityType = entityType != null ? entityType : "";
        return auditLogRepository.findByActionContainingIgnoreCaseAndEntityTypeContainingIgnoreCase(filterAction, filterEntityType, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getStationAuditLogs(Long stationId, String action, String entityType, Pageable pageable) {
        String filterAction = action != null ? action : "";
        String filterEntityType = entityType != null ? entityType : "";
        return auditLogRepository.findByStationIdAndActionContainingIgnoreCaseAndEntityTypeContainingIgnoreCase(stationId, filterAction, filterEntityType, pageable);
    }
}
