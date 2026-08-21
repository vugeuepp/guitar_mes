package com.example.guitarmes.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_body")
public class Body {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String serialNo;
	
	private String modelName;
	
	private String color;
	
	private String currentProcess;
	
	private String status;
	
	@ManyToOne
	@JoinColumn(name = "body_master_id")
	private BodyMaster bodyMaster;
	
	public Body() {
		
	}
	
	public Body(
			String serialNo,
			String modelName,
			String color,
			String currentProcess,
			String status) {
		this.serialNo = serialNo;
		this.modelName = modelName;
		this.color = color;
		this.currentProcess = currentProcess;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
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

	public BodyMaster getBodyMaster() {
		return bodyMaster;
	}

	public void setBodyMaster(BodyMaster bodyMaster) {
		this.bodyMaster = bodyMaster;
	}
	
	
	
}
