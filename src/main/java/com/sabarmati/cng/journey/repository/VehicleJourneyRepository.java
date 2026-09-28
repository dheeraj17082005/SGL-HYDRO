package com.sabarmati.cng.journey.repository;

import com.sabarmati.cng.journey.entity.JourneyStatus;
import com.sabarmati.cng.journey.entity.VehicleJourney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleJourneyRepository extends JpaRepository<VehicleJourney, Long> {
    List<VehicleJourney> findByStationId(Long stationId);
    List<VehicleJourney> findByVehicleId(Long vehicleId);
    List<VehicleJourney> findByStationIdAndStatusOrderByQueueEntryTimeAsc(Long stationId, JourneyStatus status);
    Optional<VehicleJourney> findFirstByStationIdAndVehicleIdAndStatusInOrderByIdDesc(Long stationId, Long vehicleId, List<JourneyStatus> statuses);
}
