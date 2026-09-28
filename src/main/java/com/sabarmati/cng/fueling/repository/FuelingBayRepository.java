package com.sabarmati.cng.fueling.repository;

import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelingBayRepository extends JpaRepository<FuelingBay, Long> {
    List<FuelingBay> findByStationId(Long stationId);
    List<FuelingBay> findByStationIdAndStatus(Long stationId, BayStatus status);
}
