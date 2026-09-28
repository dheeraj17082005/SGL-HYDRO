package com.sabarmati.cng.journey.dto;

import jakarta.validation.constraints.NotNull;

public class AssignBayRequest {

    @NotNull(message = "Bay ID is required")
    private Long bayId;

    public AssignBayRequest() {
    }

    public AssignBayRequest(Long bayId) {
        this.bayId = bayId;
    }

    public Long getBayId() {
        return bayId;
    }

    public void setBayId(Long bayId) {
        this.bayId = bayId;
    }
}
