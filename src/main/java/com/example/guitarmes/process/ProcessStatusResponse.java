package com.example.guitarmes.process;

import java.time.LocalDateTime;

import com.example.guitarmes.common.DateTimeFormatterUtil;

public class ProcessStatusResponse {
	private String processName;
	private String status;
	private String workerName;
	
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	
	private Long workMinutes;
	private String workMinutesText;
	
	private Long historyId;
	private String startTimeText;
	private String endTimeText;
	
	public ProcessStatusResponse(String processName, String status, String workerName, LocalDateTime startTime,
			LocalDateTime endTime, Long workMinutes, Long historyId) {
		this.processName = processName;
		this.status = status;
		this.workerName = workerName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.workMinutes = workMinutes;
		this.historyId = historyId;
		
		this.startTimeText = DateTimeFormatterUtil.format(startTime);
		this.endTimeText = DateTimeFormatterUtil.format(endTime);
		
		if (workMinutes == null) {
			this.workMinutesText = "-";
		} else {
			this.workMinutesText = workMinutes + "分";
		}
	}
	
	public String getProcessName() {
		return processName;
	}
	public void setProcessName(String processName) {
		this.processName = processName;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
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
	public Long getHistoryId() {
		return historyId;
	}
	public void setHistoryId(Long historyId) {
		this.historyId = historyId;
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
