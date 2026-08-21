package com.example.guitarmes.dto;

public class GuitarCreateRequest {
	private String serialNo;
	private Long productId;
	private String currentProcess;
	
	public String getSerialNo() {
		return serialNo;
	}
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public String getCurrentProcess() {
		return currentProcess;
	}
	public void setCurrentProcess(String currentProcess) {
		this.currentProcess = currentProcess;
	}
	
}
