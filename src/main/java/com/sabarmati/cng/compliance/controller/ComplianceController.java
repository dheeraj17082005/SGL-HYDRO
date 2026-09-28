package com.sabarmati.cng.compliance.controller;

import com.sabarmati.cng.common.dto.ApiResponse;
import com.sabarmati.cng.compliance.dto.ComplianceRequest;
import com.sabarmati.cng.compliance.dto.ComplianceResponse;
import com.sabarmati.cng.compliance.service.ComplianceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<ComplianceResponse>> verifyCompliance(@Valid @RequestBody ComplianceRequest request) {
        ComplianceResponse response = complianceService.verifyCompliance(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Compliance decision evaluated"));
    }
}
