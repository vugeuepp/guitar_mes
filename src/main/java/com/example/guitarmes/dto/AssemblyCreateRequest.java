package com.example.guitarmes.dto;

public class AssemblyCreateRequest {
	private Long guitarId;
	private Long neckId;
	private Long bodyId;
	private String workerName;
	public Long getGuitarId() {
		return guitarId;
	}
	public void setGuitarId(Long guitarId) {
		this.guitarId = guitarId;
	}
	public Long getNeckId() {
		return neckId;
	}
	public void setNeckId(Long neckId) {
		this.neckId = neckId;
	}
	public Long getBodyId() {
		return bodyId;
	}
	public void setBodyId(Long bodyId) {
		this.bodyId = bodyId;
	}
	public String getWorkerName() {
		return workerName;
	}
	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}
	
	
}
