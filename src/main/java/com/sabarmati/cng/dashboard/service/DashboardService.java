package com.sabarmati.cng.dashboard.service;

import com.sabarmati.cng.alert.entity.Alert;
import com.sabarmati.cng.alert.entity.AlertType;
import com.sabarmati.cng.alert.repository.AlertRepository;
import com.sabarmati.cng.common.exception.ResourceNotFoundException;
import com.sabarmati.cng.dashboard.dto.*;
import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.Dispenser;
import com.sabarmati.cng.fueling.entity.DispenserStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.DispenserRepository;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.journey.entity.ComplianceStatus;
import com.sabarmati.cng.journey.entity.JourneyStatus;
import com.sabarmati.cng.journey.entity.VehicleJourney;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DashboardService {

    private final StationRepository stationRepository;
    private final VehicleJourneyRepository vehicleJourneyRepository;
    private final FuelingBayRepository fuelingBayRepository;
    private final DispenserRepository dispenserRepository;
    private final AlertRepository alertRepository;

    public DashboardService(StationRepository stationRepository,
                             VehicleJourneyRepository vehicleJourneyRepository,
                             FuelingBayRepository fuelingBayRepository,
                             DispenserRepository dispenserRepository,
                             AlertRepository alertRepository) {
        this.stationRepository = stationRepository;
        this.vehicleJourneyRepository = vehicleJourneyRepository;
        this.fuelingBayRepository = fuelingBayRepository;
        this.dispenserRepository = dispenserRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public StationDashboardResponse getStationDashboard(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

        List<VehicleJourney> journeys = vehicleJourneyRepository.findByStationId(stationId);
        List<FuelingBay> bays = fuelingBayRepository.findByStationId(stationId);

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long vehiclesInQueue = journeys.stream().filter(j -> j.getStatus() == JourneyStatus.IN_QUEUE).count();
        long vehiclesFueling = journeys.stream().filter(j -> j.getStatus() == JourneyStatus.FUELING).count();

        long availableBays = bays.stream().filter(b -> b.getStatus() == BayStatus.AVAILABLE).count();
        long occupiedBays = bays.stream().filter(b -> b.getStatus() == BayStatus.OCCUPIED).count();

        long completedJourneysToday = journeys.stream()
                .filter(j -> (j.getStatus() == JourneyStatus.FUELING_COMPLETED || j.getStatus() == JourneyStatus.EXITED) &&
                        j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        long blockedVehiclesToday = journeys.stream()
                .filter(j -> j.getStatus() == JourneyStatus.BLOCKED &&
                        j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        long avgWaitingTime = (long) journeys.stream()
                .filter(j -> j.getQueueEntryTime() != null)
                .mapToLong(j -> {
                    LocalDateTime endQueue = j.getFuelingStartTime() != null ? j.getFuelingStartTime() : LocalDateTime.now();
                    return Duration.between(j.getQueueEntryTime(), endQueue).getSeconds();
                })
                .average()
                .orElse(0.0);

        long avgFuelingDuration = (long) journeys.stream()
                .filter(j -> j.getFuelingStartTime() != null)
                .mapToLong(j -> {
                    LocalDateTime endFueling = j.getFuelingEndTime() != null ? j.getFuelingEndTime() : LocalDateTime.now();
                    return Duration.between(j.getFuelingStartTime(), endFueling).getSeconds();
                })
                .average()
                .orElse(0.0);

        return StationDashboardResponse.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .vehiclesInQueue(vehiclesInQueue)
                .vehiclesFueling(vehiclesFueling)
                .availableBays(availableBays)
                .occupiedBays(occupiedBays)
                .completedJourneysToday(completedJourneysToday)
                .blockedVehiclesToday(blockedVehiclesToday)
                .averageWaitingTimeSeconds(avgWaitingTime)
                .averageFuelingDurationSeconds(avgFuelingDuration)
                .build();
    }

    @Transactional(readOnly = true)
    public QueueMetricsResponse getQueueMetrics(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }

        List<VehicleJourney> journeys = vehicleJourneyRepository.findByStationId(stationId);

        List<VehicleJourney> queuedJourneys = journeys.stream()
                .filter(j -> j.getStatus() == JourneyStatus.IN_QUEUE)
                .toList();

        long currentQueueLength = queuedJourneys.size();

        long avgWait = (long) queuedJourneys.stream()
                .filter(j -> j.getQueueEntryTime() != null)
                .mapToLong(j -> Duration.between(j.getQueueEntryTime(), LocalDateTime.now()).getSeconds())
                .average()
                .orElse(0.0);

        long maxWait = queuedJourneys.stream()
                .filter(j -> j.getQueueEntryTime() != null)
                .mapToLong(j -> Duration.between(j.getQueueEntryTime(), LocalDateTime.now()).getSeconds())
                .max()
                .orElse(0L);

        return QueueMetricsResponse.builder()
                .currentQueueLength(currentQueueLength)
                .averageWaitingTimeSeconds(avgWait)
                .maximumWaitingTimeSeconds(maxWait)
                .build();
    }

    @Transactional(readOnly = true)
    public ThroughputMetricsResponse getThroughputMetrics(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }

        List<VehicleJourney> journeys = vehicleJourneyRepository.findByStationId(stationId);

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        long completedToday = journeys.stream()
                .filter(j -> (j.getStatus() == JourneyStatus.FUELING_COMPLETED || j.getStatus() == JourneyStatus.EXITED) &&
                        j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        long completedLastHour = journeys.stream()
                .filter(j -> (j.getStatus() == JourneyStatus.FUELING_COMPLETED || j.getStatus() == JourneyStatus.EXITED) &&
                        j.getFuelingEndTime() != null && j.getFuelingEndTime().isAfter(oneHourAgo))
                .count();

        long blockedToday = journeys.stream()
                .filter(j -> j.getStatus() == JourneyStatus.BLOCKED &&
                        j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        return ThroughputMetricsResponse.builder()
                .completedJourneysToday(completedToday)
                .completedJourneysLastHour(completedLastHour)
                .blockedVehiclesToday(blockedToday)
                .build();
    }

    @Transactional(readOnly = true)
    public UtilizationMetricsResponse getUtilizationMetrics(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }

        List<FuelingBay> bays = fuelingBayRepository.findByStationId(stationId);
        List<Dispenser> dispensers = dispenserRepository.findByStationId(stationId);

        long totalBays = bays.size();
        long availableBays = bays.stream().filter(b -> b.getStatus() == BayStatus.AVAILABLE).count();
        long occupiedBays = bays.stream().filter(b -> b.getStatus() == BayStatus.OCCUPIED).count();
        long outOfServiceBays = bays.stream().filter(b -> b.getStatus() == BayStatus.OUT_OF_SERVICE).count();

        double utilizationPercentage = totalBays > 0 ? ((double) occupiedBays / totalBays) * 100.0 : 0.0;

        long totalDispensers = dispensers.size();
        long availableDispensers = dispensers.stream().filter(d -> d.getStatus() == DispenserStatus.AVAILABLE).count();
        long occupiedDispensers = dispensers.stream().filter(d -> d.getStatus() == DispenserStatus.OCCUPIED).count();

        return UtilizationMetricsResponse.builder()
                .totalBays(totalBays)
                .availableBays(availableBays)
                .occupiedBays(occupiedBays)
                .outOfServiceBays(outOfServiceBays)
                .utilizationPercentage(Math.round(utilizationPercentage * 100.0) / 100.0)
                .totalDispensers(totalDispensers)
                .availableDispensers(availableDispensers)
                .occupiedDispensers(occupiedDispensers)
                .build();
    }

    @Transactional(readOnly = true)
    public ComplianceMetricsResponse getComplianceMetrics(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }

        List<VehicleJourney> journeys = vehicleJourneyRepository.findByStationId(stationId);
        List<Alert> alerts = alertRepository.findByStationId(stationId);

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long totalProcessedToday = journeys.stream()
                .filter(j -> j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        long eligibleVehicles = journeys.stream()
                .filter(j -> j.getComplianceStatus() == ComplianceStatus.ELIGIBLE &&
                        j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        long blockedVehicles = journeys.stream()
                .filter(j -> j.getComplianceStatus() == ComplianceStatus.NOT_ELIGIBLE &&
                        j.getEntryTime() != null && j.getEntryTime().isAfter(startOfDay))
                .count();

        long expiredHydroTest = alerts.stream().filter(a -> a.getType() == AlertType.EXPIRED_HYDRO_TEST).count();
        long invalidRegistration = alerts.stream().filter(a -> a.getType() == AlertType.INVALID_REGISTRATION || a.getType() == AlertType.UNREGISTERED_VEHICLE).count();
        long missingHydroTest = alerts.stream().filter(a -> a.getType() == AlertType.COMPLIANCE_VIOLATION).count();

        return ComplianceMetricsResponse.builder()
                .totalVehiclesProcessedToday(totalProcessedToday)
                .eligibleVehicles(eligibleVehicles)
                .blockedVehicles(blockedVehicles)
                .expiredHydroTestCount(expiredHydroTest)
                .invalidRegistrationCount(invalidRegistration)
                .missingHydroTestCount(missingHydroTest)
                .build();
    }
}
