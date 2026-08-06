package com.example.guitarmes.dto;

public class ProcessCountResponse {
	private String processName;
	private long count;
	
	public ProcessCountResponse(String processName, long count) {
		this.processName = processName;
		this.count = count;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}
}
