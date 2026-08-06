package com.example.guitarmes.dto;

import java.time.LocalDateTime;

public class ProcessHistoryResponse {
	private String processName;
	private String workerName;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Long workMinutes;
	
	public String getProcessName() {
		return processName;
	}
	public void setProcessName(String processName) {
		this.processName = processName;
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
	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}
	public LocalDateTime getEndTime() {
		return endTime;
	}
	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}
	public Long getWorkMinutes() {
		return workMinutes;
	}
	public void setWorkMinutes(Long workMinutes) {
		this.workMinutes = workMinutes;
	}
}
