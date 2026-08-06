package com.example.guitarmes.dto;

public class GuitarCreateRequest {
	private String serialNo;
	private String modelName;
	private String currentProcess;
	
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
	
}
