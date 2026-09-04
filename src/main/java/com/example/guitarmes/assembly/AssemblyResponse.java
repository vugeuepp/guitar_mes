package com.example.guitarmes.assembly;

import java.time.LocalDateTime;

public class AssemblyResponse {
	private Long assemblyId;
	private String guitarSerial;
	private String productName;
	private String productColor;
	private String neckSerial;
	private String bodySerial;
	private String workerName;
	private LocalDateTime assemblyDate;
	private String assemblyDateText;
	
	public Long getAssemblyId() {
		return assemblyId;
	}
	public void setAssemblyId(Long assemblyId) {
		this.assemblyId = assemblyId;
	}
	public String getGuitarSerial() {
		return guitarSerial;
	}
	public void setGuitarSerial(String guitarSerial) {
		this.guitarSerial = guitarSerial;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getProductColor() {
		return productColor;
	}
	public void setProductColor(String productColor) {
		this.productColor = productColor;
	}
	public String getNeckSerial() {
		return neckSerial;
	}
	public void setNeckSerial(String neckSerial) {
		this.neckSerial = neckSerial;
	}
	public String getBodySerial() {
		return bodySerial;
	}
	public void setBodySerial(String bodySerial) {
		this.bodySerial = bodySerial;
	}
	public String getWorkerName() {
		return workerName;
	}
	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}
	public LocalDateTime getAssemblyDate() {
		return assemblyDate;
	}
	public void setAssemblyDate(LocalDateTime assemblyDate) {
		this.assemblyDate = assemblyDate;
	}
	public String getAssemblyDateText() {
		return assemblyDateText;
	}
	public void setAssemblyDateText(String assemblyDateText) {
		this.assemblyDateText = assemblyDateText;
	}
	
}
