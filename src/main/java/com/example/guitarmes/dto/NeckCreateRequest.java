package com.example.guitarmes.dto;

public class NeckCreateRequest {
	private String serialNo;
	private String modelName;
	private String currentProcess;
	private String status;
	public String getSerialNo() {
		return serialNo;
	}
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	public String getCurrentProcess() {
		return currentProcess;
	}
	public void setCurrentProcess(String currentProcess) {
		this.currentProcess = currentProcess;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	
}
