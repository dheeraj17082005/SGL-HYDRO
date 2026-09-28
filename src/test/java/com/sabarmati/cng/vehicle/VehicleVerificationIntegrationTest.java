package com.sabarmati.cng.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarmati.cng.vehicle.dto.VehicleVerificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VehicleVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void verifyVehicle_validRegistration_returnsEligibleResponse() throws Exception {
        VehicleVerificationRequest request = new VehicleVerificationRequest("GJ01AB1234");

        mockMvc.perform(post("/api/v1/vehicles/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationNumber").value("GJ01AB1234"))
                .andExpect(jsonPath("$.data.vehicleFound").value(true))
                .andExpect(jsonPath("$.data.complianceStatus").value("ELIGIBLE"))
                .andExpect(jsonPath("$.data.source").value("MOCK"));
    }

    @Test
    void verifyVehicle_invalidRegistration_returnsNotEligibleResponse() throws Exception {
        VehicleVerificationRequest request = new VehicleVerificationRequest("GJ01INV");

        mockMvc.perform(post("/api/v1/vehicles/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.complianceStatus").value("NOT_ELIGIBLE"));
    }

    @Test
    void verifyVehicle_unregisteredVehicle_returnsNotEligibleResponse() throws Exception {
        VehicleVerificationRequest request = new VehicleVerificationRequest("GJ99UNREG9999");

        mockMvc.perform(post("/api/v1/vehicles/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleFound").value(false))
                .andExpect(jsonPath("$.data.complianceStatus").value("NOT_ELIGIBLE"));
    }

    @Test
    void verifyVehicle_missingHydroTest_returnsNotEligibleWithUnknownHydroStatus() throws Exception {
        VehicleVerificationRequest request = new VehicleVerificationRequest("GJ01NOHYDRO");

        mockMvc.perform(post("/api/v1/vehicles/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationNumber").value("GJ01NOHYDRO"))
                .andExpect(jsonPath("$.data.complianceStatus").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.data.hydroTestStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.reasons", hasItem("CNG cylinder hydro-test certificate is missing or unverified")));
    }

    @Test
    void verifyVehicle_expiredHydroTest_returnsNotEligibleWithExpiredHydroStatus() throws Exception {
        VehicleVerificationRequest request = new VehicleVerificationRequest("GJ01EXPHYDRO");

        mockMvc.perform(post("/api/v1/vehicles/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationNumber").value("GJ01EXPHYDRO"))
                .andExpect(jsonPath("$.data.complianceStatus").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.data.hydroTestStatus").value("EXPIRED"))
                .andExpect(jsonPath("$.data.reasons", hasItem("CNG cylinder hydro-test certificate is expired")));
    }

    @Test
    void verifyVehicle_100VehicleDataset_returnsEnrichedHydroTestDetails() throws Exception {
        VehicleVerificationRequest request = new VehicleVerificationRequest("MH12DE1050");

        mockMvc.perform(post("/api/v1/vehicles/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationNumber").value("MH12DE1050"))
                .andExpect(jsonPath("$.data.complianceStatus").value("ELIGIBLE"))
                .andExpect(jsonPath("$.data.hydroTestStatus").value("VALID"))
                .andExpect(jsonPath("$.data.hydroTestCertNumber").value("PESO-CERT-2024-5050"))
                .andExpect(jsonPath("$.data.cylinderSerialNo").value("CYL-CNG-MH12-8050"))
                .andExpect(jsonPath("$.data.makeModel").exists())
                .andExpect(jsonPath("$.data.rtoOffice").value("MH-12 Pune Regional Transport Office"));
    }
}

