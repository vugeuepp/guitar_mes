package com.example.guitarmes.neck;

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