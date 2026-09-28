package com.sabarmati.cng.fueling.repository;

import com.sabarmati.cng.fueling.entity.Dispenser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispenserRepository extends JpaRepository<Dispenser, Long> {
    List<Dispenser> findByStationId(Long stationId);
    List<Dispenser> findByBayId(Long bayId);
}
