package com.sabarmati.cng.anpr.repository;

import com.sabarmati.cng.anpr.entity.AnprDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnprDetectionRepository extends JpaRepository<AnprDetection, Long> {
    List<AnprDetection> findByStationId(Long stationId);
    List<AnprDetection> findByRegistrationNumber(String registrationNumber);
}
