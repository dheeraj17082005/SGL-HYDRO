package com.sabarmati.cng.station.repository;

import com.sabarmati.cng.station.entity.StationZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationZoneRepository extends JpaRepository<StationZone, Long> {
    List<StationZone> findByStationId(Long stationId);
}
