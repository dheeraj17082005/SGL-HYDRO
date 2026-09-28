package com.sabarmati.cng.alert.service;

import com.sabarmati.cng.alert.entity.Alert;
import com.sabarmati.cng.alert.entity.AlertSeverity;
import com.sabarmati.cng.alert.entity.AlertStatus;
import com.sabarmati.cng.alert.entity.AlertType;
import com.sabarmati.cng.alert.repository.AlertRepository;
import com.sabarmati.cng.audit.service.AuditService;
import com.sabarmati.cng.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AuditService auditService;

    public AlertService(AlertRepository alertRepository, AuditService auditService) {
        this.alertRepository = alertRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Alert createAlert(Long stationId, Long vehicleId, Long journeyId, AlertType type, AlertSeverity severity, String message) {
        Alert alert = Alert.builder()
                .stationId(stationId)
                .vehicleId(vehicleId)
                .journeyId(journeyId)
                .type(type)
                .severity(severity)
                .message(message)
                .status(AlertStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public Page<Alert> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Alert> getAlertsByStation(Long stationId, Pageable pageable) {
        return alertRepository.findByStationId(stationId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Alert> getAlertsByStation(Long stationId) {
        return alertRepository.findByStationId(stationId);
    }

    @Transactional(readOnly = true)
    public Alert getAlertById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
    }

    @Transactional
    public Alert resolveAlert(Long id, String username) {
        Alert alert = getAlertById(id);
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());

        Alert savedAlert = alertRepository.save(alert);

        auditService.logAction(alert.getStationId(), username, "ALERT_RESOLVED", "Alert",
                alert.getId().toString(), "Alert " + alert.getId() + " resolved by user " + username);

        return savedAlert;
    }
}
