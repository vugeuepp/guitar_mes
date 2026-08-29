package com.example.guitarmes.process;

import java.time.LocalDateTime;

public class ProcessHistoryResponse {
	private String processName;
	private String workerName;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Long workMinutes;
	private String startTimeText;
	private String endTimeText;
	private String workMinutesText;
	
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
	public String getStartTimeText() {
		return startTimeText;
	}
	public void setStartTimeText(String startTimeText) {
		this.startTimeText = startTimeText;
	}
	public String getEndTimeText() {
		return endTimeText;
	}
	public void setEndTimeText(String endTimeText) {
		this.endTimeText = endTimeText;
	}
	public String getWorkMinutesText() {
		return workMinutesText;
	}
	public void setWorkMinutesText(String workMinutesText) {
		this.workMinutesText = workMinutesText;
	}
}
