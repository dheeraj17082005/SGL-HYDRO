package com.sabarmati.cng.journey.service;

import com.sabarmati.cng.alert.entity.AlertSeverity;
import com.sabarmati.cng.alert.entity.AlertType;
import com.sabarmati.cng.alert.service.AlertService;
import com.sabarmati.cng.audit.service.AuditService;
import com.sabarmati.cng.common.exception.InvalidStateTransitionException;
import com.sabarmati.cng.common.exception.ResourceNotFoundException;
import com.sabarmati.cng.compliance.dto.ComplianceRequest;
import com.sabarmati.cng.compliance.dto.ComplianceResponse;
import com.sabarmati.cng.compliance.dto.ComplianceResult;
import com.sabarmati.cng.compliance.service.ComplianceService;
import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.journey.dto.AssignBayRequest;
import com.sabarmati.cng.journey.dto.CreateJourneyRequest;
import com.sabarmati.cng.journey.dto.JourneyEventDto;
import com.sabarmati.cng.journey.dto.VehicleJourneyResponse;
import com.sabarmati.cng.journey.entity.*;
import com.sabarmati.cng.journey.repository.JourneyEventRepository;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.entity.Vehicle;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JourneyService {

    private final VehicleJourneyRepository vehicleJourneyRepository;
    private final JourneyEventRepository journeyEventRepository;
    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelingBayRepository fuelingBayRepository;
    private final ComplianceService complianceService;
    private final AlertService alertService;
    private final AuditService auditService;

    public JourneyService(VehicleJourneyRepository vehicleJourneyRepository,
                          JourneyEventRepository journeyEventRepository,
                          StationRepository stationRepository,
                          VehicleRepository vehicleRepository,
                          FuelingBayRepository fuelingBayRepository,
                          ComplianceService complianceService,
                          AlertService alertService,
                          AuditService auditService) {
        this.vehicleJourneyRepository = vehicleJourneyRepository;
        this.journeyEventRepository = journeyEventRepository;
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelingBayRepository = fuelingBayRepository;
        this.complianceService = complianceService;
        this.alertService = alertService;
        this.auditService = auditService;
    }

    @Transactional
    public VehicleJourneyResponse createJourney(CreateJourneyRequest request) {
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + request.getStationId()));

        String cleanRegNumber = request.getRegistrationNumber() != null ?
                request.getRegistrationNumber().replaceAll("[^a-zA-Z0-9]", "").toUpperCase() : "";

        Vehicle vehicle = vehicleRepository.findByRegistrationNumber(cleanRegNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not registered with registration number: " + cleanRegNumber));

        VehicleJourney journey = VehicleJourney.builder()
                .station(station)
                .vehicle(vehicle)
                .entryTime(LocalDateTime.now())
                .status(JourneyStatus.ENTERED)
                .complianceStatus(ComplianceStatus.PENDING)
                .events(new ArrayList<>())
                .build();

        // Save initial journey
        journey = vehicleJourneyRepository.save(journey);

        // Record ENTRY_DETECTED event
        JourneyEvent entryEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.ENTRY_DETECTED)
                .timestamp(LocalDateTime.now())
                .metadata("Vehicle entry detected at station " + station.getCode())
                .build();
        journey.getEvents().add(entryEvent);

        // Perform compliance check
        ComplianceResponse complianceResult = complianceService.verifyCompliance(
                ComplianceRequest.builder()
                        .registrationNumber(cleanRegNumber)
                        .stationId(station.getId())
                        .build()
        );

        if (complianceResult.isEligible()) {
            journey.setComplianceStatus(ComplianceStatus.ELIGIBLE);

            JourneyEvent compEvent = JourneyEvent.builder()
                    .journey(journey)
                    .eventType(EventType.COMPLIANCE_APPROVED)
                    .timestamp(LocalDateTime.now())
                    .metadata("Compliance verified successfully")
                    .build();
            journey.getEvents().add(compEvent);

            auditService.logAction(station.getId(), null, "JOURNEY_STARTED", "VehicleJourney",
                    journey.getId().toString(), "Journey started for eligible vehicle " + cleanRegNumber);
        } else {
            journey.setComplianceStatus(ComplianceStatus.NOT_ELIGIBLE);
            journey.setStatus(JourneyStatus.BLOCKED);

            JourneyEvent rejectEvent = JourneyEvent.builder()
                    .journey(journey)
                    .eventType(EventType.COMPLIANCE_REJECTED)
                    .timestamp(LocalDateTime.now())
                    .metadata("Compliance failed: " + complianceResult.getReason())
                    .build();
            journey.getEvents().add(rejectEvent);

            // Generate Alert based on failure reason
            AlertType alertType = mapResultToAlertType(complianceResult.getResult());
            alertService.createAlert(station.getId(), vehicle.getId(), journey.getId(),
                    alertType, AlertSeverity.HIGH, "Non-compliant vehicle attempted fueling: " + complianceResult.getReason());

            auditService.logAction(station.getId(), null, "JOURNEY_BLOCKED", "VehicleJourney",
                    journey.getId().toString(), "Journey blocked due to non-compliance: " + complianceResult.getReason());
        }

        VehicleJourney savedJourney = vehicleJourneyRepository.save(journey);
        return mapToResponse(savedJourney);
    }

    @Transactional
    public VehicleJourneyResponse enterQueue(Long journeyId) {
        VehicleJourney journey = vehicleJourneyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle journey not found with id: " + journeyId));

        if (journey.getStatus() != JourneyStatus.ENTERED) {
            throw new InvalidStateTransitionException("Cannot enter queue from status: " + journey.getStatus());
        }

        if (journey.getComplianceStatus() != ComplianceStatus.ELIGIBLE) {
            throw new InvalidStateTransitionException("Non-compliant vehicle (compliance status: " + journey.getComplianceStatus() + ") cannot enter queue");
        }

        journey.setStatus(JourneyStatus.IN_QUEUE);
        journey.setQueueEntryTime(LocalDateTime.now());

        JourneyEvent queueEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.QUEUE_ENTERED)
                .timestamp(LocalDateTime.now())
                .metadata("Vehicle entered station queue")
                .build();
        journey.getEvents().add(queueEvent);

        VehicleJourney savedJourney = vehicleJourneyRepository.save(journey);
        return mapToResponse(savedJourney);
    }

    @Transactional(readOnly = true)
    public List<VehicleJourneyResponse> getStationQueue(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }
        return vehicleJourneyRepository.findByStationIdAndStatusOrderByQueueEntryTimeAsc(stationId, JourneyStatus.IN_QUEUE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleJourneyResponse assignBay(Long journeyId, AssignBayRequest request) {
        VehicleJourney journey = vehicleJourneyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle journey not found with id: " + journeyId));

        if (journey.getStatus() != JourneyStatus.IN_QUEUE) {
            throw new InvalidStateTransitionException("Cannot assign bay to journey with status: " + journey.getStatus());
        }

        if (journey.getComplianceStatus() != ComplianceStatus.ELIGIBLE) {
            throw new InvalidStateTransitionException("Only ELIGIBLE vehicles can be assigned to a fueling bay");
        }

        FuelingBay bay = fuelingBayRepository.findById(request.getBayId())
                .orElseThrow(() -> new ResourceNotFoundException("Fueling bay not found with id: " + request.getBayId()));

        if (!bay.getStation().getId().equals(journey.getStation().getId())) {
            throw new InvalidStateTransitionException("Fueling bay " + bay.getId() + " does not belong to station " + journey.getStation().getId());
        }

        if (bay.getStatus() != BayStatus.AVAILABLE) {
            throw new InvalidStateTransitionException("Fueling bay " + bay.getBayNumber() + " is currently " + bay.getStatus());
        }

        bay.setStatus(BayStatus.OCCUPIED);
        fuelingBayRepository.save(bay);

        journey.setAssignedBay(bay);
        journey.setStatus(JourneyStatus.BAY_ASSIGNED);

        JourneyEvent bayEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.BAY_ASSIGNED)
                .timestamp(LocalDateTime.now())
                .metadata("Assigned to Bay " + bay.getBayNumber())
                .build();
        journey.getEvents().add(bayEvent);

        VehicleJourney savedJourney = vehicleJourneyRepository.save(journey);
        return mapToResponse(savedJourney);
    }

    @Transactional
    public VehicleJourneyResponse startFueling(Long journeyId) {
        VehicleJourney journey = vehicleJourneyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle journey not found with id: " + journeyId));

        if (journey.getStatus() != JourneyStatus.BAY_ASSIGNED) {
            throw new InvalidStateTransitionException("Cannot start fueling for journey with status: " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.FUELING);
        journey.setFuelingStartTime(LocalDateTime.now());

        JourneyEvent startEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.FUELING_STARTED)
                .timestamp(LocalDateTime.now())
                .metadata("Fueling started at assigned Bay " + (journey.getAssignedBay() != null ? journey.getAssignedBay().getBayNumber() : ""))
                .build();
        journey.getEvents().add(startEvent);

        VehicleJourney savedJourney = vehicleJourneyRepository.save(journey);
        return mapToResponse(savedJourney);
    }

    @Transactional
    public VehicleJourneyResponse completeFueling(Long journeyId) {
        VehicleJourney journey = vehicleJourneyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle journey not found with id: " + journeyId));

        if (journey.getStatus() != JourneyStatus.FUELING) {
            throw new InvalidStateTransitionException("Cannot complete fueling for journey with status: " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.FUELING_COMPLETED);
        journey.setFuelingEndTime(LocalDateTime.now());

        if (journey.getAssignedBay() != null) {
            FuelingBay bay = journey.getAssignedBay();
            bay.setStatus(BayStatus.AVAILABLE);
            fuelingBayRepository.save(bay);
        }

        JourneyEvent completeEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.FUELING_COMPLETED)
                .timestamp(LocalDateTime.now())
                .metadata("Fueling completed successfully")
                .build();
        journey.getEvents().add(completeEvent);

        VehicleJourney savedJourney = vehicleJourneyRepository.save(journey);
        return mapToResponse(savedJourney);
    }

    @Transactional
    public VehicleJourneyResponse exitStation(Long journeyId) {
        VehicleJourney journey = vehicleJourneyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle journey not found with id: " + journeyId));

        if (journey.getStatus() != JourneyStatus.FUELING_COMPLETED) {
            throw new InvalidStateTransitionException("Cannot exit station from journey status: " + journey.getStatus());
        }

        journey.setStatus(JourneyStatus.EXITED);
        journey.setExitTime(LocalDateTime.now());

        JourneyEvent exitEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.EXIT_DETECTED)
                .timestamp(LocalDateTime.now())
                .metadata("Vehicle exited CNG station")
                .build();
        journey.getEvents().add(exitEvent);

        auditService.logAction(journey.getStation().getId(), null, "VEHICLE_EXITED", "VehicleJourney",
                journey.getId().toString(), "Vehicle " + journey.getVehicle().getRegistrationNumber() + " completed journey and exited");

        VehicleJourney savedJourney = vehicleJourneyRepository.save(journey);
        return mapToResponse(savedJourney);
    }

    @Transactional(readOnly = true)
    public VehicleJourneyResponse getJourneyById(Long id) {
        VehicleJourney journey = vehicleJourneyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle journey not found with id: " + id));
        return mapToResponse(journey);
    }

    @Transactional(readOnly = true)
    public List<VehicleJourneyResponse> getJourneysByStation(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }
        return vehicleJourneyRepository.findByStationId(stationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AlertType mapResultToAlertType(ComplianceResult result) {
        return switch (result) {
            case HYDRO_TEST_EXPIRED -> AlertType.EXPIRED_HYDRO_TEST;
            case REGISTRATION_INVALID -> AlertType.INVALID_REGISTRATION;
            case VEHICLE_NOT_FOUND -> AlertType.UNREGISTERED_VEHICLE;
            default -> AlertType.COMPLIANCE_VIOLATION;
        };
    }

    private VehicleJourneyResponse mapToResponse(VehicleJourney journey) {
        List<JourneyEventDto> eventDtos = journey.getEvents() != null ?
                journey.getEvents().stream()
                        .map(e -> JourneyEventDto.builder()
                                .id(e.getId())
                                .eventType(e.getEventType())
                                .timestamp(e.getTimestamp())
                                .metadata(e.getMetadata())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList();

        Long waitingTimeSeconds = null;
        if (journey.getQueueEntryTime() != null) {
            LocalDateTime endQueueTime = journey.getFuelingStartTime() != null ?
                    journey.getFuelingStartTime() : LocalDateTime.now();
            waitingTimeSeconds = Duration.between(journey.getQueueEntryTime(), endQueueTime).getSeconds();
        }

        Long fuelingDurationSeconds = null;
        if (journey.getFuelingStartTime() != null) {
            LocalDateTime endFuelingTime = journey.getFuelingEndTime() != null ?
                    journey.getFuelingEndTime() : LocalDateTime.now();
            fuelingDurationSeconds = Duration.between(journey.getFuelingStartTime(), endFuelingTime).getSeconds();
        }

        return VehicleJourneyResponse.builder()
                .id(journey.getId())
                .stationId(journey.getStation() != null ? journey.getStation().getId() : null)
                .stationName(journey.getStation() != null ? journey.getStation().getName() : null)
                .vehicleId(journey.getVehicle() != null ? journey.getVehicle().getId() : null)
                .registrationNumber(journey.getVehicle() != null ? journey.getVehicle().getRegistrationNumber() : null)
                .ownerName(journey.getVehicle() != null ? journey.getVehicle().getOwnerName() : null)
                .entryTime(journey.getEntryTime())
                .queueEntryTime(journey.getQueueEntryTime())
                .waitingTimeSeconds(waitingTimeSeconds)
                .fuelingStartTime(journey.getFuelingStartTime())
                .fuelingEndTime(journey.getFuelingEndTime())
                .fuelingDurationSeconds(fuelingDurationSeconds)
                .exitTime(journey.getExitTime())
                .status(journey.getStatus())
                .complianceStatus(journey.getComplianceStatus())
                .assignedBayId(journey.getAssignedBay() != null ? journey.getAssignedBay().getId() : null)
                .assignedBayNumber(journey.getAssignedBay() != null ? journey.getAssignedBay().getBayNumber() : null)
                .events(eventDtos)
                .build();
    }
}
