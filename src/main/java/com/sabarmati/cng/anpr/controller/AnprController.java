package com.sabarmati.cng.anpr.controller;

import com.sabarmati.cng.anpr.dto.AnprDetectionRequest;
import com.sabarmati.cng.anpr.dto.AnprDetectionResponse;
import com.sabarmati.cng.anpr.service.AnprService;
import com.sabarmati.cng.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anpr")
public class AnprController {

    private final AnprService anprService;

    public AnprController(AnprService anprService) {
        this.anprService = anprService;
    }

    @PostMapping("/detections")
    public ResponseEntity<ApiResponse<AnprDetectionResponse>> processDetection(@Valid @RequestBody AnprDetectionRequest request) {
        AnprDetectionResponse response = anprService.processDetection(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }
}
