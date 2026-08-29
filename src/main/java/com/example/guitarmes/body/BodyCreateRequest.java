package com.example.guitarmes.body;

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