package com.example.guitarmes.dto;

public class NeckCreateRequest {

    private Long neckMasterId;

    public NeckCreateRequest() {
    }

    public Long getNeckMasterId() {
        return neckMasterId;
    }

    public void setNeckMasterId(
            Long neckMasterId) {

        this.neckMasterId =
                neckMasterId;
    }
}