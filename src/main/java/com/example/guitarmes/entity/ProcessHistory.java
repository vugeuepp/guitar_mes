package com.example.guitarmes.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_process_history")
public class ProcessHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long guitarId;
	private Long processId;
	private String workerName;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	
	public  ProcessHistory() {
		
	}
	
	public ProcessHistory(Long guitarId, Long processId, String workerName, LocalDateTime startTime) {
		this.guitarId = guitarId;
		this.processId = processId;
		this.workerName = workerName;
		this.startTime = startTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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
	
	
}
