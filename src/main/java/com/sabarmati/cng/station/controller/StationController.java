package com.sabarmati.cng.station.controller;

import com.sabarmati.cng.common.dto.ApiResponse;
import com.sabarmati.cng.station.dto.FuelingBayResponse;
import com.sabarmati.cng.station.dto.StationRequest;
import com.sabarmati.cng.station.dto.StationResponse;
import com.sabarmati.cng.station.service.StationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StationResponse>>> getAllStations() {
        List<StationResponse> stations = stationService.getAllStations();
        return ResponseEntity.ok(ApiResponse.success(stations, "Stations fetched successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StationResponse>> getStationById(@PathVariable Long id) {
        StationResponse station = stationService.getStationById(id);
        return ResponseEntity.ok(ApiResponse.success(station, "Station fetched successfully"));
    }

    @GetMapping("/{id}/bays")
    public ResponseEntity<ApiResponse<List<FuelingBayResponse>>> getStationBays(@PathVariable Long id) {
        List<FuelingBayResponse> bays = stationService.getStationBays(id);
        return ResponseEntity.ok(ApiResponse.success(bays, "Station fueling bays fetched successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StationResponse>> createStation(@Valid @RequestBody StationRequest request) {
        StationResponse createdStation = stationService.createStation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdStation, "Station created successfully"));
    }
}
