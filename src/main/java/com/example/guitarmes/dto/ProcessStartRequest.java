package com.example.guitarmes.dto;

public class ProcessStartRequest {
	private Long guitarId;
	private Long processId;
	private String workerName;
	public Long getGuitarId() {
		return guitarId;
	}
	public void setGuitarId(Long guitarId) {
		this.guitarId = guitarId;
	}
	public Long getProcessId() {
		return processId;
	}
	public void setProcessId(Long processId) {
		this.processId = processId;
	}
	public String getWorkerName() {
		return workerName;
	}
	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}
	
	
}
