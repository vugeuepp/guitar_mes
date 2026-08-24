package com.example.guitarmes.dto;

public class BodyCreateRequest {

    private Long bodyMasterId;

    public BodyCreateRequest() {
    }

    public Long getBodyMasterId() {
        return bodyMasterId;
    }

    public void setBodyMasterId(
            Long bodyMasterId) {

        this.bodyMasterId =
                bodyMasterId;
    }
}