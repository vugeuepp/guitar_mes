package com.example.guitarmes.dto;

public class BodyProcessHistoryResponse {

    private Long historyId;
    private String processName;
    private String result;
    private String workerName;
    private String startTimeText;
    private String endTimeText;
    private String workMinutesText;
    private String note;

    public BodyProcessHistoryResponse() {
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(
            String processName) {

        this.processName = processName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(
            String workerName) {

        this.workerName = workerName;
    }

    public String getStartTimeText() {
        return startTimeText;
    }

    public void setStartTimeText(
            String startTimeText) {

        this.startTimeText = startTimeText;
    }

    public String getEndTimeText() {
        return endTimeText;
    }

    public void setEndTimeText(
            String endTimeText) {

        this.endTimeText = endTimeText;
    }

    public String getWorkMinutesText() {
        return workMinutesText;
    }

    public void setWorkMinutesText(
            String workMinutesText) {

        this.workMinutesText =
                workMinutesText;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}