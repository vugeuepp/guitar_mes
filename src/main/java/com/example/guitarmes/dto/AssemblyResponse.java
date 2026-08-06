package com.example.guitarmes.dto;

import java.time.LocalDateTime;

public class AssemblyResponse {
	private Long assemblyId;
	private String guitarSerial;
	private String neckSerial;
	private String bodySerial;
	private String workerName;
	private LocalDateTime assemblyDate;
	
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
}
