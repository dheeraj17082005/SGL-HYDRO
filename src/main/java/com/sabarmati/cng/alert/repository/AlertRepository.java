package com.sabarmati.cng.alert.repository;

import com.sabarmati.cng.alert.entity.Alert;
import com.sabarmati.cng.alert.entity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStationId(Long stationId);
    Page<Alert> findByStationId(Long stationId, Pageable pageable);
    List<Alert> findByStatus(AlertStatus status);
}
