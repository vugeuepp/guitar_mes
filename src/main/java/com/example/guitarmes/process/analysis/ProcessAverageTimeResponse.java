package com.example.guitarmes.process.analysis;

public class ProcessAverageTimeResponse {
	private String processName;
	
	private Long averageMinutes;

	public ProcessAverageTimeResponse(String processName, Long averageMinutes) {
		this.processName = processName;
		this.averageMinutes = averageMinutes;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public Long getAverageMinutes() {
		return averageMinutes;
	}

	public void setAverageMinutes(Long averageMinutes) {
		this.averageMinutes = averageMinutes;
	}
	
	
}
