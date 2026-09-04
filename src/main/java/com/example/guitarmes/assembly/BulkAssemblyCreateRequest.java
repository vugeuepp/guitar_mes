package com.example.guitarmes.assembly;

import java.util.ArrayList;
import java.util.List;

public class BulkAssemblyCreateRequest {
    private Long productionOrderId;
    private Long productionScheduleId;
    private String workerName;
    private List<Long> neckIds = new ArrayList<>();
    private List<Long> bodyIds = new ArrayList<>();

    public Long getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(Long value) { this.productionOrderId = value; }
    public Long getProductionScheduleId() { return productionScheduleId; }
    public void setProductionScheduleId(Long value) { this.productionScheduleId = value; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String value) { this.workerName = value; }
    public List<Long> getNeckIds() { return neckIds; }
    public void setNeckIds(List<Long> value) { this.neckIds = value; }
    public List<Long> getBodyIds() { return bodyIds; }
    public void setBodyIds(List<Long> value) { this.bodyIds = value; }
}
