package com.example.guitarmes.dto;

public class GuitarProgressResponse {
	private Long id;
	private String serialNo;
	private String modelName;
	private String currentProcess;
	private int progressRate;
	private boolean hasRunningProcess;
	private boolean hasNextProcess;
	private boolean needAssembly;

	public GuitarProgressResponse(Long id, String serialNo, String modelName, String currentProcess, int progressRate,
			boolean hasRunningProcess, boolean hasNextProcess, boolean needAssembly) {
		this.id = id;
		this.serialNo = serialNo;
		this.modelName = modelName;
		this.currentProcess = currentProcess;
		this.progressRate = progressRate;
		this.hasRunningProcess = hasRunningProcess;
		this.hasNextProcess = hasNextProcess;
		this.needAssembly = needAssembly;
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

	public String getCurrentProcess() {
		return currentProcess;
	}

	public void setCurrentProcess(String currentProcess) {
		this.currentProcess = currentProcess;
	}

	public int getProgressRate() {
		return progressRate;
	}

	public void setProgressRate(int progressRate) {
		this.progressRate = progressRate;
	}
	
	public boolean isHasRunningProcess() {
		return hasRunningProcess;
	}
	
	public boolean isHasNextProcess() {
		return hasNextProcess;
	}
	
	public boolean isNeedAssembly() {
		return needAssembly;
	}
	
}
