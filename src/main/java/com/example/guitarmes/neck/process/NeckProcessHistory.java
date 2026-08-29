package com.example.guitarmes.neck.process;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_neck_process_history")
public class NeckProcessHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long neckId;

    private Long processId;

    private String result;

    private String workerName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String note;

    public NeckProcessHistory() {
    }

    public NeckProcessHistory(
            Long neckId,
            Long processId,
            String workerName,
            LocalDateTime startTime) {

        this.neckId = neckId;
        this.processId = processId;
        this.workerName = workerName;
        this.startTime = startTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNeckId() {
        return neckId;
    }

    public void setNeckId(Long neckId) {
        this.neckId = neckId;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
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

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(
            LocalDateTime startTime) {

        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(
            LocalDateTime endTime) {

        this.endTime = endTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
