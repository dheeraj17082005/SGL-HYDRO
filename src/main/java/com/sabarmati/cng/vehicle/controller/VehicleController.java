package com.sabarmati.cng.vehicle.controller;

import com.sabarmati.cng.common.dto.ApiResponse;
import com.sabarmati.cng.vehicle.dto.VehicleRequest;
import com.sabarmati.cng.vehicle.dto.VehicleResponse;
import com.sabarmati.cng.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vehicle registered successfully"));
    }

    @GetMapping("/{registrationNumber}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleByRegistrationNumber(@PathVariable String registrationNumber) {
        VehicleResponse response = vehicleService.getVehicleByRegistrationNumber(registrationNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle retrieved successfully"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<com.sabarmati.cng.vehicle.dto.VehicleVerificationResponse>> verifyVehicle(@Valid @RequestBody com.sabarmati.cng.vehicle.dto.VehicleVerificationRequest request) {
        com.sabarmati.cng.vehicle.dto.VehicleVerificationResponse response = vehicleService.verifyVehicle(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle verification evaluated successfully"));
    }
}
