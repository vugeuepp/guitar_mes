package com.example.guitarmes.process.analysis;

public class ComponentProcessAverageTimeResponse {

    private String targetType;
    private String processName;
    private long averageMinutes;
    private long completedCount;

    public ComponentProcessAverageTimeResponse() {
    }

    public ComponentProcessAverageTimeResponse(
            String targetType,
            String processName,
            long averageMinutes,
            long completedCount) {

        this.targetType = targetType;
        this.processName = processName;
        this.averageMinutes = averageMinutes;
        this.completedCount = completedCount;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(
            String targetType) {

        this.targetType = targetType;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(
            String processName) {

        this.processName = processName;
    }

    public long getAverageMinutes() {
        return averageMinutes;
    }

    public void setAverageMinutes(
            long averageMinutes) {

        this.averageMinutes = averageMinutes;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(
            long completedCount) {

        this.completedCount = completedCount;
    }
}