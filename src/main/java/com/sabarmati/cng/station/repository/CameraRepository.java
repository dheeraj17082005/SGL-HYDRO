package com.sabarmati.cng.station.repository;

import com.sabarmati.cng.station.entity.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {
    List<Camera> findByStationId(Long stationId);
}
