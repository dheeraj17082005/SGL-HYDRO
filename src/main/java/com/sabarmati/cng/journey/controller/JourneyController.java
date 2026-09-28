package com.sabarmati.cng.journey.controller;

import com.sabarmati.cng.common.dto.ApiResponse;
import com.sabarmati.cng.journey.dto.AssignBayRequest;
import com.sabarmati.cng.journey.dto.CreateJourneyRequest;
import com.sabarmati.cng.journey.dto.VehicleJourneyResponse;
import com.sabarmati.cng.journey.service.JourneyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @PostMapping("/api/v1/journeys")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> createJourney(@Valid @RequestBody CreateJourneyRequest request) {
        VehicleJourneyResponse response = journeyService.createJourney(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vehicle journey initialized"));
    }

    @GetMapping("/api/v1/journeys/{id}")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> getJourneyById(@PathVariable Long id) {
        VehicleJourneyResponse response = journeyService.getJourneyById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle journey retrieved successfully"));
    }

    @GetMapping("/api/v1/stations/{stationId}/journeys")
    public ResponseEntity<ApiResponse<List<VehicleJourneyResponse>>> getJourneysByStation(@PathVariable Long stationId) {
        List<VehicleJourneyResponse> responses = journeyService.getJourneysByStation(stationId);
        return ResponseEntity.ok(ApiResponse.success(responses, "Station vehicle journeys retrieved successfully"));
    }

    @PostMapping("/api/v1/journeys/{journeyId}/queue")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> enterQueue(@PathVariable Long journeyId) {
        VehicleJourneyResponse response = journeyService.enterQueue(journeyId);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle entered station queue"));
    }

    @GetMapping("/api/v1/stations/{stationId}/queue")
    public ResponseEntity<ApiResponse<List<VehicleJourneyResponse>>> getStationQueue(@PathVariable Long stationId) {
        List<VehicleJourneyResponse> response = journeyService.getStationQueue(stationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Station queue fetched successfully"));
    }

    @PostMapping("/api/v1/journeys/{journeyId}/assign-bay")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> assignBay(@PathVariable Long journeyId,
                                                                         @Valid @RequestBody AssignBayRequest request) {
        VehicleJourneyResponse response = journeyService.assignBay(journeyId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Fueling bay assigned successfully"));
    }

    @PostMapping("/api/v1/journeys/{journeyId}/fueling/start")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> startFueling(@PathVariable Long journeyId) {
        VehicleJourneyResponse response = journeyService.startFueling(journeyId);
        return ResponseEntity.ok(ApiResponse.success(response, "Fueling started successfully"));
    }

    @PostMapping("/api/v1/journeys/{journeyId}/fueling/complete")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> completeFueling(@PathVariable Long journeyId) {
        VehicleJourneyResponse response = journeyService.completeFueling(journeyId);
        return ResponseEntity.ok(ApiResponse.success(response, "Fueling completed successfully"));
    }

    @PostMapping("/api/v1/journeys/{journeyId}/exit")
    public ResponseEntity<ApiResponse<VehicleJourneyResponse>> exitStation(@PathVariable Long journeyId) {
        VehicleJourneyResponse response = journeyService.exitStation(journeyId);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle exited station successfully"));
    }
}
