package com.sabarmati.cng.audit.repository;

import com.sabarmati.cng.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByStationId(Long stationId);
    Page<AuditLog> findByStationId(Long stationId, Pageable pageable);
    Page<AuditLog> findByActionContainingIgnoreCaseAndEntityTypeContainingIgnoreCase(String action, String entityType, Pageable pageable);
    Page<AuditLog> findByStationIdAndActionContainingIgnoreCaseAndEntityTypeContainingIgnoreCase(Long stationId, String action, String entityType, Pageable pageable);
}
