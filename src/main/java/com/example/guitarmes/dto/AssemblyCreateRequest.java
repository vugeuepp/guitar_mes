package com.example.guitarmes.dto;

public class AssemblyCreateRequest {

    private Long productionOrderId;

    private Long neckId;

    private Long bodyId;

    private String workerName;

    public AssemblyCreateRequest() {
    }

    public Long getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(
            Long productionOrderId) {

        this.productionOrderId =
                productionOrderId;
    }

    public Long getNeckId() {
        return neckId;
    }

    public void setNeckId(
            Long neckId) {

        this.neckId = neckId;
    }

    public Long getBodyId() {
        return bodyId;
    }

    public void setBodyId(
            Long bodyId) {

        this.bodyId = bodyId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(
            String workerName) {

        this.workerName = workerName;
    }
}