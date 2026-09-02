package com.example.guitarmes.process;

import java.util.ArrayList;
import java.util.List;

public class BulkProcessStartRequest {
    private List<Long> guitarIds = new ArrayList<>();
    private Long processId;
    private String workerName;
    public List<Long> getGuitarIds() { return guitarIds; }
    public void setGuitarIds(List<Long> guitarIds) { this.guitarIds = guitarIds; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long processId) { this.processId = processId; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
}
