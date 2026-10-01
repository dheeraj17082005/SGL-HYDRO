package com.sabarmati.cng.anpr.service;

import com.sabarmati.cng.alert.entity.AlertSeverity;
import com.sabarmati.cng.alert.entity.AlertType;
import com.sabarmati.cng.alert.service.AlertService;
import com.sabarmati.cng.anpr.dto.AnprDetectionRequest;
import com.sabarmati.cng.anpr.dto.AnprDetectionResponse;
import com.sabarmati.cng.anpr.entity.AnprDetection;
import com.sabarmati.cng.anpr.repository.AnprDetectionRepository;
import com.sabarmati.cng.audit.service.AuditService;
import com.sabarmati.cng.common.exception.InvalidStateTransitionException;
import com.sabarmati.cng.common.exception.ResourceNotFoundException;
import com.sabarmati.cng.compliance.dto.ComplianceRequest;
import com.sabarmati.cng.compliance.dto.ComplianceResponse;
import com.sabarmati.cng.compliance.dto.ComplianceResult;
import com.sabarmati.cng.compliance.service.ComplianceService;
import com.sabarmati.cng.integration.HydroTestVerificationClient;
import com.sabarmati.cng.integration.HydroTestVerificationResult;
import com.sabarmati.cng.integration.VehicleRegistrationClient;
import com.sabarmati.cng.integration.VehicleRegistrationResult;
import com.sabarmati.cng.journey.entity.*;
import com.sabarmati.cng.journey.repository.VehicleJourneyRepository;
import com.sabarmati.cng.station.entity.Camera;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.repository.CameraRepository;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.vehicle.entity.HydroTestCertificate;
import com.sabarmati.cng.vehicle.entity.HydroTestStatus;
import com.sabarmati.cng.vehicle.entity.RegistrationStatus;
import com.sabarmati.cng.vehicle.entity.Vehicle;
import com.sabarmati.cng.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AnprService {

    private static final List<JourneyStatus> ACTIVE_STATUSES = List.of(
            JourneyStatus.ENTERED,
            JourneyStatus.IN_QUEUE,
            JourneyStatus.BAY_ASSIGNED,
            JourneyStatus.FUELING,
            JourneyStatus.FUELING_COMPLETED
    );

    private final AnprDetectionRepository anprDetectionRepository;
    private final StationRepository stationRepository;
    private final CameraRepository cameraRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleJourneyRepository vehicleJourneyRepository;
    private final ComplianceService complianceService;
    private final VehicleRegistrationClient vehicleRegistrationClient;
    private final HydroTestVerificationClient hydroTestVerificationClient;
    private final AlertService alertService;
    private final AuditService auditService;

    public AnprService(AnprDetectionRepository anprDetectionRepository,
                       StationRepository stationRepository,
                       CameraRepository cameraRepository,
                       VehicleRepository vehicleRepository,
                       VehicleJourneyRepository vehicleJourneyRepository,
                       ComplianceService complianceService,
                       VehicleRegistrationClient vehicleRegistrationClient,
                       HydroTestVerificationClient hydroTestVerificationClient,
                       AlertService alertService,
                       AuditService auditService) {
        this.anprDetectionRepository = anprDetectionRepository;
        this.stationRepository = stationRepository;
        this.cameraRepository = cameraRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleJourneyRepository = vehicleJourneyRepository;
        this.complianceService = complianceService;
        this.vehicleRegistrationClient = vehicleRegistrationClient;
        this.hydroTestVerificationClient = hydroTestVerificationClient;
        this.alertService = alertService;
        this.auditService = auditService;
    }

    @Transactional
    public AnprDetectionResponse processDetection(AnprDetectionRequest request) {
        String cleanRegNumber = sanitizeRegistrationNumber(request.getRegistrationNumber());

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + request.getStationId()));

        Camera camera = cameraRepository.findById(request.getCameraId())
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found with id: " + request.getCameraId()));

        if (!camera.getStation().getId().equals(station.getId())) {
            throw new InvalidStateTransitionException("Camera " + camera.getId() + " does not belong to station " + station.getId());
        }

        LocalDateTime detectionTime = request.getDetectedAt() != null ? request.getDetectedAt() : LocalDateTime.now();

        // 1. Record history of ANPR camera detection
        AnprDetection detection = AnprDetection.builder()
                .stationId(station.getId())
                .cameraId(camera.getId())
                .registrationNumber(cleanRegNumber)
                .detectedAt(detectionTime)
                .createdAt(LocalDateTime.now())
                .build();
        anprDetectionRepository.save(detection);

        // 2. Retrieve or create vehicle locally via verification client
        Vehicle vehicle = getOrCreateVehicle(cleanRegNumber);

        // 3. Deduplication Check: if active journey exists, return existing journey
        Optional<VehicleJourney> activeJourneyOpt = vehicleJourneyRepository
                .findFirstByStationIdAndVehicleIdAndStatusInOrderByIdDesc(station.getId(), vehicle.getId(), ACTIVE_STATUSES);

        if (activeJourneyOpt.isPresent()) {
            VehicleJourney activeJourney = activeJourneyOpt.get();
            boolean isEligible = activeJourney.getComplianceStatus() == ComplianceStatus.ELIGIBLE;
            return AnprDetectionResponse.builder()
                    .journeyId(activeJourney.getId())
                    .registrationNumber(cleanRegNumber)
                    .plateDetected(true)
                    .ocrConfidence(0.95)
                    .verificationSource(vehicleRegistrationClient.getProviderName())
                    .registrationVerified(isEligible)
                    .hydroTestVerified(isEligible)
                    .complianceStatus(activeJourney.getComplianceStatus())
                    .journeyStatus(activeJourney.getStatus())
                    .message("Duplicate ANPR detection - returning existing active journey")
                    .build();
        }

        // 4. Create new Vehicle Journey & perform compliance verification
        VehicleJourney journey = VehicleJourney.builder()
                .station(station)
                .vehicle(vehicle)
                .entryTime(detectionTime)
                .events(new ArrayList<>())
                .build();

        journey = vehicleJourneyRepository.save(journey);

        // Add ENTRY_DETECTED event
        JourneyEvent entryEvent = JourneyEvent.builder()
                .journey(journey)
                .eventType(EventType.ENTRY_DETECTED)
                .timestamp(detectionTime)
                .metadata("ANPR camera " + camera.getCameraIdentifier() + " detected plate " + cleanRegNumber)
                .build();
        journey.getEvents().add(entryEvent);

        ComplianceResponse complianceResponse = complianceService.verifyCompliance(
                ComplianceRequest.builder()
                        .registrationNumber(cleanRegNumber)
                        .stationId(station.getId())
                        .build()
        );

        if (complianceResponse.isEligible()) {
            journey.setStatus(JourneyStatus.IN_QUEUE);
            journey.setQueueEntryTime(LocalDateTime.now());
            journey.setComplianceStatus(ComplianceStatus.ELIGIBLE);

            JourneyEvent regEvent = JourneyEvent.builder()
                    .journey(journey)
                    .eventType(EventType.REGISTRATION_VERIFIED)
                    .timestamp(LocalDateTime.now())
                    .metadata("mParivahan registration verified")
                    .build();
            journey.getEvents().add(regEvent);

            JourneyEvent hydroEvent = JourneyEvent.builder()
                    .journey(journey)
                    .eventType(EventType.HYDRO_TEST_VERIFIED)
                    .timestamp(LocalDateTime.now())
                    .metadata("Hydro-test certificate verified")
                    .build();
            journey.getEvents().add(hydroEvent);

            JourneyEvent appEvent = JourneyEvent.builder()
                    .journey(journey)
                    .eventType(EventType.COMPLIANCE_APPROVED)
                    .timestamp(LocalDateTime.now())
                    .metadata("Compliance approved for fueling")
                    .build();
            journey.getEvents().add(appEvent);

            auditService.logAction(station.getId(), null, "ANPR_ENTRY_APPROVED", "VehicleJourney",
                    journey.getId().toString(), "ANPR detection approved for vehicle " + cleanRegNumber);

            vehicleJourneyRepository.save(journey);

            return AnprDetectionResponse.builder()
                    .journeyId(journey.getId())
                    .registrationNumber(cleanRegNumber)
                    .plateDetected(true)
                    .ocrConfidence(0.95)
                    .verificationSource(vehicleRegistrationClient.getProviderName())
                    .registrationVerified(true)
                    .hydroTestVerified(true)
                    .complianceStatus(ComplianceStatus.ELIGIBLE)
                    .journeyStatus(JourneyStatus.IN_QUEUE)
                    .message("Vehicle is eligible and automatically added to fueling queue")
                    .build();
        } else {
            journey.setStatus(JourneyStatus.BLOCKED);
            journey.setComplianceStatus(ComplianceStatus.NOT_ELIGIBLE);

            JourneyEvent rejEvent = JourneyEvent.builder()
                    .journey(journey)
                    .eventType(EventType.COMPLIANCE_REJECTED)
                    .timestamp(LocalDateTime.now())
                    .metadata("Compliance failed: " + complianceResponse.getReason())
                    .build();
            journey.getEvents().add(rejEvent);

            AlertType alertType = mapResultToAlertType(complianceResponse.getResult());
            alertService.createAlert(station.getId(), vehicle.getId(), journey.getId(),
                    alertType, AlertSeverity.HIGH, "ANPR detection non-compliant: " + complianceResponse.getReason());

            auditService.logAction(station.getId(), null, "ANPR_ENTRY_BLOCKED", "VehicleJourney",
                    journey.getId().toString(), "ANPR detection blocked vehicle " + cleanRegNumber + ": " + complianceResponse.getReason());

            vehicleJourneyRepository.save(journey);

            boolean isRegValid = "VALID".equalsIgnoreCase(complianceResponse.getRegistrationStatus());
            boolean isHydroValid = "VALID".equalsIgnoreCase(complianceResponse.getHydroTestStatus());

            return AnprDetectionResponse.builder()
                    .journeyId(journey.getId())
                    .registrationNumber(cleanRegNumber)
                    .plateDetected(true)
                    .ocrConfidence(0.95)
                    .verificationSource(vehicleRegistrationClient.getProviderName())
                    .registrationVerified(isRegValid)
                    .hydroTestVerified(isHydroValid)
                    .complianceStatus(ComplianceStatus.NOT_ELIGIBLE)
                    .journeyStatus(JourneyStatus.BLOCKED)
                    .message("Vehicle compliance failed: " + complianceResponse.getReason())
                    .build();
        }
    }

    private Vehicle getOrCreateVehicle(String cleanRegNumber) {
        return vehicleRepository.findByRegistrationNumber(cleanRegNumber)
                .orElseGet(() -> {
                    VehicleRegistrationResult regResult = vehicleRegistrationClient.verifyRegistration(cleanRegNumber);
                    HydroTestVerificationResult hydroResult = hydroTestVerificationClient.verify(cleanRegNumber);

                    Vehicle v = Vehicle.builder()
                            .registrationNumber(cleanRegNumber)
                            .vehicleType(regResult.getVehicleType() != null ? regResult.getVehicleType() : "AUTO")
                            .ownerName(regResult.getOwnerName() != null ? regResult.getOwnerName() : "Unknown Owner")
                            .registrationStatus(regResult.isValid() ? RegistrationStatus.VALID : RegistrationStatus.INVALID)
                            .registrationExpiry(regResult.getRegistrationExpiry() != null ? regResult.getRegistrationExpiry() : LocalDateTime.now().toLocalDate())
                            .build();

                    if (hydroResult.isFound()) {
                        HydroTestCertificate cert = HydroTestCertificate.builder()
                                .vehicle(v)
                                .certificateNumber(hydroResult.getCertificateNumber() != null ? hydroResult.getCertificateNumber() : "CERT-AUTO")
                                .issueDate(LocalDateTime.now().minusMonths(6).toLocalDate())
                                .expiryDate(hydroResult.getExpiryDate() != null ? hydroResult.getExpiryDate() : LocalDateTime.now().toLocalDate())
                                .status(hydroResult.isValid() ? HydroTestStatus.VALID : HydroTestStatus.EXPIRED)
                                .issuingAuthority(hydroResult.getIssuingAuthority() != null ? hydroResult.getIssuingAuthority() : "PESO Approved")
                                .build();
                        v.setHydroTestCertificate(cert);
                    }

                    return vehicleRepository.save(v);
                });
    }

    private AlertType mapResultToAlertType(ComplianceResult result) {
        return switch (result) {
            case HYDRO_TEST_EXPIRED -> AlertType.EXPIRED_HYDRO_TEST;
            case REGISTRATION_INVALID -> AlertType.INVALID_REGISTRATION;
            case VEHICLE_NOT_FOUND -> AlertType.UNREGISTERED_VEHICLE;
            default -> AlertType.COMPLIANCE_VIOLATION;
        };
    }

    private String sanitizeRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) return "";
        return registrationNumber.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }
}
